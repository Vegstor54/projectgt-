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
    HARD_HAMMER("hard_hammer", "Hard Hammer", "Forges ingots into plates and flattens metal parts.", Group.METAL),
    SCREWDRIVER("screwdriver", "Screwdriver", "Tightens screws when assembling machine parts.", Group.METAL),
    SAW("saw", "Saw", "Cuts wood and metal into smaller parts.", Group.METAL),
    CROWBAR("crowbar", "Crowbar", "Pries parts and blocks loose.", Group.METAL),
    WIRE_CUTTERS("wire_cutters", "Wire Cutters", "Cuts wire from metal plates.", Group.METAL),
    WRENCH("wrench", "Wrench", "Configures pipes and assembles casings. Right-click a pipe face to open or close a side; Shift+right-click sets an arrow or restrictive mode.", Group.METAL),
    FILE("file", "File", "Smooths metal parts and shapes pipes.", Group.METAL),
    MORTAR("mortar", "Mortar", "Grinds ores and ingots into dust.", Group.METAL),
    KNIFE("knife", "Knife", "Cuts and carves.", Group.METAL),
    SCYTHE("scythe", "Scythe", "Cuts grass and crops.", Group.METAL),
    SOFT_MALLET("soft_mallet", "Soft Mallet", "Taps machines gently where a hard hammer would damage parts.", Group.SOFT),
    PICKAXE("pickaxe", "Pickaxe", "Mines stone and ores.", Group.ANY),
    AXE("axe", "Axe", "Chops wood.", Group.ANY),
    SHOVEL("shovel", "Shovel", "Digs dirt, sand and gravel.", Group.ANY),
    HOE("hoe", "Hoe", "Tills soil.", Group.ANY),
    SWORD("sword", "Sword", "Fights mobs.", Group.ANY);

    /** Из каких материалов делается: METAL только твёрдые металлы, SOFT только дерево и подобное, ANY любые. */
    private enum Group { METAL, SOFT, ANY }

    private final String id;
    private final String displayName;
    private final String description;
    private final Group group;

    ToolType(String id, String displayName, String description, Group group) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.group = group;
    }

    /** Короткое описание назначения (английский текст для ланга). */
    public String description() {
        return description;
    }

    /** Ключ перевода описания. */
    public String descriptionKey() {
        return "tooltip." + ProjectGT.MODID + ".tool." + id;
    }

    /** Кирка, топор, лопата, мотыга: показываем скорость и уровень добычи. */
    public boolean isDigger() {
        return this == PICKAXE || this == AXE || this == SHOVEL || this == HOE;
    }

    /** Ручной инструмент, который тратит прочность при крафте. */
    public boolean usesDurabilityInCrafting() {
        return !isDigger() && this != SWORD && this != WRENCH;
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

    /** Целиком металлический инструмент (монтировка, ключ): одна текстура, весь предмет красится материалом. */
    public boolean isFullMetal() {
        return this == CROWBAR || this == WRENCH;
    }

    /** Единственная текстура целиком металлического инструмента (тонируется слоем 0). */
    public ResourceLocation fullTexture() {
        return ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "item/tool/" + id);
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
