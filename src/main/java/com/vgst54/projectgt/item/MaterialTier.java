package com.vgst54.projectgt.item;

import com.vgst54.projectgt.material.ToolStats;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Tier для инструментов из материала. Починка наковальней отключена (пустой ингредиент). */
public record MaterialTier(ToolStats stats) implements Tier {
    @Override
    public int getUses() {
        return stats.durability();
    }

    @Override
    public float getSpeed() {
        return stats.speed();
    }

    @Override
    public float getAttackDamageBonus() {
        return stats.tier();
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return switch (stats.tier()) {
            case 0 -> BlockTags.INCORRECT_FOR_WOODEN_TOOL;
            case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
            case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
            default -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 10;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
}
