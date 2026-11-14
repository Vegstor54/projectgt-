package com.vgst54.projectgt.material;

import java.util.EnumSet;
import java.util.Set;

import com.vgst54.projectgt.ProjectGT;

import net.minecraft.resources.ResourceLocation;

/**
 * Набор иконок: определяет, какие формы у материала имеют собственную текстуру.
 * Остальные формы используют общую серую текстуру (item/material/&lt;форма&gt;).
 */
public enum IconSet {
    /** Металлы: сырая руда берёт ванильную текстуру raw_iron (по ссылке, файл не копируется). */
    METALLIC("metallic", EnumSet.of(MaterialForm.RAW_ORE), "item/raw_iron"),
    /** Самоцветы и кристаллы. */
    CRYSTAL("crystal", EnumSet.of(MaterialForm.RAW_ORE), null),
    /** Минералы: рыхлая порода. */
    MINERAL("mineral", EnumSet.of(MaterialForm.RAW_ORE), null);

    private final String folder;
    private final Set<MaterialForm> overrides;
    /** Ванильная текстура сырой руды для этого набора (minecraft:...), либо null — тогда своя. */
    private final String vanillaRawOre;

    IconSet(String folder, Set<MaterialForm> overrides, String vanillaRawOre) {
        this.folder = folder;
        this.overrides = overrides;
        this.vanillaRawOre = vanillaRawOre;
    }

    /** Накладка с жилами для блоков руды (серая, тонируется цветом материала). */
    public ResourceLocation oreOverlay() {
        return ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "block/ore_overlay/" + folder);
    }

    public ResourceLocation texture(MaterialForm form) {
        if (form == MaterialForm.RAW_ORE && vanillaRawOre != null) {
            return ResourceLocation.withDefaultNamespace(vanillaRawOre);
        }
        String path = overrides.contains(form)
                ? "item/material/" + folder + "/" + form.suffix()
                : "item/material/" + form.suffix();
        return ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, path);
    }
}
