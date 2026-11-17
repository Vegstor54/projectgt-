package com.vgst54.projectgt.boiler;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Жидкое топливо для котлов: сколько тиков горения даёт 1 mB. Креозот и тяжёлое топливо добавим вместе с ними. */
public final class BoilerFuels {
    private BoilerFuels() {}

    /** Тиков горения на 1 mB, либо 0, если жидкость не топливо. Лава: ведро (1000 mB) горит 20000 тиков. */
    public static int ticksPerMb(Fluid fluid) {
        if (fluid.isSame(Fluids.LAVA)) {
            return 20;
        }
        return 0;
    }
}
