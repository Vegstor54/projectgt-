package com.vgst54.projectgt.client;

import java.util.ArrayList;
import java.util.List;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.item.ToolType;
import com.vgst54.projectgt.material.ToolStats;
import com.vgst54.projectgt.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Описание инструментов мода: назначение, материал, прочность, для кирок и подобных скорость и уровень добычи.
 * Ванильные строки (урон, скорость атаки) остаются как есть, наши вставляются сразу под названием.
 */
@EventBusSubscriber(modid = ProjectGT.MODID, value = Dist.CLIENT)
public final class ToolTooltips {
    private static final String[] TIER_KEYS = {"wood", "stone", "iron", "diamond"};

    private ToolTooltips() {}

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ModItems.ToolInfo info = ModItems.toolInfo(stack.getItem());
        if (info == null) {
            return;
        }
        ToolType type = info.type();
        ToolStats stats = info.material().tool();

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(type.descriptionKey()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.projectgt.tool.material", info.material().displayName())
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.projectgt.tool.durability",
                stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()).withStyle(ChatFormatting.AQUA));
        if (type.isDigger()) {
            lines.add(Component.translatable("tooltip.projectgt.tool.mining_speed", stats.speed())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            lines.add(Component.translatable("tooltip.projectgt.tool.mining_level",
                            Component.translatable("tooltip.projectgt.tier." + TIER_KEYS[Math.min(stats.tier(), 3)]))
                    .withStyle(ChatFormatting.YELLOW));
        }
        if (type.usesDurabilityInCrafting()) {
            lines.add(Component.translatable("tooltip.projectgt.tool.crafting_wear").withStyle(ChatFormatting.DARK_GRAY));
        }
        event.getToolTip().addAll(1, lines);
    }
}
