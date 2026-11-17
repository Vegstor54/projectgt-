package com.vgst54.projectgt.machine;

/**
 * Вариант парового тира: низкое или высокое давление.
 *
 * Рецепты общие с электрическими, в EU/t и ticks. Паровая машина пересчитывает их так:
 * - пар в тик = EU/t * STEAM_PER_EU * speed;
 * - длительность = ticks * DURATION_MULTIPLIER / speed.
 *
 * Итого на рецепт уходит пара в 4 раза больше, чем EU (2 mB за EU и вдвое дольше), как в GT5.
 * Высокое давление идёт вдвое быстрее и тратит вдвое больше пара в тик, так что пара на рецепт
 * столько же, просто быстрее. Числа можно править при балансе, это единственное место.
 */
public enum SteamPressure {
    LOW(1),
    HIGH(2);

    /** Сколько mB пара стоит один EU. */
    public static final int STEAM_PER_EU = 2;
    /** Во сколько раз паровая машина медленнее электрической при низком давлении. */
    public static final int DURATION_MULTIPLIER = 2;

    private final int speed;

    SteamPressure(int speed) {
        this.speed = speed;
    }

    /** Множитель скорости: ×1 для низкого давления, ×2 для высокого. */
    public int speed() {
        return speed;
    }

    /** Тянет ли паровой тир рецепт с таким расходом (не выше напряжения SV). */
    public static boolean accepts(long recipeEut) {
        return recipeEut <= Tier.SV.voltage();
    }

    /** Расход пара в тик (mB/t) для рецепта с расходом recipeEut EU/t. */
    public long steamPerTick(long recipeEut) {
        return recipeEut * STEAM_PER_EU * speed;
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
