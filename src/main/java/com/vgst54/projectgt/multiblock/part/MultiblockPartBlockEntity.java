package com.vgst54.projectgt.multiblock.part;

import javax.annotation.Nullable;

import com.vgst54.projectgt.multiblock.PartAbility;
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
 * Хранилище люка: у шин 4 слота предметов, у парового люка бак пара.
 * Снаружи (воронки, трубы): шина ввода только принимает, шина вывода только отдаёт, паровой люк только принимает пар.
 * Мультиблок читает содержимое напрямую через {@link #items()} и {@link #steam()}.
 */
public class MultiblockPartBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ITEM_SLOTS = 4;
    /** Ёмкость парового люка, mB. */
    public static final int STEAM_CAPACITY = 16_000;

    private final PartAbility ability;
    private final ItemStackHandler items = new ItemStackHandler(ITEM_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FluidTank steam = new FluidTank(STEAM_CAPACITY, fluid -> fluid.is(ModFluids.STEAM.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IItemHandler outsideItems;
    private final IFluidHandler outsideFluid = new IFluidHandler() {
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
    };

    public MultiblockPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MULTIBLOCK_PART.get(), pos, state);
        this.ability = ((MultiblockPartBlock) state.getBlock()).ability();
        this.outsideItems = new OutsideItems(ability == PartAbility.STEAM_IMPORT_ITEMS);
    }

    public PartAbility ability() {
        return ability;
    }

    public ItemStackHandler items() {
        return items;
    }

    public FluidTank steam() {
        return steam;
    }

    /** Предметы снаружи; null у парового люка. */
    @Nullable
    public IItemHandler outsideItems() {
        return ability == PartAbility.STEAM ? null : outsideItems;
    }

    /** Жидкость снаружи; null у шин. */
    @Nullable
    public IFluidHandler outsideFluid() {
        return ability == PartAbility.STEAM ? outsideFluid : null;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(slot));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PartItemMenu(id, inventory, items, ability == PartAbility.STEAM_EXPORT_ITEMS,
                ContainerLevelAccess.create(level, worldPosition), getBlockState().getBlock());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.put("Steam", steam.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        steam.readFromNBT(registries, tag.getCompound("Steam"));
    }

    /** Снаружи шина ввода только принимает, шина вывода только отдаёт. */
    private final class OutsideItems implements IItemHandler {
        private final boolean insertOnly;

        private OutsideItems(boolean insertOnly) {
            this.insertOnly = insertOnly;
        }

        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return insertOnly ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return insertOnly ? ItemStack.EMPTY : items.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return insertOnly && items.isItemValid(slot, stack);
        }
    }
}
