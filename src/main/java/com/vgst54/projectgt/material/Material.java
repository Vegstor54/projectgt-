package com.vgst54.projectgt.material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;

/**
 * Материал: идентификатор, отображаемое имя, цвет тонировки, цвет руды (по умолчанию тот же),
 * набор иконок, набор форм и, при необходимости, ванильный предмет сырой руды.
 */
public record Material(String id, String displayName, int color, int oreColor, IconSet iconSet,
                       Set<MaterialForm> forms, Supplier<Item> vanillaRaw, int meltingPoint) {

    /** Материал с набором иконок METALLIC. */
    public static Material of(String id, String displayName, int rgb, MaterialForm first, MaterialForm... rest) {
        return of(id, displayName, rgb, IconSet.METALLIC, first, rest);
    }

    public static Material of(String id, String displayName, int rgb, IconSet iconSet,
                              MaterialForm first, MaterialForm... rest) {
        EnumSet<MaterialForm> set = EnumSet.of(first, rest);
        return new Material(id, displayName, rgb, rgb, iconSet, Collections.unmodifiableSet(set), null, 0);
    }

    /** Отдельный цвет для руды (блоки руды и сырая руда), если он должен отличаться от цвета металла. */
    public Material withOreColor(int rgb) {
        return new Material(id, displayName, color, rgb, iconSet, forms, vanillaRaw, meltingPoint);
    }

    /**
     * Блоки руды этого материала дропают ванильную сырую руду (например, Items.RAW_IRON)
     * вместо собственной формы RAW_ORE.
     */
    public Material withVanillaRaw(Supplier<Item> raw) {
        return new Material(id, displayName, color, oreColor, iconSet, forms, raw, meltingPoint);
    }

    /**
     * Точка плавления в условных градусах (для баланса, не строго по реальности). 0 значит «не задана»:
     * такой материал нигде не ограничен по температуре. Источники тепла и их потолки: см. Heat.
     */
    public Material withMeltingPoint(int degrees) {
        return new Material(id, displayName, color, oreColor, iconSet, forms, vanillaRaw, degrees);
    }

    public boolean has(MaterialForm form) {
        return forms.contains(form);
    }

    /** Есть ли у материала руда: своя сырая руда или ванильная. */
    public boolean hasOre() {
        return forms.contains(MaterialForm.RAW_ORE) || vanillaRaw != null;
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
