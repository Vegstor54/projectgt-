package com.vgst54.projectgt.boiler;

import javax.annotation.Nullable;

import com.vgst54.projectgt.registry.ModBlockEntities;
import com.vgst54.projectgt.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Одноблочный паровой котёл.
 *
 * Нагрев (heat, от 0 до warmupTicks): растёт на 1 за каждый тик горения, остывает на 1 раз в COOL_INTERVAL тиков.
 * Пар идёт, когда нагрев не ниже четверти максимума, и пропорционален нагреву. На 1 mB воды выходит
 * STEAM_PER_WATER mB пара, как в GT. Без воды пара нет, котёл просто греется дальше.
 * Пар сам выталкивается в соседние жидкостные ёмкости и трубы.
 *
 * Снаружи доступно: вода (заливка) и пар (забор); у жидкостного ещё и топливо (заливка).
 * У угольного есть слот топлива (предметный handler для воронок).
 * Пока нет: взрыва при сухом нагреве, GUI (статус по ПКМ пустой рукой).
 */
public class SteamBoilerBlockEntity extends BlockEntity implements MenuProvider {
    /** mB пара на 1 mB воды. */
    public static final int STEAM_PER_WATER = 160;
    /** Раз во сколько тиков остывает на 1 при отсутствии горения. */
    private static final int COOL_INTERVAL = 4;
    /** Сколько mB жидкого топлива берём за один заход горения. */
    private static final int LIQUID_FUEL_PORTION = 50;

    private final BoilerType type;
    private final FluidTank water;
    private final FluidTank steam;
    private final FluidTank fuel;
    private final ItemStackHandler fuelSlot;
    private final IFluidHandler fluidHandler = new Handler();
    /** Для меню: 0 нагрев, 1 нагрев на максимуме, 2 пламя 0..1000, 3 вода в баке, 4 ёмкость бака воды. Клиенту уходит как short, наши значения меньше 32767. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> heat;
                case 1 -> type.warmupTicks();
                case 2 -> flamePermille();
                case 3 -> water.getFluidAmount();
                case 4 -> type.waterCapacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Только для чтения: клиент данные не меняет.
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    private int heat;
    /** Сколько тиков ещё горит текущее топливо. */
    private int burnTime;
    /** Сколько тиков горения было у текущей порции топлива (для индикатора пламени). */
    private int burnTotal;
    /** Горел ли котёл в последнем тике (у солнечного это просто «светит солнце»). */
    private boolean lit;
    /** Пар, уже оплаченный водой, но ещё не выданный (вода тратится целыми mB). */
    private int steamCredit;

