package com.vgst54.projectgt.boiler;

import javax.annotation.Nullable;

import com.vgst54.projectgt.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Меню парового котла: слот топлива (только у угольного), индикатор пламени и шкала нагрева.
 * Порядок слотов: [0 топливо, если есть], затем инвентарь игрока (27) и хотбар (9).
 */
public class BoilerMenu extends AbstractContainerMenu {
    public static final int FUEL_X = 56;
    public static final int FUEL_Y = 53;

    /** Температура окружающей среды, K (20 °C). Нагрев 0 соответствует ей. */
    public static final int AMBIENT_K = 293;
    /** Температура при полном нагреве, K. Выбрана так, чтобы пар начинался ровно при 373 K (100 °C): на 1/4 нагрева. */
    public static final int MAX_K = 613;

    private final ContainerData data;
    private final ContainerLevelAccess access;
    @Nullable
    private final Block block;
    private final boolean hasFuelSlot;
    private final int inventoryStart;

    /** Клиентская сторона: буфер говорит, есть ли слот топлива. */
    public BoilerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBoolean() ? new ItemStackHandler(1) : null, new SimpleContainerData(3),
                ContainerLevelAccess.NULL, null);
    }

    public BoilerMenu(int id, Inventory inventory, @Nullable IItemHandler fuel, ContainerData data,
                      ContainerLevelAccess access, @Nullable Block block) {
        super(ModMenus.BOILER.get(), id);
        this.data = data;
        this.access = access;
        this.block = block;
        this.hasFuelSlot = fuel != null;
        this.inventoryStart = hasFuelSlot ? 1 : 0;

        if (fuel != null) {
            addSlot(new SlotItemHandler(fuel, 0, FUEL_X, FUEL_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public boolean hasFuelSlot() {
        return hasFuelSlot;
    }

    /** Нагрев в кельвинах. */
    public int kelvin() {
        int warmup = data.get(1);
        if (warmup <= 0) {
            return AMBIENT_K;
        }
        return AMBIENT_K + data.get(0) * (MAX_K - AMBIENT_K) / warmup;
    }

    /** Температура, с которой идёт пар, K (четверть нагрева). */
    public static int steamStartKelvin() {
        return AMBIENT_K + (MAX_K - AMBIENT_K) / 4;
    }

    /** Заполненная часть шкалы нагрева в пикселях (0..height). */
    public int heatPixels(int height) {
        int warmup = data.get(1);
        return warmup <= 0 ? 0 : Math.min(height, data.get(0) * height / warmup);
    }

    /** Высота пламени в пикселях (0..height). */
    public int flamePixels(int height) {
        return Math.min(height, data.get(2) * height / 1000);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int hotbarStart = inventoryStart + 27;
        int end = hotbarStart + 9;
        if (index < inventoryStart) {
            if (!moveItemStackTo(stack, inventoryStart, end, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!hasFuelSlot || !moveItemStackTo(stack, 0, 1, false)) {
            if (index < hotbarStart) {
                if (!moveItemStackTo(stack, hotbarStart, end, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, inventoryStart, hotbarStart, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return block == null || stillValid(access, player, block);
    }
}
