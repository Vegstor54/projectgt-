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
 * Рецептный движок одной машины или мультиблока. Вызывается каждый тик на сервере из блок-сущности.
 *
 * Цикл:
 * 1. Простой: ищем рецепт своего вида, который совпал по входам, проходит проверку тира ({@link RecipeScaling#accepts}),
 *    чьи выходы влезут в выходные слоты и на чей первый тик хватает энергии. Нашли: списываем входы, запоминаем рецепт.
 * 2. Работа: каждый тик берём энергию у {@link EnergySource}. Хватило на полный тик: прогресс +1.
 *    Не хватило: прогресс сбрасывается в 0 (входы не возвращаются), после возвращения питания ждём
 *    {@link #RESTART_DELAY} тиков и рецепт идёт заново.
 * 3. Прогресс дошёл до длительности: кладём выходы в выходные слоты. Нет места: ждём, пока освободят.
 *
 * Параллели (как в GT): maxParallels это фиксированное число контроллера (у одиночных машин 1). При старте один и тот же
 * рецепт запускается сразу N раз, где N = min(maxParallels, на сколько запусков хватает входов, на сколько влезают
 * выходы). Входы, выходы и расход энергии (у пара mB) умножаются на N, длительность не меняется. Слоты люков
 * ограничивают параллели через входы и выходы. Проверка тира смотрит на расход одного запуска.
 *
 * Энергия и тир задаются снаружи: {@link RecipeScaling} (электричество или пар) и {@link EnergySource}.
 */
public final class RecipeProcessor {
    /** Пауза в тиках после пропажи питания, прежде чем рецепт начнётся заново (20 тиков = 1 секунда). */
    public static final int RESTART_DELAY = 40;

    private final MachineType machine;
    private final RecipeScaling scaling;
    private final IItemHandler inputs;
    private final IItemHandler outputs;
    private final EnergySource energy;
    private final int maxParallels;

    @Nullable
    private MachineRecipe active;
    /** id активного рецепта, нужен для сохранения. */
    @Nullable
    private ResourceLocation activeId;
    /** После загрузки мира рецепт восстанавливаем по id, когда появится level. */
    @Nullable
    private ResourceLocation pendingId;
    private int progress;
    /** Сколько тиков ещё ждать после возвращения питания. Не сохраняется: после загрузки мира пауза не нужна. */
    private int restartDelay;
    private int duration;
    /** Сколько запусков рецепта идёт сейчас. */
    private int parallels;
    /** Энергия за тик на все идущие запуски вместе. */
    private long energyPerTick;

    public RecipeProcessor(MachineType machine, RecipeScaling scaling, IItemHandler inputs, IItemHandler outputs,
                           EnergySource energy) {
        this(machine, scaling, inputs, outputs, energy, 1);
    }

    public RecipeProcessor(MachineType machine, RecipeScaling scaling, IItemHandler inputs, IItemHandler outputs,
                           EnergySource energy, int maxParallels) {
        this.machine = machine;
        this.scaling = scaling;
        this.inputs = inputs;
        this.outputs = outputs;
        this.energy = energy;
        this.maxParallels = Math.max(1, maxParallels);
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

    /** id рецепта в работе или null в простое. */
    @Nullable
    public ResourceLocation recipeId() {
        return activeId != null ? activeId : pendingId;
    }

    /** Сколько запусков рецепта идёт сейчас (0 в простое). */
    public int parallels() {
        return parallels;
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
                // Питание пропало: прогресс сбрасывается, входы остаются списанными.
                boolean changed = progress != 0;
                progress = 0;
                restartDelay = RESTART_DELAY;
                return changed;
            }
            if (restartDelay > 0) {
                // Питание вернулось: ждём паузу, потом рецепт идёт заново с нуля.
                restartDelay--;
                return false;
            }
            energy.extract(energyPerTick, false);
            progress++;
        }
        if (progress >= duration && fits(active.copyOutputs(parallels))) {
            for (ItemStack stack : active.copyOutputs(parallels)) {
                ItemHandlerHelper.insertItem(outputs, stack, false);
            }
            active = null;
            activeId = null;
            progress = 0;
            duration = 0;
            parallels = 0;
        }
        return true;
    }

    private boolean start(Level level) {
        MachineRecipeInput input = MachineRecipeInput.of(inputs);
        List<RecipeHolder<MachineRecipe>> all = level.getRecipeManager().getAllRecipesFor(ModMachineRecipes.type(machine).get());
        for (RecipeHolder<MachineRecipe> holder : all) {
            MachineRecipe recipe = holder.value();
            if (!scaling.accepts(recipe.eut())) {
                continue;
            }
            // Сначала по входам, затем урезаем так, чтобы выходы влезли.
            int times = recipe.maxParallels(input.items(), maxParallels);
            while (times > 0 && !fits(recipe.copyOutputs(times))) {
                times--;
            }
            // Стартуем, только если энергии хватает хотя бы на первый тик: иначе входы сгорели бы впустую.
            long perRun = scaling.energyPerTick(recipe.eut());
            while (times > 0 && energy.extract(perRun * times, true) < perRun * times) {
                times--;
            }
            if (times == 0 || !recipe.consume(inputs, times)) {
                continue;
            }
            begin(recipe, holder.id(), times);
            return true;
        }
        return false;
    }

    private void begin(MachineRecipe recipe, ResourceLocation id, int times) {
        active = recipe;
        activeId = id;
        progress = 0;
        parallels = times;
        duration = scaling.duration(recipe.duration());
        energyPerTick = scaling.energyPerTick(recipe.eut()) * times;
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
        int savedProgress = progress;
        int savedParallels = parallels;
        level.getRecipeManager().byKey(id).ifPresent(holder -> {
            if (holder.value() instanceof MachineRecipe recipe && recipe.machine() == machine) {
                begin(recipe, id, Math.max(1, savedParallels));
                progress = Math.min(savedProgress, duration);
            }
        });
        // Рецепт пропал (убрали датапаком): входы уже списаны, потерянный прогресс не восстанавливаем.
        if (active == null) {
            activeId = null;
            progress = 0;
            duration = 0;
            parallels = 0;
        }
    }

    // --- Сохранение ---

    public void save(CompoundTag tag) {
        tag.putInt("Progress", progress);
        tag.putInt("Parallels", parallels);
        if (activeId != null) {
            tag.putString("Recipe", activeId.toString());
        }
    }

    public void load(CompoundTag tag) {
        progress = tag.getInt("Progress");
        parallels = tag.getInt("Parallels");
        active = null;
        activeId = null;
        pendingId = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
    }
}
