package com.vgst54.projectgt.client;

import com.vgst54.projectgt.boiler.BoilerMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Экран парового котла. Рисуется без текстур-файлов: панель, ячейки, пламя над слотом топлива
 * (как в печке) и шкала нагрева; при наведении на шкалу показывается температура в кельвинах.
 */
public class BoilerScreen extends AbstractContainerScreen<BoilerMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int BAR_BG = 0xFF373737;
    private static final int FLAME = 0xFFFF8A1F;
    private static final int HEAT = 0xFFD8402A;
    private static final int WATER = 0xFF3F76E4;

    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;
    private static final int FLAME_W = 14;
    private static final int FLAME_H = 14;

    private static final int HEAT_X = 116;
    private static final int HEAT_Y = 18;
    private static final int HEAT_W = 12;
    private static final int HEAT_H = 52;

    private static final int WATER_X = 140;
    private static final int WATER_Y = 18;
    private static final int WATER_W = 12;
    private static final int WATER_H = 52;

    public BoilerScreen(BoilerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL_DARK);
        graphics.fill(x, y, x + imageWidth - 1, y + imageHeight - 1, PANEL_LIGHT);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL);

        for (Slot slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, PANEL_DARK);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 17, y + slot.y + 17, PANEL_LIGHT);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, SLOT_BG);
        }

        // Пламя: заполняется снизу и убывает вверх по мере выгорания топлива
        graphics.fill(x + FLAME_X + 1, y + FLAME_Y, x + FLAME_X + 1 + FLAME_W, y + FLAME_Y + FLAME_H, BAR_BG);
        int flame = menu.flamePixels(FLAME_H);
        graphics.fill(x + FLAME_X + 1, y + FLAME_Y + FLAME_H - flame,
                x + FLAME_X + 1 + FLAME_W, y + FLAME_Y + FLAME_H, FLAME);

        // Шкала нагрева (снизу вверх)
        graphics.fill(x + HEAT_X, y + HEAT_Y, x + HEAT_X + HEAT_W, y + HEAT_Y + HEAT_H, BAR_BG);
        int heat = menu.heatPixels(HEAT_H);
        graphics.fill(x + HEAT_X, y + HEAT_Y + HEAT_H - heat, x + HEAT_X + HEAT_W, y + HEAT_Y + HEAT_H, HEAT);
        // Шкала воды (снизу вверх)
        graphics.fill(x + WATER_X, y + WATER_Y, x + WATER_X + WATER_W, y + WATER_Y + WATER_H, BAR_BG);
        int water = menu.waterPixels(WATER_H);
        graphics.fill(x + WATER_X, y + WATER_Y + WATER_H - water, x + WATER_X + WATER_W, y + WATER_Y + WATER_H, WATER);
        // Метка, с которой начинается пар (четверть шкалы)
        int markY = y + HEAT_Y + HEAT_H - HEAT_H / 4;
        graphics.fill(x + HEAT_X - 2, markY, x + HEAT_X + HEAT_W + 2, markY + 1, PANEL_DARK);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(HEAT_X, HEAT_Y, HEAT_W, HEAT_H, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, java.util.List.of(
                    Component.translatable("gui.projectgt.boiler.heat", menu.kelvin()),
                    Component.translatable("gui.projectgt.boiler.steam_from", BoilerMenu.steamStartKelvin())),
                    mouseX, mouseY);
        }
        if (isHovering(WATER_X, WATER_Y, WATER_W, WATER_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.projectgt.boiler.water",
                    menu.water(), menu.waterCapacity()), mouseX, mouseY);
        }
    }
}
