package com.vgst54.projectgt.machine;

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
 * Меню паровой машины: слот входа, слот выхода (только забрать), инвентарь игрока.
 * Порядок слотов: 0 вход, 1 выход, 2..28 инвентарь, 29..37 хотбар.
 */
public class SteamMachineMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 56;
    public static final int OUTPUT_X = 116;
    public static final int SLOT_Y = 35;

    private static final int INPUT_SLOT = 0;
    private static final int INVENTORY_START = 2;
    private static final int HOTBAR_START = 29;
    private static final int HOTBAR_END = 38;

    private final ContainerData data;
    private final ContainerLevelAccess access;
    @Nullable
    private final Block block;

    /** Клиентская сторона: данные придут синхронизацией слотов и ContainerData. */
    public SteamMachineMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, new ItemStackHandler(1), new ItemStackHandler(1), new SimpleContainerData(3),
                ContainerLevelAccess.NULL, null);
    }

    public SteamMachineMenu(int id, Inventory inventory, IItemHandler input, IItemHandler output, ContainerData data,
                            ContainerLevelAccess access, @Nullable Block block) {
        super(ModMenus.STEAM_MACHINE.get(), id);
        this.data = data;
        this.access = access;
        this.block = block;

        addSlot(new SlotItemHandler(input, 0, INPUT_X, SLOT_Y));
        addSlot(new SlotItemHandler(output, 0, OUTPUT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
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

    public int progress() {
        return data.get(0);
    }

    public int duration() {
        return data.get(1);
    }

    public int steam() {
        return data.get(2);
    }

    /** Заполненная часть шкалы прогресса в пикселях (0..width). */
    public int progressPixels(int width) {
        int duration = duration();
        return duration <= 0 ? 0 : Math.min(width, progress() * width / duration);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < INVENTORY_START) {
            // Из машины в инвентарь игрока.
            if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
            // Не влезло во вход: перекладываем между инвентарём и хотбаром.
            if (index < HOTBAR_START) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) {
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
