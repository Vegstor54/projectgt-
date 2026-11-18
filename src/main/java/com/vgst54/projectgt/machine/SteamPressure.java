package com.vgst54.projectgt.machine;

/**
 * Вариант парового тира: низкое или высокое давление.
 *
 * Рецепты общие с электрическими, в EU/t и ticks, паровые машины тянут рецепты до LV (32 EU/t).
 * Пересчёт (числа сверены с GTCEu Modern по карточкам кузнечного молота, рецепт 16 EU/t, 50 с):
 * - длительность = ticks * DURATION_MULTIPLIER / speed (низкое давление 100 с, высокое 50 с);
 * - пар в тик = EU/t * STEAM_PER_EU * speed / DURATION_MULTIPLIER (16 mB/t и 32 mB/t);
 * - пара на рецепт в сумме STEAM_PER_EU * EU рецепта, одинаково при любом давлении (32 000 mB на 16 000 EU).
 *
 * Высокое давление быстрее вдвое и тратит вдвое больше пара в тик, на рецепт пара столько же.
 * Числа можно править при балансе, это единственное место.
 */
public enum SteamPressure {
    LOW(1),
    HIGH(2);

    /** Сколько mB пара стоит один EU рецепта (в сумме на рецепт). */
    public static final int STEAM_PER_EU = 2;
    /** Во сколько раз паровая машина медленнее электрической при низком давлении. */
    public static final int DURATION_MULTIPLIER = 2;
    /** Паровые машины тянут рецепты до LV. */
    public static final long MAX_RECIPE_EUT = Tier.LV.voltage();

    private final int speed;

    SteamPressure(int speed) {
        this.speed = speed;
    }

    /** Множитель скорости: ×1 для низкого давления, ×2 для высокого. */
    public int speed() {
        return speed;
    }

    /** Тянет ли паровая машина рецепт с таким расходом (не выше LV). */
    public static boolean accepts(long recipeEut) {
        return recipeEut <= MAX_RECIPE_EUT;
    }

    /** Расход пара в тик (mB/t) для рецепта с расходом recipeEut EU/t. */
    public long steamPerTick(long recipeEut) {
        return recipeEut * STEAM_PER_EU * speed / DURATION_MULTIPLIER;
    }

    /** Длительность рецепта на паровой машине, в тиках. */
    public int duration(int recipeTicks) {
        return Math.max(1, recipeTicks * DURATION_MULTIPLIER / speed);
    }

    /** Весь пар на один рецепт, mB. */
    public long totalSteam(long recipeEut, int recipeTicks) {
        return steamPerTick(recipeEut) * duration(recipeTicks);
    }
}
