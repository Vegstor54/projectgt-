package com.vgst54.projectgt.machine.recipe;

/** Вид машины для рецептов: у каждого свой тип рецептов (projectgt:&lt;id&gt;) и своя папка json. */
public enum MachineType {
    MACERATOR("macerator", "Macerator"),
    EXTRACTOR("extractor", "Extractor"),
    COMPRESSOR("compressor", "Compressor"),
    FORGE_HAMMER("forge_hammer", "Forge Hammer");

    private final String id;
    private final String displayName;

    MachineType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }
}
