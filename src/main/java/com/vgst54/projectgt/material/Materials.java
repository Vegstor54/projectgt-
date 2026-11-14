package com.vgst54.projectgt.material;

import static com.vgst54.projectgt.material.MaterialForm.BOLT;
import static com.vgst54.projectgt.material.MaterialForm.DOUBLE_INGOT;
import static com.vgst54.projectgt.material.MaterialForm.DOUBLE_PLATE;
import static com.vgst54.projectgt.material.MaterialForm.DUST;
import static com.vgst54.projectgt.material.MaterialForm.GEAR;
import static com.vgst54.projectgt.material.MaterialForm.INGOT;
import static com.vgst54.projectgt.material.MaterialForm.PLATE;
import static com.vgst54.projectgt.material.MaterialForm.RAW_ORE;
import static com.vgst54.projectgt.material.MaterialForm.ROD;
import static com.vgst54.projectgt.material.MaterialForm.SCREW;
import static com.vgst54.projectgt.material.MaterialForm.SMALL_GEAR;
import static com.vgst54.projectgt.material.MaterialForm.SMALL_SPRING;
import static com.vgst54.projectgt.material.MaterialForm.SPRING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Список всех материалов мода. Порядок объявления = порядок во вкладке. */
public final class Materials {
    private static final List<Material> ALL = new ArrayList<>();

    // --- Металлы (руда есть). Ванильные: слитков тут нет, они уже есть в игре. ---
    public static final Material IRON   = add(Material.of("iron",   "Iron",   0xC8C8C8,
            RAW_ORE, DUST, DOUBLE_INGOT, PLATE, DOUBLE_PLATE, ROD, BOLT, SCREW, GEAR, SMALL_GEAR, SPRING, SMALL_SPRING));
    public static final Material COPPER = add(Material.of("copper", "Copper", 0xE07040,
            RAW_ORE, DUST, DOUBLE_INGOT, PLATE, DOUBLE_PLATE, ROD, BOLT, SCREW, GEAR, SMALL_GEAR, SPRING, SMALL_SPRING));
    public static final Material TIN    = add(Material.of("tin",    "Tin",    0xEEF3F7,
            RAW_ORE, DUST, INGOT, DOUBLE_INGOT, PLATE, DOUBLE_PLATE, ROD, BOLT, SCREW, GEAR, SMALL_GEAR, SPRING, SMALL_SPRING));

    // --- Золото: руды нет (пока), мягкое, только заготовки. ---
    public static final Material GOLD   = add(Material.of("gold",   "Gold",   0xFFDF3A,
            DUST, DOUBLE_INGOT, PLATE, DOUBLE_PLATE, ROD));

    // --- Сплавы: руды нет, получаются крафтом/смешиванием. ---
    public static final Material BRONZE = add(Material.of("bronze", "Bronze", 0xD08A2E,
            DUST, INGOT, DOUBLE_INGOT, PLATE, DOUBLE_PLATE, ROD, BOLT, SCREW, GEAR, SMALL_GEAR, SPRING, SMALL_SPRING));
    public static final Material STEEL  = add(Material.of("steel",  "Steel",  0x6F7378,
            DUST, INGOT, DOUBLE_INGOT, PLATE, DOUBLE_PLATE, ROD, BOLT, SCREW, GEAR, SMALL_GEAR, SPRING, SMALL_SPRING));

    // --- Кристаллы ---
    public static final Material DIAMOND  = add(Material.of("diamond", "Diamond", 0x58E0D8, IconSet.CRYSTAL,
            RAW_ORE, DUST));

    // --- Минералы ---
    public static final Material REDSTONE = add(Material.of("redstone", "Redstone", 0xE02020, IconSet.MINERAL,
            RAW_ORE));

    private Materials() {}

    private static Material add(Material material) {
        ALL.add(material);
        return material;
    }

    public static List<Material> all() {
        return Collections.unmodifiableList(ALL);
    }
}
