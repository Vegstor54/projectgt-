package com.vgst54.projectgt.registry;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.item.ToolType;
import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.MaterialForm;
import com.vgst54.projectgt.material.Materials;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ProjectGT.MODID);

    private static final Map<Material, Map<MaterialForm, DeferredItem<Item>>> MATERIAL_ITEMS = new HashMap<>();
    private static final Map<Material, Map<ToolType, DeferredItem<Item>>> TOOL_ITEMS = new HashMap<>();

    static {
        for (Material material : Materials.all()) {
            Map<MaterialForm, DeferredItem<Item>> byForm = new EnumMap<>(MaterialForm.class);
            for (MaterialForm form : material.forms()) {
                byForm.put(form, ITEMS.registerSimpleItem(form.itemId(material)));
            }
            MATERIAL_ITEMS.put(material, byForm);

            if (material.hasTools()) {
                Map<ToolType, DeferredItem<Item>> byType = new EnumMap<>(ToolType.class);
                for (ToolType type : ToolType.values()) {
                    if (type.canMake(material.tool())) {
                        byType.put(type, ITEMS.registerItem(type.itemId(material), props -> type.create(material, props), new Item.Properties()));
                    }
                }
                TOOL_ITEMS.put(material, byType);
            }
        }
    }

    private ModItems() {}

    public static DeferredItem<Item> get(Material material, MaterialForm form) {
        Map<MaterialForm, DeferredItem<Item>> byForm = MATERIAL_ITEMS.get(material);
        DeferredItem<Item> item = byForm == null ? null : byForm.get(form);
        if (item == null) {
            throw new IllegalArgumentException(material.id() + " has no form " + form);
        }
        return item;
    }

    /** Инструмент данного типа из материала. */
    public static DeferredItem<Item> tool(Material material, ToolType type) {
        Map<ToolType, DeferredItem<Item>> byType = TOOL_ITEMS.get(material);
        DeferredItem<Item> item = byType == null ? null : byType.get(type);
        if (item == null) {
            throw new IllegalArgumentException(material.id() + " has no tool " + type);
        }
        return item;
    }

    /** Все инструменты: материалы в порядке объявления, внутри типы в порядке enum. */
    public static Stream<Map.Entry<Material, Map<ToolType, DeferredItem<Item>>>> byMaterialTools() {
        return Materials.all().stream().filter(Material::hasTools).map(m -> Map.entry(m, TOOL_ITEMS.get(m)));
    }

    /** Предмет, который дропает руда материала: своя сырая руда или ванильная. */
    public static Item rawOre(Material material) {
        if (material.has(MaterialForm.RAW_ORE)) {
            return get(material, MaterialForm.RAW_ORE).get();
        }
        if (material.vanillaRaw() != null) {
            return material.vanillaRaw().get();
        }
        throw new IllegalArgumentException(material.id() + " has no raw ore");
    }

    /** Все предметы материалов в порядке объявления материалов и форм. */
    public static Stream<DeferredItem<Item>> materialItems() {
        return Materials.all().stream().flatMap(m -> MATERIAL_ITEMS.get(m).values().stream());
    }

    public static Stream<Map.Entry<Material, Map<MaterialForm, DeferredItem<Item>>>> byMaterial() {
        return Materials.all().stream().map(m -> Map.entry(m, MATERIAL_ITEMS.get(m)));
    }
}
