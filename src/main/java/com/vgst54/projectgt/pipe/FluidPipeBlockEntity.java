package com.vgst54.projectgt.pipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.vgst54.projectgt.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Жидкостная труба: внутренний бак ёмкостью в throughput.
 *
 * Раздача (раз в DISTRIBUTE_INTERVAL тиков): труба пытается отдать всё содержимое соседям.
 * Доли пропорциональны свободному месту у соседа, поэтому пустые баки получают больше.
 * Стрелки (flowMask): если стрелка стоит хотя бы на одной стороне, труба отдаёт только в эти стороны
 * и не принимает с них. Стрелок может быть сколько угодно, хоть все шесть.
 * Если жидкость горячее heatLimit, труба исчезает и поджигает блоки вокруг.
 */
public class FluidPipeBlockEntity extends BlockEntity {
    /** Раз в секунду, как в GTNH. */
    public static final int DISTRIBUTE_INTERVAL = 20;
    /** Сколько пробуем влить при оценке свободного места у соседа (SIMULATE). */
    private static final int PROBE_AMOUNT = 1 << 30;

    private final FluidPipeType type;
    private final FluidTank tank;
    /** Биты по Direction.ordinal(): 1 значит сторона выключена ключом. */
    private int disabledMask;
    /** Биты по Direction.ordinal(): 1 значит рука торчит в эту сторону даже без соседа («в никуда»). */
    private int forcedMask;
    /** Биты по Direction.ordinal(): 1 значит на этой стороне стоит стрелка (выход). 0 значит стрелок нет. */
    private int flowMask;
    private boolean overheated;
    /** Сдвиг, чтобы трубы не раздавали все в один и тот же тик. */
    private final int phase;
    /** Обработчики по сторонам; последний (индекс 6) для side == null. */
    private final SideHandler[] handlers = new SideHandler[7];

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PIPE.get(), pos, state);
        this.type = ((FluidPipeBlock) state.getBlock()).type();
        this.tank = new FluidTank(type.throughput()) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        this.phase = Math.floorMod(pos.asLong(), DISTRIBUTE_INTERVAL);
        for (int i = 0; i < handlers.length; i++) {
            handlers[i] = new SideHandler(i == 6 ? null : Direction.values()[i]);
        }
    }

    public FluidPipeType type() {
        return type;
    }

    // --- Стороны и направление ---

    public int disabledMask() {
        return disabledMask;
    }

    public boolean isSideDisabled(Direction side) {
        return (disabledMask & (1 << side.ordinal())) != 0;
    }

    public int forcedMask() {
        return forcedMask;
    }

    /**
     * Переключает сторону по тому, торчит ли сейчас рука (extended).
     * Торчит: выключаем сторону, даже если там сосед. Не торчит: включаем, рука торчит даже в пустоту.
     * Возвращает true, если сторона теперь включена.
     */
    public boolean toggleSide(Direction side, boolean extended) {
        int bit = 1 << side.ordinal();
        if (extended) {
            disabledMask |= bit;
            forcedMask &= ~bit;
        } else {
            disabledMask &= ~bit;
            forcedMask |= bit;
        }
        setChanged();
        return !extended;
    }

    public int flowMask() {
        return flowMask;
    }

    public boolean hasArrow(Direction side) {
        return (flowMask & (1 << side.ordinal())) != 0;
    }

    /** Ставит или снимает стрелку на стороне. Возвращает true, если стрелка теперь стоит. */
    public boolean toggleArrow(Direction side) {
        flowMask ^= 1 << side.ordinal();
        setChanged();
        // Клиенту нужны стрелки для оверлея ключа.
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return hasArrow(side);
    }

    /** Обработчик жидкости для соседей. Выключенная сторона ничего не отдаёт и не принимает. */
    @Nullable
    public IFluidHandler handler(@Nullable Direction side) {
        if (side != null && isSideDisabled(side)) {
            return null;
        }
        return handlers[side == null ? 6 : side.ordinal()];
    }

    // --- Тик ---

    public void serverTick() {
        if (level == null) {
            return;
        }
        if (overheated) {
            burn();
            return;
        }
        if ((level.getGameTime() + phase) % DISTRIBUTE_INTERVAL == 0) {
            distribute();
        }
    }

    private void distribute() {
        FluidStack content = tank.getFluid();
        if (content.isEmpty()) {
            return;
        }

        record Target(IFluidHandler handler, int space) {}
        List<Target> targets = new ArrayList<>();
        long totalSpace = 0;
        for (Direction dir : Direction.values()) {
            if (isSideDisabled(dir) || (flowMask != 0 && !hasArrow(dir))) {
                continue;
            }
            IFluidHandler neighbor = level.getCapability(
                    Capabilities.FluidHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (neighbor == null) {
                continue;
            }
            int space = neighbor.fill(content.copyWithAmount(PROBE_AMOUNT), IFluidHandler.FluidAction.SIMULATE);
            if (space > 0) {
                targets.add(new Target(neighbor, space));
                totalSpace += space;
            }
        }
        if (targets.isEmpty()) {
            return;
        }

        // Доля соседа = содержимое × его свободное место / всё свободное место (но не больше его места).
        int amount = content.getAmount();
        int[] shares = new int[targets.size()];
        int given = 0;
        for (int i = 0; i < shares.length; i++) {
            shares[i] = (int) Math.min(targets.get(i).space(), (long) amount * targets.get(i).space() / totalSpace);
            given += shares[i];
        }
        // Остаток от округления отдаём тем, у кого больше места.
        int remainder = amount - given;
        if (remainder > 0) {
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < shares.length; i++) {
                order.add(i);
            }
            order.sort(Comparator.comparingInt((Integer i) -> targets.get(i).space()).reversed());
            for (int i : order) {
                int extra = Math.min(remainder, targets.get(i).space() - shares[i]);
                shares[i] += extra;
                remainder -= extra;
                if (remainder <= 0) {
                    break;
                }
            }
        }

        for (int i = 0; i < shares.length; i++) {
            if (shares[i] <= 0) {
                continue;
            }
            int accepted = targets.get(i).handler().fill(content.copyWithAmount(shares[i]), IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) {
                tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /** Перегрев: труба пропадает вместе с жидкостью, на её месте и рядом появляется огонь. */
    private void burn() {
        Level lvl = level;
        BlockPos pos = worldPosition;
        lvl.removeBlock(pos, false);
        List<BlockPos> spots = new ArrayList<>();
        spots.add(pos);
        for (Direction dir : Direction.values()) {
            spots.add(pos.relative(dir));
        }
        for (BlockPos spot : spots) {
            if (lvl.getBlockState(spot).isAir()) {
                BlockState fire = BaseFireBlock.getState(lvl, spot);
                if (fire.canSurvive(lvl, spot)) {
                    lvl.setBlockAndUpdate(spot, fire);
                }
            }
        }
    }

    // --- Сохранение ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("DisabledMask", disabledMask);
        tag.putInt("ForcedMask", forcedMask);
        tag.putInt("FlowMask", flowMask);
    }

    // Синхронизация с клиентом: нужна только для оверлея ключа (стрелки и стороны).
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        disabledMask = tag.getInt("DisabledMask");
        forcedMask = tag.getInt("ForcedMask");
        flowMask = tag.getInt("FlowMask");
    }

    /** Обработчик жидкости одной стороны трубы. */
    private final class SideHandler implements IFluidHandler {
        @Nullable
        private final Direction side;

        SideHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return tank.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return tank.getFluidInTank(index);
        }

        @Override
        public int getTankCapacity(int index) {
            return tank.getTankCapacity(index);
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return tank.isFluidValid(index, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // Со сторон со стрелкой (выходов) ничего не принимаем: так нет откатов назад.
            if (resource.isEmpty() || (side != null && hasArrow(side))) {
                return 0;
            }
            int filled = tank.fill(resource, action);
            if (filled > 0 && action.execute()
                    && resource.getFluidType().getTemperature(resource) > type.heatLimit()) {
                overheated = true;
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return tank.drain(maxDrain, action);
        }
    }
}
