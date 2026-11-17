package com.vgst54.projectgt.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Ручной инструмент для рецептов (кувалда, пила, напильник и т.д.). Остаётся в сетке крафта
 * и теряет единицу прочности при каждом крафте, как в GT. Рецепты, которым нужен инструмент,
 * берут его по тегу projectgt:tools/<тип>.
 */
public class CraftingToolItem extends Item {
    public CraftingToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setDamageValue(copy.getDamageValue() + 1);
        return copy.getDamageValue() >= copy.getMaxDamage() ? ItemStack.EMPTY : copy;
    }
}
