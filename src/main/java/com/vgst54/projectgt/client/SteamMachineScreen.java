package com.vgst54.projectgt.client;

import com.vgst54.projectgt.machine.SteamMachineBlockEntity;
import com.vgst54.projectgt.machine.SteamMachineMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Экран паровой машины. Рисуется без текстур-файлов: панель, ячейки, стрелка прогресса и шкала пара.
 */
public class SteamMachineScreen extends AbstractContainerScreen<SteamMachineMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int BAR_BG = 0xFF373737;
    private static final int PROGRESS = 0xFF3FA13F;
    private static final int STEAM = 0xFFE8EEF2;

    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 39;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 8;

    private static final int STEAM_X = 150;
    private static final int STEAM_Y = 18;
    private static final int STEAM_W = 12;
    private static final int STEAM_H = 52;

    public SteamMachineScreen(SteamMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // Панель с объёмной рамкой
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL_DARK);
        graphics.fill(x, y, x + imageWidth - 1, y + imageHeight - 1, PANEL_LIGHT);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL);

        for (Slot slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, PANEL_DARK);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 17, y + slot.y + 17, PANEL_LIGHT);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, SLOT_BG);
        }

        // Стрелка прогресса
        graphics.fill(x + ARROW_X, y + ARROW_Y, x + ARROW_X + ARROW_W, y + ARROW_Y + ARROW_H, BAR_BG);
        graphics.fill(x + ARROW_X, y + ARROW_Y, x + ARROW_X + menu.progressPixels(ARROW_W), y + ARROW_Y + ARROW_H, PROGRESS);

        // Шкала пара (снизу вверх)
        graphics.fill(x + STEAM_X, y + STEAM_Y, x + STEAM_X + STEAM_W, y + STEAM_Y + STEAM_H, BAR_BG);
        int filled = Math.min(STEAM_H, menu.steam() * STEAM_H / SteamMachineBlockEntity.STEAM_CAPACITY);
        graphics.fill(x + STEAM_X, y + STEAM_Y + STEAM_H - filled, x + STEAM_X + STEAM_W, y + STEAM_Y + STEAM_H, STEAM);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(STEAM_X, STEAM_Y, STEAM_W, STEAM_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.projectgt.machine.steam",
                    menu.steam(), SteamMachineBlockEntity.STEAM_CAPACITY), mouseX, mouseY);
        }
    }
}
