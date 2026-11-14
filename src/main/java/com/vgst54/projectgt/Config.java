package com.vgst54.projectgt;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Общий конфиг мода (projectgt-common.toml). Сюда добавляем все переключатели
 * "крутых опций" по аналогии с GregTech: мир, баланс, совместимость и т.д.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_VANILLA_ORE_GENERATION;

    static {
        BUILDER.push("worldgen");
        ENABLE_VANILLA_ORE_GENERATION = BUILDER
                .comment("Whether vanilla ores (coal, iron, copper, gold, redstone, diamond, lapis, emerald,",
                        "ancient debris, nether gold and quartz) generate in the world.",
                        "Disabled by default: Project-GT+++ generates its own ores instead.",
                        "Changing this requires a world reload (or server restart) to take effect for new chunks.")
                .define("enableVanillaOreGeneration", false);
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