    public SteamBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_BOILER.get(), pos, state);
        this.type = ((SteamBoilerBlock) state.getBlock()).type();
        this.water = new FluidTank(type.waterCapacity(), fluid -> fluid.is(Fluids.WATER)) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        this.steam = new FluidTank(type.steamCapacity(), fluid -> fluid.is(ModFluids.STEAM.get())) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        this.fuel = new FluidTank(type.fuelCapacity(), fluid -> BoilerFuels.ticksPerMb(fluid.getFluid()) > 0) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        this.fuelSlot = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return isSolidFuel(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    public BoilerType type() {
        return type;
    }

    /** Подходит ли предмет как твёрдое топливо (горит в печи, но не ведро с лавой). */
    private static boolean isSolidFuel(ItemStack stack) {
        return !(stack.getItem() instanceof BucketItem) && stack.getBurnTime(null) > 0;
    }

    // --- Доступ снаружи ---

    public IFluidHandler fluidHandler() {
        return fluidHandler;
    }

    /** Слот топлива для воронок; null у котлов без твёрдого топлива. */
    @Nullable
    public IItemHandler itemHandler() {
        return type.kind() == BoilerKind.SOLID ? fuelSlot : null;
    }

    /** Кладёт предмет в слот топлива. Возвращает остаток (то, что не поместилось). */
    public ItemStack insertFuel(ItemStack stack) {
        if (type.kind() != BoilerKind.SOLID) {
            return stack;
        }
        return fuelSlot.insertItem(0, stack, false);
    }

    /** Строка статуса для сообщения игроку. */
    public Component status() {
        return Component.translatable("message.projectgt.boiler.status",
                heat * 100 / type.warmupTicks(), steam.getFluidAmount(), water.getFluidAmount());
    }

    /** Пламя для индикатора: 0..1000. Без топлива и без горения 0; у солнечного 1000, пока светит солнце. */
    private int flamePermille() {
        if (!lit) {
            return 0;
        }
        if (type.kind() == BoilerKind.SOLAR || burnTotal <= 0) {
            return 1000;
        }
        return Math.min(1000, burnTime * 1000 / burnTotal);
    }

    // --- Меню ---

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BoilerMenu(id, inventory, itemHandler(), data,
                ContainerLevelAccess.create(level, worldPosition), getBlockState().getBlock());
    }

    // --- Тик ---

    public void serverTick() {
        if (level == null) {
            return;
        }
        boolean burning = updateBurning(level);
        lit = burning;
        if (burning) {
            if (heat < type.warmupTicks()) {
                heat++;
            }
        } else if (heat > 0 && level.getGameTime() % COOL_INTERVAL == 0) {
            heat--;
        }
        produceSteam();
        pushSteam(level);
        if (burning || heat > 0) {
            setChanged();
        }
    }

    /** Горит ли котёл в этом тике. Заодно берёт новое топливо, когда прежнее кончилось. */
    private boolean updateBurning(Level level) {
        if (type.kind() == BoilerKind.SOLAR) {
            return level.canSeeSky(worldPosition.above()) && level.isDay() && !level.isRaining();
        }
        if (burnTime > 0) {
            burnTime--;
            return true;
        }
        // Пар некуда девать: новое топливо не жжём зря.
        if (steam.getFluidAmount() >= steam.getCapacity()) {
            return false;
        }
        return type.kind() == BoilerKind.SOLID ? igniteSolid() : igniteLiquid();
    }

    private boolean igniteSolid() {
        ItemStack stack = fuelSlot.getStackInSlot(0);
        if (stack.isEmpty()) {
            return false;
        }
        int ticks = stack.getBurnTime(null);
        if (ticks <= 0) {
            return false;
        }
        fuelSlot.extractItem(0, 1, false);
        burnTime = ticks * type.fuelDurationFactor();
        burnTotal = burnTime;
        return true;
    }

    private boolean igniteLiquid() {
        FluidStack stored = fuel.getFluid();
        int perMb = BoilerFuels.ticksPerMb(stored.getFluid());
        if (perMb <= 0 || stored.getAmount() < LIQUID_FUEL_PORTION) {
            return false;
        }
        fuel.drain(LIQUID_FUEL_PORTION, IFluidHandler.FluidAction.EXECUTE);
        burnTime = perMb * LIQUID_FUEL_PORTION * type.fuelDurationFactor();
        burnTotal = burnTime;
        return true;
    }

    private void produceSteam() {
        if (heat < type.warmupTicks() / 4) {
            return;
        }
        int want = type.steamPerTick() * heat / type.warmupTicks();
        want = Math.min(want, steam.getCapacity() - steam.getFluidAmount());
        if (want <= 0) {
            return;
        }
        if (steamCredit < want) {
            int waterNeeded = Math.ceilDiv(want - steamCredit, STEAM_PER_WATER);
            FluidStack drained = water.drain(waterNeeded, IFluidHandler.FluidAction.EXECUTE);
            steamCredit += drained.getAmount() * STEAM_PER_WATER;
        }
        int made = Math.min(want, steamCredit);
        if (made <= 0) {
            return;
        }
        steamCredit -= made;
        steam.fill(new FluidStack(ModFluids.STEAM.get(), made), IFluidHandler.FluidAction.EXECUTE);
    }

    private void pushSteam(Level level) {
        if (steam.isEmpty()) {
            return;
        }
        for (Direction dir : Direction.values()) {
            IFluidHandler neighbor = level.getCapability(
                    Capabilities.FluidHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (neighbor == null) {
                continue;
            }
            FluidStack offer = steam.drain(steam.getFluidAmount(), IFluidHandler.FluidAction.SIMULATE);
            if (offer.isEmpty()) {
                return;
            }
            int accepted = neighbor.fill(offer, IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) {
                steam.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /** Выбрасывает топливо из слота при поломке блока. */
    public void dropContents(Level level, BlockPos pos) {
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), fuelSlot.getStackInSlot(0));
    }

    // --- Сохранение ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("Steam", steam.writeToNBT(registries, new CompoundTag()));
        tag.put("Fuel", fuel.writeToNBT(registries, new CompoundTag()));
        tag.put("FuelSlot", fuelSlot.serializeNBT(registries));
        tag.putInt("Heat", heat);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTotal", burnTotal);
        tag.putInt("SteamCredit", steamCredit);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water.readFromNBT(registries, tag.getCompound("Water"));
        steam.readFromNBT(registries, tag.getCompound("Steam"));
        fuel.readFromNBT(registries, tag.getCompound("Fuel"));
        fuelSlot.deserializeNBT(registries, tag.getCompound("FuelSlot"));
        heat = tag.getInt("Heat");
        burnTime = tag.getInt("BurnTime");
        burnTotal = tag.getInt("BurnTotal");
        steamCredit = tag.getInt("SteamCredit");
    }

    /**
     * Внешний вид баков: 0 вода, 1 пар, 2 топливо (только у жидкостного).
     * Заливается вода (и топливо у жидкостного), забирается только пар.
     */
    private final class Handler implements IFluidHandler {
        private FluidTank[] tanks() {
            return type.kind() == BoilerKind.LIQUID ? new FluidTank[] {water, steam, fuel} : new FluidTank[] {water, steam};
        }

        @Override
        public int getTanks() {
            return type.kind() == BoilerKind.LIQUID ? 3 : 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tanks()[tank].getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tanks()[tank].getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            // Пар снаружи не заливается.
            return tank != 1 && tanks()[tank].isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (water.isFluidValid(resource)) {
                return water.fill(resource, action);
            }
            if (type.kind() == BoilerKind.LIQUID && fuel.isFluidValid(resource)) {
                return fuel.fill(resource, action);
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return steam.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return steam.drain(maxDrain, action);
        }
    }
}
