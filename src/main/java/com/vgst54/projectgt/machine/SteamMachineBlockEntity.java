package com.vgst54.projectgt.machine;

import javax.annotation.Nullable;

import com.vgst54.projectgt.machine.recipe.MachineType;
import com.vgst54.projectgt.machine.recipe.RecipeProcessor;
import com.vgst54.projectgt.registry.ModBlockEntities;
import com.vgst54.projectgt.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Паровая машина: бак пара, один входной и один выходной слот, рецептный движок.
 *
 * Снаружи (трубы, воронки, котёл рядом): пар только заливается, предметы кладутся во вход и забираются из выхода.
 * Энергия берётся из бака ({@link SteamSource}), пересчёт рецепта по давлению делает {@link RecipeScaling#steam}.
 * Пока нет: выхлопа пара (Exhaust Vent), внутреннего перегрева.
 */
public class SteamMachineBlockEntity extends BlockEntity implements MenuProvider {
    /** Ёмкость бака пара, как у паровых машин GTCEu (16 000 mB). */
    public static final int STEAM_CAPACITY = 16_000;

    private final MachineType machine;
    private final SteamPressure pressure;
    private final FluidTank steam;
    private final ItemStackHandler input = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final RecipeProcessor processor;
    private final IFluidHandler fluidHandler = new SteamInput();
    private final IItemHandler itemHandler = new SidedItems();
    /** Для меню: 0 прогресс, 1 длительность, 2 пар в баке. Передаётся клиенту как short, значения у нас меньше 32767. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> processor.progress();
                case 1 -> processor.duration();
                case 2 -> steam.getFluidAmount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Данные только для чтения: клиент их не меняет.
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public SteamMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_MACHINE.get(), pos, state);
        SteamMachineBlock block = (SteamMachineBlock) state.getBlock();
        this.machine = block.machine();
        this.pressure = block.pressure();
        this.steam = new FluidTank(STEAM_CAPACITY, fluid -> fluid.is(ModFluids.STEAM.get())) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        this.processor = new RecipeProcessor(machine, RecipeScaling.steam(pressure), input, output, new SteamSource(steam));
    }

    public MachineType machine() {
        return machine;
    }

    public SteamPressure pressure() {
        return pressure;
    }

    // --- Доступ снаружи ---

    public IFluidHandler fluidHandler() {
        return fluidHandler;
    }

    public IItemHandler itemHandler() {
        return itemHandler;
    }

    // --- Тик ---

    public void serverTick() {
        if (level != null && processor.tick(level)) {
            setChanged();
        }
    }

    /** Выбрасывает содержимое слотов при поломке блока. */
    public void dropContents(Level level, BlockPos pos) {
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), input.getStackInSlot(0));
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), output.getStackInSlot(0));
    }

    // --- Меню ---

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SteamMachineMenu(id, inventory, input, output, data,
                ContainerLevelAccess.create(level, worldPosition), getBlockState().getBlock());
    }

    // --- Сохранение ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Steam", steam.writeToNBT(registries, new CompoundTag()));
        tag.put("Input", input.serializeNBT(registries));
        tag.put("Output", output.serializeNBT(registries));
        processor.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        steam.readFromNBT(registries, tag.getCompound("Steam"));
        input.deserializeNBT(registries, tag.getCompound("Input"));
        output.deserializeNBT(registries, tag.getCompound("Output"));
        processor.load(tag);
    }

    /** Снаружи пар только заливается (из труб и котла), забрать его нельзя. */
    private final class SteamInput implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return steam.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return steam.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return steam.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return steam.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    /** Снаружи слот 0 это вход (только вставка), слот 1 это выход (только извлечение). */
    private final class SidedItems implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? input.getStackInSlot(0) : output.getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? input.insertItem(0, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? output.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? input.getSlotLimit(0) : output.getSlotLimit(0);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && input.isItemValid(0, stack);
        }
    }
}
