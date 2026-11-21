package com.vgst54.projectgt.multiblock.part;

import javax.annotation.Nullable;

import com.vgst54.projectgt.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Меню шины: четыре слота в ряд и инвентарь игрока. У шины вывода в слоты ничего не кладётся, только забирается. */
public class PartItemMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 53;
    public static final int SLOT_Y = 35;

    private static final int BUS_SLOTS = MultiblockPartBlockEntity.ITEM_SLOTS;
    private static final int HOTBAR_START = BUS_SLOTS + 27;
    private static final int END = HOTBAR_START + 9;

    private final ContainerLevelAccess access;
    @Nullable
    private final Block block;
    private final boolean export;

    /** Клиентская сторона: буфер говорит, это шина вывода. */
    public PartItemMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, new ItemStackHandler(BUS_SLOTS), buffer.readBoolean(), ContainerLevelAccess.NULL, null);
    }

    public PartItemMenu(int id, Inventory inventory, IItemHandler handler, boolean export, ContainerLevelAccess access,
                        @Nullable Block block) {
        super(ModMenus.PART_ITEMS.get(), id);
        this.access = access;
        this.block = block;
        this.export = export;
        for (int i = 0; i < BUS_SLOTS; i++) {
            addSlot(new SlotItemHandler(handler, i, SLOT_X + i * 18, SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !export && super.mayPlace(stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < BUS_SLOTS) {
            if (!moveItemStackTo(stack, BUS_SLOTS, END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (export || !moveItemStackTo(stack, 0, BUS_SLOTS, false)) {
            if (index < HOTBAR_START) {
                if (!moveItemStackTo(stack, HOTBAR_START, END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, BUS_SLOTS, HOTBAR_START, false)) {
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
