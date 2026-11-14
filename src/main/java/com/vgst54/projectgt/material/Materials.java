package com.vgst54.projectgt.material;

import static com.vgst54.projectgt.material.MaterialForm.DUST;
import static com.vgst54.projectgt.material.MaterialForm.INGOT;
import static com.vgst54.projectgt.material.MaterialForm.PLATE;
import static com.vgst54.projectgt.material.MaterialForm.ROD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Список всех материалов мода. Порядок объявления = порядок во вкладке. */
public final class Materials {
    private static final List<Material> ALL = new ArrayList<>();

    // Ванильные металлы: слитки уже есть в игре, добавляем только остальные формы.
    public static final Material IRON   = add(Material.of("iron",   "Iron",   0xC8C8C8, DUST, PLATE, ROD));
    public static final Material COPPER = add(Material.of("copper", "Copper", 0xE07040, DUST, PLATE, ROD));
    public static final Material GOLD   = add(Material.of("gold",   "Gold",   0xFFDF3A, DUST, PLATE, ROD));

    // Свои металлы: все формы.
    public static final Material TIN    = add(Material.of("tin",    "Tin",    0xD3D4D5, DUST, INGOT, PLATE, ROD));
    public static final Material BRONZE = add(Material.of("bronze", "Bronze", 0xD08A2E, DUST, INGOT, PLATE, ROD));
    public static final Material STEEL  = add(Material.of("steel",  "Steel",  0x6F7378, DUST, INGOT, PLATE, ROD));

    private Materials() {}

    private static Material add(Material material) {
        ALL.add(material);
        return material;
    }

    public static List<Material> all() {
        return Collections.unmodifiableList(ALL);
    }
}
