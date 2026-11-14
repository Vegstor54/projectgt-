package com.vgst54.projectgt.material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Материал: идентификатор, отображаемое имя, цвет тонировки, набор иконок
 * и набор форм, в которых он существует как предмет.
 */
public record Material(String id, String displayName, int color, IconSet iconSet, Set<MaterialForm> forms) {

    /** Материал с набором иконок METALLIC. */
    public static Material of(String id, String displayName, int rgb, MaterialForm first, MaterialForm... rest) {
        return of(id, displayName, rgb, IconSet.METALLIC, first, rest);
    }

    public static Material of(String id, String displayName, int rgb, IconSet iconSet,
                              MaterialForm first, MaterialForm... rest) {
        EnumSet<MaterialForm> set = EnumSet.of(first, rest);
        return new Material(id, displayName, rgb, iconSet, Collections.unmodifiableSet(set));
    }

    public boolean has(MaterialForm form) {
        return forms.contains(form);
    }

    /** ARGB для тонировки предметов. */
    public int tint() {
        return 0xFF000000 | color;
    }
}
