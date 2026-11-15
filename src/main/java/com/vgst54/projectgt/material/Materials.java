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

import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Список всех материалов мода. Порядок объявления = порядок во вкладке. */
public final class Materials {
    private static final List<Material> ALL = new ArrayList<>();

    // --- Металлы (руда есть). Ванильные: слитков тут нет, они уже есть в игре. ---
    public static final Material IRON   = add(Material.of("iron",   "Iron",   0xC8C8C8,
            DUST, DOUBLE_INGOT, PLATE, DOUBLE_PLATE, ROD, BOLT, SCREW, GEAR, SMALL_GEAR, SPRING, SMALL_SPRING).withOreColor(0xD9A98A).withVanillaRaw(() -> Items.RAW_IRON));
    public static final Material COPPER = add(Material.of("copper", "Copper", 0xE07040,
            DUST, DOUBLE_INGOT, PLATE, DOUBLE_PLATE, ROD, BOLT, SCREW, GEAR, SMALL_GEAR, SPRING, SMALL_SPRING)
            .withVanillaRaw(() -> Items.RAW_COPPER));
    public static final Material TIN    = add(Material.of("tin",    "Tin",    0xB4BDC9,
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
    // Алмаз: руда дропает ванильный алмаз (своей сырой руды нет)
    public static final Material DIAMOND  = add(Material.of("diamond", "Diamond", 0x58E0D8, IconSet.CRYSTAL,
            DUST).withVanillaRaw(() -> Items.DIAMOND));

    // --- Минералы ---
    public static final Material REDSTONE = add(Material.of("redstone", "Redstone", 0xE02020, IconSet.MINERAL,
            RAW_ORE));

    // --- Минералы-руды (из них добывается металл; сами по себе не слитки) ---
    // Олово и вольфрам: ядро гранитного узла
    public static final Material CASSITERITE  = ore("cassiterite",  "Cassiterite",  0x5A4636, IconSet.METALLIC);
    public static final Material WOLFRAMITE   = ore("wolframite",   "Wolframite",   0x2E2A2C, IconSet.METALLIC);
    // Медь
    public static final Material CHALCOPYRITE = ore("chalcopyrite", "Chalcopyrite", 0xD6B23A, IconSet.METALLIC);
    public static final Material MALACHITE    = ore("malachite",    "Malachite",    0x2FA36B, IconSet.MINERAL);
    // Колчедан: спутник сульфидных жил
    public static final Material PYRITE       = ore("pyrite",       "Pyrite",       0xE0C060, IconSet.METALLIC);
    // Свинец, цинк, серебро
    public static final Material GALENA       = ore("galena",       "Galena",       0x6A7386, IconSet.METALLIC);
    public static final Material SPHALERITE   = ore("sphalerite",   "Sphalerite",   0xA8763A, IconSet.METALLIC);
    public static final Material ARGENTITE    = ore("argentite",    "Argentite",    0x9AA0A8, IconSet.METALLIC);
    // Железо
    public static final Material HEMATITE     = ore("hematite",     "Hematite",     0x8B2E22, IconSet.METALLIC);
    public static final Material MAGNETITE    = ore("magnetite",    "Magnetite",    0x3A3F4A, IconSet.METALLIC);
    public static final Material LIMONITE     = ore("limonite",     "Limonite",     0xB8782E, IconSet.MINERAL); // бурый железняк
    public static final Material SIDERITE     = ore("siderite",     "Siderite",     0xA8895C, IconSet.MINERAL);
    // Никель, хром, марганец, алюминий
    public static final Material PENTLANDITE  = ore("pentlandite",  "Pentlandite",  0xB8A860, IconSet.METALLIC);
    public static final Material CHROMITE     = ore("chromite",     "Chromite",     0x2D3A33, IconSet.METALLIC);
    public static final Material PYROLUSITE   = ore("pyrolusite",   "Pyrolusite",   0x34343E, IconSet.METALLIC);
    public static final Material BAUXITE      = ore("bauxite",      "Bauxite",      0xB85C38, IconSet.MINERAL);
    // Золото (самородное) и ванадиевый магнетит
    public static final Material NATIVE_GOLD  = ore("native_gold",  "Native Gold",  0xFFD83A, IconSet.METALLIC);
    public static final Material VANADIUM_MAGNETITE = ore("vanadium_magnetite", "Vanadium Magnetite", 0x4A4F6A, IconSet.METALLIC);
    // Самоцвет Верхнего мира, сера и кварц Незера (кварц дропает ванильный предмет)
    public static final Material SAPPHIRE     = ore("sapphire",     "Sapphire",     0x2F5BD6, IconSet.CRYSTAL);
    public static final Material SULFUR       = ore("sulfur",       "Sulfur",       0xE6D63A, IconSet.MINERAL);
    // Цеолит и реальгар (спутники халькопирит-касситеритовой жилы)
    public static final Material ZEOLITE      = ore("zeolite",      "Zeolite",      0xE8E4DC, IconSet.MINERAL);
    public static final Material REALGAR      = ore("realgar",      "Realgar",      0xD2452B, IconSet.MINERAL);
    public static final Material QUARTZ       = add(Material.of("quartz", "Quartz", 0xE8E0D8, IconSet.CRYSTAL, DUST)
            .withVanillaRaw(() -> Items.QUARTZ));
    // Уголь: блоки руды дропают ванильный уголь
    public static final Material COAL         = add(Material.of("coal", "Coal", 0x2B2B2B, IconSet.MINERAL, DUST)
            .withVanillaRaw(() -> Items.COAL));
    public static final Material GRAPHITE     = ore("graphite",     "Graphite",     0x3A3A44, IconSet.MINERAL);

    private Materials() {}

    /** Минерал-руда: сырая руда и пыль. */
    private static Material ore(String id, String name, int rgb, IconSet icons) {
        return add(Material.of(id, name, rgb, icons, RAW_ORE, DUST));
    }

    private static Material add(Material material) {
        ALL.add(material);
        return material;
    }

    public static List<Material> all() {
        return Collections.unmodifiableList(ALL);
    }
}
