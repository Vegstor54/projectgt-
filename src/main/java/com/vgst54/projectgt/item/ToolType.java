package com.vgst54.projectgt.item;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.ToolStats;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

/** Тип инструмента. Для каждой пары (материал с ToolStats, тип) регистрируется свой предмет. */
public enum ToolType {
    HARD_HAMMER("hard_hammer", "Hard Hammer", Group.METAL),
    SCREWDRIVER("screwdriver", "Screwdriver", Group.METAL),
    SAW("saw", "Saw", Group.METAL),
    CROWBAR("crowbar", "Crowbar", Group.METAL),
    WIRE_CUTTERS("wire_cutters", "Wire Cutters", Group.METAL),
    WRENCH("wrench", "Wrench", Group.METAL),
    FILE("file", "File", Group.METAL),
    MORTAR("mortar", "Mortar", Group.METAL),
    KNIFE("knife", "Knife", Group.METAL),
    SCYTHE("scythe", "Scythe", Group.METAL),
    SOFT_MALLET("soft_mallet", "Soft Mallet", Group.SOFT),
    PICKAXE("pickaxe", "Pickaxe", Group.ANY),
    AXE("axe", "Axe", Group.ANY),
    SHOVEL("shovel", "Shovel", Group.ANY),
    HOE("hoe", "Hoe", Group.ANY),
    SWORD("sword", "Sword", Group.ANY);

    /** Из каких материалов делается: METAL только твёрдые металлы, SOFT только дерево и подобное, ANY любые. */
    private enum Group { METAL, SOFT, ANY }

    private final String id;
    private final String displayName;
    private final Group group;

    ToolType(String id, String displayName, Group group) {
        this.id = id;
        this.displayName = displayName;
        this.group = group;
    }

    public String id() {
        return id;
    }

    public boolean canMake(ToolStats stats) {
        return switch (group) {
            case METAL -> stats.metal();
            case SOFT -> !stats.metal();
            case ANY -> true;
        };
    }

    public String itemId(Material material) {
        return material.id() + "_" + id;
    }

    public String displayName(Material material) {
        return material.displayName() + " " + displayName;
    }

    /** Общий тег предметов этого типа: projectgt:tools/<тип>. Нужен рецептам и блокам. */
    public TagKey<Item> tag() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "tools/" + id));
    }

    /** Текстура рукояти (без тонировки) и оголовья (серая, тонируется материалом). */
    public ResourceLocation handleTexture() {
        return ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "item/tool/" + id + "_handle");
    }

    public ResourceLocation headTexture() {
        return ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "item/tool/" + id + "_head");
    }

    public Item create(Material material, Item.Properties properties) {
        ToolStats stats = material.tool();
        MaterialTier tier = new MaterialTier(stats);
        return switch (this) {
            case PICKAXE -> new PickaxeItem(tier, properties.attributes(PickaxeItem.createAttributes(tier, 1.0F, -2.8F)));
            case AXE -> new AxeItem(tier, properties.attributes(AxeItem.createAttributes(tier, 6.0F, -3.2F)));
            case SHOVEL -> new ShovelItem(tier, properties.attributes(ShovelItem.createAttributes(tier, 1.5F, -3.0F)));
            case HOE -> new HoeItem(tier, properties.attributes(HoeItem.createAttributes(tier, -1.0F, -2.0F)));
            case SWORD -> new SwordItem(tier, properties.attributes(SwordItem.createAttributes(tier, 3, -2.4F)));
            case WRENCH -> new WrenchItem(properties.durability(stats.durability()));
            default -> new CraftingToolItem(properties.durability(stats.durability()));
        };
    }
}
