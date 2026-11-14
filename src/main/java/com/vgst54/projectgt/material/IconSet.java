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
    /** Металлы: тёмный самородок. */
    METALLIC("metallic", EnumSet.of(MaterialForm.RAW_ORE)),
    /** Самоцветы и кристаллы. */
    CRYSTAL("crystal", EnumSet.of(MaterialForm.RAW_ORE)),
    /** Минералы: рыхлая порода. */
    MINERAL("mineral", EnumSet.of(MaterialForm.RAW_ORE));

    private final String folder;
    private final Set<MaterialForm> overrides;

    IconSet(String folder, Set<MaterialForm> overrides) {
        this.folder = folder;
        this.overrides = overrides;
    }

    public ResourceLocation texture(MaterialForm form) {
        String path = overrides.contains(form)
                ? "item/material/" + folder + "/" + form.suffix()
                : "item/material/" + form.suffix();
        return ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, path);
    }
}
