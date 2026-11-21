package com.vgst54.projectgt.multiblock;

import com.vgst54.projectgt.registry.ModBlocks;

/** Формы мультиблоков мода. */
public final class Multiblocks {
    private Multiblocks() {}

    /**
     * Паровой измельчитель: куб 3×3×3 из бронзовых кирпичей, контроллер в центре передней грани, внутри воздух.
     * Любая клетка корпуса может быть люком. Нужно ровно по одному: шина ввода, шина вывода, паровой люк.
     */
    public static MultiblockPattern steamGrinder() {
        return MultiblockPattern.start()
                .aisle("CCC", "C@C", "CCC")
                .aisle("CCC", "C#C", "CCC")
                .aisle("CCC", "CCC", "CCC")
                .where('C', Matcher.blocks("casing", 20, 25, ModBlocks.BRONZE_BRICKS_CASING.get())
                        .or(Matcher.abilities("steam_import_bus", 1, 1, PartAbility.STEAM_IMPORT_ITEMS))
                        .or(Matcher.abilities("steam_export_bus", 1, 1, PartAbility.STEAM_EXPORT_ITEMS))
                        .or(Matcher.abilities("steam_hatch", 1, 1, PartAbility.STEAM)))
                .where('#', Matcher.air())
                .build();
    }
}
