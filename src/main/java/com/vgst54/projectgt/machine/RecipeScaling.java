package com.vgst54.projectgt.machine;

/**
 * Как конкретная машина читает рецепт (рецепты общие, в EU/t и тиках):
 * тянет ли она такой расход, сколько длится рецепт и сколько EU забирает за тик.
 * Сколько всего EU уйдёт на рецепт, не зависит от машины: energyPerTick × duration = EU/t × тики.
 */
public interface RecipeScaling {
    /** Тянет ли машина рецепт с таким расходом (проверка тира напряжения). */
    boolean accepts(long recipeEut);

    /** Длительность рецепта на этой машине, тиков. */
    int duration(int recipeTicks);

    /** Сколько EU забирать у источника за тик (округляется вверх, рецепт не бывает дешевле). */
    long energyPerTick(long recipeEut);

    /** Электрическая машина тира tier: рецепт идёт как написан, тянет расход до напряжения тира. */
    static RecipeScaling electric(Tier tier) {
        return new RecipeScaling() {
            @Override
            public boolean accepts(long recipeEut) {
                return recipeEut <= tier.voltage();
            }

            @Override
            public int duration(int recipeTicks) {
                return recipeTicks;
            }

            @Override
            public long energyPerTick(long recipeEut) {
                return recipeEut;
            }
        };
    }

    /**
     * Паровая машина: рецепты до LV, на низком давлении вдвое дольше. EU в тик вдвое меньше на низком давлении,
     * источник пара переводит EU в mB (2 mB за EU), получаются 16 и 32 mB/t для рецепта 16 EU/t.
     */
    static RecipeScaling steam(SteamPressure pressure) {
        return new RecipeScaling() {
            @Override
            public boolean accepts(long recipeEut) {
                return SteamPressure.accepts(recipeEut);
            }

            @Override
            public int duration(int recipeTicks) {
                return pressure.duration(recipeTicks);
            }

            @Override
            public long energyPerTick(long recipeEut) {
                return Math.ceilDiv(recipeEut * pressure.speed(), (long) SteamPressure.DURATION_MULTIPLIER);
            }
        };
    }
}
