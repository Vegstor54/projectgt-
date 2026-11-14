package com.vgst54.projectgt.material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Материал: идентификатор, отображаемое имя, цвет тонировки и набор форм,
 * в которых он существует как предмет.
 */
public record Material(String id, String displayName, int color, Set<MaterialForm> forms) {

    public static Material of(String id, String displayName, int rgb, MaterialForm first, MaterialForm... rest) {
        EnumSet<MaterialForm> set = EnumSet.of(first, rest);
        return new Material(id, displayName, rgb, Collections.unmodifiableSet(set));
    }

    public boolean has(MaterialForm form) {
        return forms.contains(form);
    }

    /** ARGB для тонировки предметов. */
    public int tint() {
        return 0xFF000000 | color;
    }
}
