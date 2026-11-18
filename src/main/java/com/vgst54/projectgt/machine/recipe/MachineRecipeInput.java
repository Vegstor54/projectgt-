package com.vgst54.projectgt.machine.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.items.IItemHandler;

/** Снимок входных слотов машины для поиска рецепта. Стеки копируются, рецепт ничего не меняет. */
public record MachineRecipeInput(List<ItemStack> items) implements RecipeInput {
    public static MachineRecipeInput of(IItemHandler handler) {
        List<ItemStack> copy = new ArrayList<>(handler.getSlots());
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            copy.add(handler.getStackInSlot(slot).copy());
        }
        return new MachineRecipeInput(copy);
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }
}
