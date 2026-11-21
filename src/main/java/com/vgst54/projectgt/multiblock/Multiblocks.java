package com.vgst54.projectgt.multiblock;

import com.vgst54.projectgt.registry.ModBlocks;

/** Формы мультиблоков мода. */
public final class Multiblocks {
    private Multiblocks() {}

    /**
     * Паровой измельчитель: куб 3×3×3 из бронзовых кирпичей, контроллер в центре передней грани, внутри воздух.
     * Не меньше 20 корпусов: остальные клетки зарезервированы под люки (пока их нет, нужен полный корпус).
     */
    public static MultiblockPattern steamGrinder() {
        return MultiblockPattern.start()
                .aisle("CCC", "C@C", "CCC")
                .aisle("CCC", "C#C", "CCC")
                .aisle("CCC", "CCC", "CCC")
                .where('C', Matcher.blocks("casing", 20, 26, ModBlocks.BRONZE_BRICKS_CASING.get()))
                .where('#', Matcher.air())
                .build();
    }
}
