package com.vgst54.projectgt.machine.recipe;

import java.util.List;

import javax.annotation.Nullable;

import com.vgst54.projectgt.machine.EnergySource;
import com.vgst54.projectgt.machine.RecipeScaling;
import com.vgst54.projectgt.registry.ModMachineRecipes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Рецептный движок одной машины. Вызывается каждый тик на сервере из блок-сущности.
 *
 * Цикл:
 * 1. Простой: ищем рецепт своего вида, который совпал по входам, проходит проверку тира ({@link RecipeScaling#accepts})
 *    и чьи выходы влезут в выходные слоты. Нашли: списываем входы, запоминаем рецепт.
 * 2. Работа: каждый тик берём энергию у {@link EnergySource}. Хватило на полный тик: прогресс +1.
 *    Не хватило: стоим (прогресс не теряется).
 * 3. Прогресс дошёл до длительности: кладём выходы в выходные слоты. Нет места: ждём, пока освободят.
 *
 * Энергия и тир задаются снаружи: {@link RecipeScaling} (электричество или пар) и {@link EnergySource}.
 */
public final class RecipeProcessor {
    private final MachineType machine;
    private final RecipeScaling scaling;
    private final IItemHandler inputs;
    private final IItemHandler outputs;
    private final EnergySource energy;

    @Nullable
    private MachineRecipe active;
    /** id активного рецепта, нужен для сохранения. */
    @Nullable
    private ResourceLocation activeId;
    /** После загрузки мира рецепт восстанавливаем по id, когда появится level. */
    @Nullable
    private ResourceLocation pendingId;
    private int progress;
    private int duration;
    private long energyPerTick;

    public RecipeProcessor(MachineType machine, RecipeScaling scaling, IItemHandler inputs, IItemHandler outputs,
                           EnergySource energy) {
        this.machine = machine;
        this.scaling = scaling;
        this.inputs = inputs;
        this.outputs = outputs;
        this.energy = energy;
    }

    public boolean isActive() {
        return active != null || pendingId != null;
    }

    public int progress() {
        return progress;
    }

    public int duration() {
        return duration;
    }

    /** Один тик работы. Возвращает true, если состояние изменилось и блок-сущности нужно сохраниться. */
    public boolean tick(Level level) {
        if (pendingId != null) {
            restore(level);
        }
        if (active == null) {
            return start(level);
        }
        if (progress < duration) {
            if (energy.extract(energyPerTick, true) < energyPerTick) {
                return false;
            }
            energy.extract(energyPerTick, false);
            progress++;
        }
        if (progress >= duration && fits(active.outputs())) {
            for (ItemStack stack : active.copyOutputs()) {
                ItemHandlerHelper.insertItem(outputs, stack, false);
            }
            active = null;
            activeId = null;
            progress = 0;
            duration = 0;
        }
        return true;
    }

    private boolean start(Level level) {
        MachineRecipeInput input = MachineRecipeInput.of(inputs);
        List<RecipeHolder<MachineRecipe>> all = level.getRecipeManager().getAllRecipesFor(ModMachineRecipes.type(machine).get());
        for (RecipeHolder<MachineRecipe> holder : all) {
            MachineRecipe recipe = holder.value();
            if (!scaling.accepts(recipe.eut()) || !recipe.matches(input, level) || !fits(recipe.outputs())) {
                continue;
            }
            if (!recipe.consume(inputs)) {
                continue;
            }
            begin(recipe, holder.id());
            return true;
        }
        return false;
    }

    private void begin(MachineRecipe recipe, ResourceLocation id) {
        active = recipe;
        activeId = id;
        progress = 0;
        duration = scaling.duration(recipe.duration());
        energyPerTick = scaling.energyPerTick(recipe.eut());
    }

    /** Влезут ли все выходы в выходные слоты (проверка на копии, поэтому два выхода в один слот считаются честно). */
    private boolean fits(List<ItemStack> stacks) {
        ItemStackHandler copy = new ItemStackHandler(outputs.getSlots());
        for (int slot = 0; slot < outputs.getSlots(); slot++) {
            copy.setStackInSlot(slot, outputs.getStackInSlot(slot).copy());
        }
        for (ItemStack stack : stacks) {
            if (!ItemHandlerHelper.insertItem(copy, stack.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void restore(Level level) {
        ResourceLocation id = pendingId;
        pendingId = null;
        if (id == null) {
            return;
        }
        level.getRecipeManager().byKey(id).ifPresent(holder -> {
            if (holder.value() instanceof MachineRecipe recipe && recipe.machine() == machine) {
                int saved = progress;
                begin(recipe, id);
                progress = Math.min(saved, duration);
            }
        });
        // Рецепт пропал (убрали датапаком): входы уже списаны, потерянный прогресс не восстанавливаем.
        if (active == null) {
            activeId = null;
            progress = 0;
            duration = 0;
        }
    }

    // --- Сохранение ---

    public void save(CompoundTag tag) {
        tag.putInt("Progress", progress);
        if (activeId != null) {
            tag.putString("Recipe", activeId.toString());
        }
    }

    public void load(CompoundTag tag) {
        progress = tag.getInt("Progress");
        active = null;
        activeId = null;
        pendingId = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
    }
}
