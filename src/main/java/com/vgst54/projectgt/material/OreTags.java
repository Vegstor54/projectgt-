package com.vgst54.projectgt.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Общие теги руд: c:ores и c:ores/<материал> (для блоков и предметов). */
public final class OreTags {
    public static final TagKey<Block> ALL_BLOCKS = TagKey.create(Registries.BLOCK, c("ores"));
    public static final TagKey<Item> ALL_ITEMS = TagKey.create(Registries.ITEM, c("ores"));

    private OreTags() {}

    public static TagKey<Block> block(Material material) {
        return TagKey.create(Registries.BLOCK, c("ores/" + material.id()));
    }

    public static TagKey<Item> item(Material material) {
        return TagKey.create(Registries.ITEM, c("ores/" + material.id()));
    }

    private static ResourceLocation c(String path) {
        return ResourceLocation.fromNamespaceAndPath("c", path);
    }
}
