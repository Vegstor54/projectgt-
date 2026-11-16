package com.vgst54.projectgt.pipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import javax.annotation.Nullable;

import com.vgst54.projectgt.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Предметная труба (по образцу GTNH).
 *
 * Предметы лежат только в первой трубе, рядом с источником, и сразу «телепортируются» к цели.
 * Раз в periodSeconds труба пытается отдать всё содержимое в инвентарь с наименьшим routing value.
 * Routing value инвентаря = сумма routing value всех труб на пути до него (включая эту трубу).
 * Стек не делится, пока в лучшей цели хватает места; остаток идёт в следующую по приоритету цель.
 * Внутренняя ёмкость равна stacks слотов; извлекать предметы из трубы снаружи нельзя.
 */
public class ItemPipeBlockEntity extends BlockEntity {
    /** Сколько труб максимум обходим при поиске цели, чтобы огромные сети не вешали сервер. */
    private static final int MAX_NODES = 512;

    private final ItemPipeType type;
    private final ItemStackHandler inventory;
    /** Для каждого слота: Direction.ordinal() стороны, с которой пришёл стек (-1 нет). Туда обратно не отдаём. */
    private final int[] origins;
    /** Биты по Direction.ordinal(): 1 значит сторона открыта. Автоматически не открывается. */
    private int openMask;
    /** Биты по Direction.ordinal(): выход через эту сторону имеет routing value ×100. */
    private int restrictiveMask;
    private final int phase;
    /** Обработчики по сторонам; последний (индекс 6) для side == null. */
    private final SideHandler[] handlers = new SideHandler[7];

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_PIPE.get(), pos, state);
        this.type = ((ItemPipeBlock) state.getBlock()).type();
        this.origins = new int[type.stacks()];
        Arrays.fill(origins, -1);
        this.inventory = new ItemStackHandler(type.stacks()) {
            @Override
            protected void onContentsChanged(int slot) {
                if (getStackInSlot(slot).isEmpty()) {
                    origins[slot] = -1;
                }
                setChanged();
            }
        };
        this.phase = Math.floorMod(pos.asLong(), type.intervalTicks());
        for (int i = 0; i < handlers.length; i++) {
            handlers[i] = new SideHandler(i == 6 ? null : Direction.values()[i]);
        }
    }

    public ItemPipeType type() {
        return type;
    }

    public int restrictiveMask() {
        return restrictiveMask;
    }

    public boolean hasRestrictive(Direction side) {
        return (restrictiveMask & (1 << side.ordinal())) != 0;
    }

    public int routingValue(Direction outputSide) {
        return type.routingValue() * (hasRestrictive(outputSide) ? ItemPipeType.RESTRICTIVE_MULTIPLIER : 1);
    }

    /** Переключает restrictive-маркер стороны; как у fluid arrow, сторона при этом открывается. */
    public boolean toggleRestrictive(Direction side) {
        int bit = 1 << side.ordinal();
        openMask |= bit;
        restrictiveMask ^= bit;
        setChanged();
        syncToClient();
        return hasRestrictive(side);
    }

    // --- Стороны ---

    public int openMask() {
        return openMask;
    }

    public boolean isOpen(Direction side) {
        return (openMask & (1 << side.ordinal())) != 0;
    }

    public void setSideOpen(Direction side, boolean open) {
        int bit = 1 << side.ordinal();
        if (open) {
            openMask |= bit;
        } else {
            openMask &= ~bit;
            restrictiveMask &= ~bit;
        }
        setChanged();
        syncToClient();
    }

    /** Открывает сторону (если уже открыта, ничего не меняет). */
    public void openSide(Direction side) {
        setSideOpen(side, true);
    }

    /** Переключает сторону. Возвращает true, если теперь открыта. */
    public boolean toggleSide(Direction side) {
        setSideOpen(side, !isOpen(side));
        return isOpen(side);
    }

    /** Клиенту нужны открытые стороны для оверлея ключа. */
    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Обработчик предметов для соседей. Закрытая сторона ничего не принимает. */
    @Nullable
    public IItemHandler handler(@Nullable Direction side) {
        if (side != null && !isOpen(side)) {
            return null;
        }
        return handlers[side == null ? 6 : side.ordinal()];
    }

    // --- Тик и маршрутизация ---

    public void serverTick() {
        if (level == null || (level.getGameTime() + phase) % type.intervalTicks() != 0) {
            return;
        }
        route();
    }

    /** Цель маршрута: обработчик инвентаря, цена пути и откуда к нему подходим. */
    private record Destination(IItemHandler handler, long cost, BlockPos pipePos, Direction side) {}

    private void route() {
        boolean empty = true;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                empty = false;
                break;
            }
        }
        if (empty) {
            return;
        }
        List<Destination> destinations = collectDestinations();
        if (destinations.isEmpty()) {
            return;
        }

        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack inSlot = inventory.getStackInSlot(slot);
            if (inSlot.isEmpty()) {
                continue;
            }
            int origin = origins[slot];
            ItemStack stack = inventory.extractItem(slot, inSlot.getCount(), false);
            for (Destination destination : destinations) {
                // Обратно в источник не отдаём (у него нет роли выхода без отдельного модуля).
                if (destination.pipePos().equals(worldPosition) && destination.side().ordinal() == origin) {
                    continue;
                }
                stack = ItemHandlerHelper.insertItem(destination.handler(), stack, false);
                if (stack.isEmpty()) {
                    break;
                }
            }
            if (!stack.isEmpty()) {
                // Не влезло нигде: возвращаем в слот вместе со стороной-источником.
                inventory.insertItem(slot, stack, false);
                origins[slot] = origin;
            }
        }
    }

    /**
     * Поиск Дейкстрой по сети труб (через стороны, открытые с обоих концов).
     * Стоимость пути = сумма routing value всех труб на нём, включая стартовую.
     * Возвращает инвентари у открытых сторон труб, отсортированные по стоимости (дешёвые первыми).
     */
    private List<Destination> collectDestinations() {
        record Node(BlockPos pos, long cost) {}

        List<Destination> destinations = new ArrayList<>();
        Map<BlockPos, Long> best = new HashMap<>();
        Set<BlockPos> done = new HashSet<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingLong(Node::cost));

        best.put(worldPosition, 0L);
        queue.add(new Node(worldPosition, 0L));

        while (!queue.isEmpty() && done.size() < MAX_NODES) {
            Node node = queue.poll();
            if (!done.add(node.pos())) {
                continue;
            }
            if (!(level.getBlockEntity(node.pos()) instanceof ItemPipeBlockEntity pipe)) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                if (!pipe.isOpen(dir)) {
                    continue;
                }
                long pathCost = node.cost() + pipe.routingValue(dir);
                BlockPos neighborPos = node.pos().relative(dir);
                if (!level.isLoaded(neighborPos)) {
                    continue;
                }
                if (level.getBlockEntity(neighborPos) instanceof ItemPipeBlockEntity other) {
                    if (!other.isOpen(dir.getOpposite())) {
                        continue;
                    }
                    if (pathCost < best.getOrDefault(neighborPos, Long.MAX_VALUE)) {
                        best.put(neighborPos, pathCost);
                        queue.add(new Node(neighborPos, pathCost));
                    }
                } else {
                    IItemHandler handler = level.getCapability(
                            Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
                    if (handler != null) {
                        destinations.add(new Destination(handler, pathCost, node.pos(), dir));
                    }
                }
            }
        }
        destinations.sort(Comparator.comparingLong(Destination::cost));
        return destinations;
    }

    // --- Сохранение и синхронизация ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putIntArray("Origins", origins);
        tag.putInt("OpenMask", openMask);
        tag.putInt("RestrictiveMask", restrictiveMask);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        int[] saved = tag.getIntArray("Origins");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = i < saved.length ? saved[i] : -1;
        }
        openMask = tag.getInt("OpenMask");
        restrictiveMask = tag.contains("RestrictiveMask")
            ? tag.getInt("RestrictiveMask") & openMask
            : tag.getBoolean("Restrictive") ? openMask : 0;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Обработчик предметов одной стороны: можно вставлять, достать нельзя. */
    private final class SideHandler implements IItemHandler {
        @Nullable
        private final Direction side;

        SideHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            ItemStack remainder = inventory.insertItem(slot, stack, simulate);
            if (!simulate && remainder.getCount() < stack.getCount() && origins[slot] == -1) {
                origins[slot] = side == null ? -1 : side.ordinal();
            }
            return remainder;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inventory.isItemValid(slot, stack);
        }
    }
}
