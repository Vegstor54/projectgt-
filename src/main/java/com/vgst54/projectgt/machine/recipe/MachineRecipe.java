package com.vgst54.projectgt.machine.recipe;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.vgst54.projectgt.machine.Tier;
import com.vgst54.projectgt.registry.ModMachineRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Рецепт машины: входы (ингредиент × количество), выходы, расход EU/t и длительность в тиках.
 * Один и тот же рецепт годится и электрической, и паровой машине, пересчёт делает {@link RecipeScaling}.
 *
 * Проверка тира: {@link #tier()} это минимальный тир, который тянет такой расход.
 */
public final class MachineRecipe implements Recipe<MachineRecipeInput> {
    private final MachineType machine;
    private final List<SizedIngredient> inputs;
    private final List<ItemStack> outputs;
    private final int eut;
    private final int duration;

    public MachineRecipe(MachineType machine, List<SizedIngredient> inputs, List<ItemStack> outputs, int eut, int duration) {
        this.machine = machine;
        this.inputs = List.copyOf(inputs);
        this.outputs = List.copyOf(outputs);
        this.eut = eut;
        this.duration = duration;
    }

    public MachineType machine() {
        return machine;
    }

    public List<SizedIngredient> inputs() {
        return inputs;
    }

    public List<ItemStack> outputs() {
        return outputs;
    }

    /** Расход энергии, EU/t. */
    public int eut() {
        return eut;
    }

    /** Длительность на электрической машине, тиков. */
    public int duration() {
        return duration;
    }

    /** Минимальный электрический тир для этого рецепта. */
    public Tier tier() {
        return Tier.forVoltage(eut);
    }

    // --- Ресурсы ---

    /**
     * План списания: сколько взять из каждого слота (индекс слота совпадает с индексом в stacks),
     * либо null, если входов не хватает. Один и тот же стек не списывается дважды для разных ингредиентов.
     */
    @Nullable
    public int[] plan(List<ItemStack> stacks) {
        int[] left = new int[stacks.size()];
        for (int i = 0; i < left.length; i++) {
            left[i] = stacks.get(i).getCount();
        }
        int[] take = new int[stacks.size()];
        for (SizedIngredient input : inputs) {
            int need = input.count();
            for (int slot = 0; slot < left.length && need > 0; slot++) {
                if (left[slot] > 0 && input.ingredient().test(stacks.get(slot))) {
                    int part = Math.min(need, left[slot]);
                    left[slot] -= part;
                    take[slot] += part;
                    need -= part;
                }
            }
            if (need > 0) {
                return null;
            }
        }
        return take;
    }

    /** Списывает входы из слотов машины. Возвращает false (и ничего не списывает), если входов не хватает. */
    public boolean consume(IItemHandler handler) {
        int[] take = plan(MachineRecipeInput.of(handler).items());
        if (take == null) {
            return false;
        }
        for (int slot = 0; slot < take.length; slot++) {
            if (take[slot] > 0) {
                handler.extractItem(slot, take[slot], false);
            }
        }
        return true;
    }

    // --- Recipe ---

    @Override
    public boolean matches(MachineRecipeInput input, Level level) {
        return plan(input.items()) != null;
    }

    @Override
    public ItemStack assemble(MachineRecipeInput input, HolderLookup.Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModMachineRecipes.serializer(machine).get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModMachineRecipes.type(machine).get();
    }

    /** Копии выходов, чтобы их можно было отдавать в слоты. */
    public List<ItemStack> copyOutputs() {
        List<ItemStack> copy = new ArrayList<>(outputs.size());
        for (ItemStack stack : outputs) {
            copy.add(stack.copy());
        }
        return copy;
    }
}
