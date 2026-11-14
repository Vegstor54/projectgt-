package com.vgst54.projectgt.material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Материал: идентификатор, отображаемое имя, цвет тонировки, цвет руды (по умолчанию тот же),
 * набор иконок и набор форм, в которых он существует как предмет.
 */
public record Material(String id, String displayName, int color, int oreColor, IconSet iconSet, Set<MaterialForm> forms) {

    /** Материал с набором иконок METALLIC. */
    public static Material of(String id, String displayName, int rgb, MaterialForm first, MaterialForm... rest) {
        return of(id, displayName, rgb, IconSet.METALLIC, first, rest);
    }

    public static Material of(String id, String displayName, int rgb, IconSet iconSet,
                              MaterialForm first, MaterialForm... rest) {
        EnumSet<MaterialForm> set = EnumSet.of(first, rest);
        return new Material(id, displayName, rgb, rgb, iconSet, Collections.unmodifiableSet(set));
    }

    /** Отдельный цвет для руды (блоки руды и сырая руда), если он должен отличаться от цвета металла. */
    public Material withOreColor(int rgb) {
        return new Material(id, displayName, color, rgb, iconSet, forms);
    }

    public boolean has(MaterialForm form) {
        return forms.contains(form);
    }

    /** ARGB для тонировки предметов. */
    public int tint() {
        return 0xFF000000 | color;
    }

    /** ARGB для тонировки руды. */
    public int oreTint() {
        return 0xFF000000 | oreColor;
    }
}
