package com.vgst54.projectgt.pipe;

import java.util.Locale;

import com.vgst54.projectgt.material.Material;

/**
 * Один вид предметной трубы: материал × размер.
 * Пропускная способность: stacks стаков за periodSeconds секунд (1/8 стака/с это stacks = 1, periodSeconds = 8).
 * Внутренняя ёмкость равна stacks слотов. baseRoutingValue труба вносит в приоритет пути;
 * в режиме restrictive (включается ключом с shift) он умножается на RESTRICTIVE_MULTIPLIER.
 */
public record ItemPipeType(Material material, PipeSize size, int stacks, int periodSeconds, int baseRoutingValue) {
    public static final int RESTRICTIVE_MULTIPLIER = 100;

    public String id() {
        return material.id() + "_" + size.id() + "_item_pipe";
    }

    public String displayName() {
        return size.displayName() + " " + material.displayName() + " Item Pipe";
    }

    /** Как часто труба раздаёт содержимое. */
    public int intervalTicks() {
        return periodSeconds * 20;
    }

    /** Пропускная способность текстом: "2" или "1/8" (стаков в секунду). */
    public String throughputText() {
        return periodSeconds == 1 ? String.valueOf(stacks) : stacks + "/" + periodSeconds;
    }

    /** Routing value без restrictive, с разделителями тысяч. */
    public String routingValueText() {
        return String.format(Locale.ROOT, "%,d", baseRoutingValue);
    }
}
