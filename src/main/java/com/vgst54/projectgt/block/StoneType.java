package com.vgst54.projectgt.block;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Тип ванильного камня, в который «вставляется» руда. */
public enum StoneType {
    STONE("stone", "", () -> Blocks.STONE, 3.0f, "block/stone"),
    DEEPSLATE("deepslate", "Deepslate ", () -> Blocks.DEEPSLATE, 4.5f, "block/deepslate"),
    GRANITE("granite", "Granite ", () -> Blocks.GRANITE, 3.0f, "block/granite"),
    DIORITE("diorite", "Diorite ", () -> Blocks.DIORITE, 3.0f, "block/diorite"),
    ANDESITE("andesite", "Andesite ", () -> Blocks.ANDESITE, 3.0f, "block/andesite"),
    TUFF("tuff", "Tuff ", () -> Blocks.TUFF, 3.0f, "block/tuff"),
    NETHERRACK("netherrack", "Nether ", () -> Blocks.NETHERRACK, 3.0f, "block/netherrack"),
    BLACKSTONE("blackstone", "Blackstone ", () -> Blocks.BLACKSTONE, 3.0f, "block/blackstone"),
    END_STONE("end_stone", "End ", () -> Blocks.END_STONE, 3.0f, "block/end_stone");

    private final String id;
    private final String namePrefix;
    private final Supplier<Block> base;
    private final float hardness;
    private final ResourceLocation texture;

    StoneType(String id, String namePrefix, Supplier<Block> base, float hardness, String texturePath) {
        this.id = id;
        this.namePrefix = namePrefix;
        this.base = base;
        this.hardness = hardness;
        this.texture = ResourceLocation.withDefaultNamespace(texturePath);
    }

    public String id() {
        return id;
    }

    /** Префикс названия: "" для обычного камня, "Deepslate " и т.д. */
    public String namePrefix() {
        return namePrefix;
    }

    /** Ванильный блок камня: от него берутся звуки, цвет на карте и т.п. */
    public Block base() {
        return base.get();
    }

    public float hardness() {
        return hardness;
    }

    /** Текстура камня для основы блока руды. */
    public ResourceLocation texture() {
        return texture;
    }
}
