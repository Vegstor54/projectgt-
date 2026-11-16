package com.vgst54.projectgt.pipe;

import java.util.Locale;

import com.vgst54.projectgt.material.Material;

/**
 * Один вид предметной трубы: материал × размер × (обычная или restrictive).
 * Пропускная способность: stacks стаков за periodSeconds секунд (1/8 стака/с это stacks = 1, periodSeconds = 8).
 * Внутренняя ёмкость равна stacks слотов. routingValue труба вносит в приоритет пути; у restrictive ×100.
 */
public record ItemPipeType(Material material, PipeSize size, int stacks, int periodSeconds,
                           int baseRoutingValue, boolean restrictive) {
    public static final int RESTRICTIVE_MULTIPLIER = 100;

    public String id() {
        return material.id() + "_" + size.id() + (restrictive ? "_restrictive" : "") + "_item_pipe";
    }

    public String displayName() {
        return size.displayName() + " " + (restrictive ? "Restrictive " : "") + material.displayName() + " Item Pipe";
    }

    public int routingValue() {
        return restrictive ? baseRoutingValue * RESTRICTIVE_MULTIPLIER : baseRoutingValue;
    }

    /** Как часто труба раздаёт содержимое. */
    public int intervalTicks() {
        return periodSeconds * 20;
    }

    /** Пропускная способность текстом: "2" или "1/8" (стаков в секунду). */
    public String throughputText() {
        return periodSeconds == 1 ? String.valueOf(stacks) : stacks + "/" + periodSeconds;
    }

    public String routingValueText() {
        return String.format(Locale.ROOT, "%,d", routingValue());
    }
}
