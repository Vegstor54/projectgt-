package com.vgst54.projectgt.boiler;

import java.util.ArrayList;
import java.util.List;

/**
 * Вид одноблочного котла: чем греется × давление.
 *
 * Расход пара в тик при полном нагреве взят из документации GTCEu Modern (L/T = mB/t):
 * твёрдое 6 / 15, солнечный 6 / 18, жидкое 12 / 30 (низкое / высокое давление).
 * Остальные числа (время нагрева, ёмкости, длительность топлива) это баланс, правится здесь.
 */
public record BoilerType(BoilerKind kind, boolean highPressure) {
    private static final List<BoilerType> ALL = build();

    private static List<BoilerType> build() {
        List<BoilerType> list = new ArrayList<>();
        for (boolean high : new boolean[] {false, true}) {
            for (BoilerKind kind : BoilerKind.values()) {
                list.add(new BoilerType(kind, high));
            }
        }
        return List.copyOf(list);
    }

    /** Все виды: сначала низкое давление, затем высокое, внутри по порядку {@link BoilerKind}. */
    public static List<BoilerType> all() {
        return ALL;
    }

    /** id блока, например low_pressure_coal_boiler. */
    public String id() {
        return (highPressure ? "high_pressure_" : "low_pressure_") + kind.id() + "_boiler";
    }

    public String displayName() {
        return (highPressure ? "High Pressure " : "Low Pressure ") + kind.displayName() + " Boiler";
    }

    /** Пар в тик при полном нагреве, mB/t. */
    public int steamPerTick() {
        return switch (kind) {
            case SOLID -> highPressure ? 15 : 6;
            case SOLAR -> highPressure ? 18 : 6;
            case LIQUID -> highPressure ? 30 : 12;
        };
    }

    /** Сколько тиков горения нужно, чтобы нагреться с нуля до полной мощности (долгий нагрев). */
    public int warmupTicks() {
        return highPressure ? 3000 : 2400;
    }

    /** Во сколько раз топливо горит дольше, чем в печи (медленное горение). У высокого давления нет замедления. */
    public int fuelDurationFactor() {
        return highPressure ? 1 : 2;
    }

    public int steamCapacity() {
        return highPressure ? 32_000 : 16_000;
    }

    public int waterCapacity() {
        return highPressure ? 16_000 : 8_000;
    }

    /** Ёмкость бака жидкого топлива; у остальных видов 0. */
    public int fuelCapacity() {
        return kind == BoilerKind.LIQUID ? 4_000 : 0;
    }
}
