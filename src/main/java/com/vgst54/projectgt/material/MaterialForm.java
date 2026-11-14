package com.vgst54.projectgt.material;

import com.vgst54.projectgt.ProjectGT;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Форма материала. Определяет суффикс id, папку common-тега и шаблон названия. */
public enum MaterialForm {
    DUST("dust", "dusts", "%s Dust"),
    INGOT("ingot", "ingots", "%s Ingot"),
    PLATE("plate", "plates", "%s Plate"),
    ROD("rod", "rods", "%s Rod");

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

    /** Путь текстуры формы: projectgt:item/material/<suffix> */
    public ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "item/material/" + suffix);
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
