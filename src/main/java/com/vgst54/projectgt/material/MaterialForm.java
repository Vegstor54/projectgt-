package com.vgst54.projectgt.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Форма материала. Определяет суффикс id, папку common-тега и шаблон названия. */
public enum MaterialForm {
    RAW_ORE("raw_ore", "raw_materials", "Raw %s Ore"),
    DUST("dust", "dusts", "%s Dust"),
    INGOT("ingot", "ingots", "%s Ingot"),
    PLATE("plate", "plates", "%s Plate"),
    DOUBLE_PLATE("double_plate", "double_plates", "Double %s Plate"),
    BOLT("bolt", "bolts", "%s Bolt"),
    SCREW("screw", "screws", "%s Screw"),
    ROD("rod", "rods", "%s Rod"),
    DOUBLE_INGOT("double_ingot", "double_ingots", "Double %s Ingot"),
    GEAR("gear", "gears", "%s Gear"),
    SMALL_GEAR("small_gear", "small_gears", "Small %s Gear"),
    SPRING("spring", "springs", "%s Spring"),
    SMALL_SPRING("small_spring", "small_springs", "Small %s Spring");

    private final String suffix;
    private final String tagFolder;
    private final String nameFormat;

    MaterialForm(String suffix, String tagFolder, String nameFormat) {
        this.suffix = suffix;
        this.tagFolder = tagFolder;
        this.nameFormat = nameFormat;
    }

    public String suffix() {
        return suffix;
    }

    /** Текстура формы с учётом набора иконок материала. */
    public ResourceLocation texture(Material material) {
        return material.iconSet().texture(this);
    }

    public String itemId(Material material) {
        return material.id() + "_" + suffix;
    }

    public String displayName(Material material) {
        return nameFormat.formatted(material.displayName());
    }

    /** Общий тег, например c:dusts/iron и c:dusts. */
    public TagKey<Item> tag(Material material) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", tagFolder + "/" + material.id()));
    }

    public TagKey<Item> formTag() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", tagFolder));
    }
}
