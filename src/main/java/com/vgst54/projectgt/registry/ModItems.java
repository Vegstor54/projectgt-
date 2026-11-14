package com.vgst54.projectgt.registry;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.MaterialForm;
import com.vgst54.projectgt.material.Materials;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ProjectGT.MODID);

    private static final Map<Material, Map<MaterialForm, DeferredItem<Item>>> MATERIAL_ITEMS = new HashMap<>();

    static {
        for (Material material : Materials.all()) {
            Map<MaterialForm, DeferredItem<Item>> byForm = new EnumMap<>(MaterialForm.class);
            for (MaterialForm form : material.forms()) {
                byForm.put(form, ITEMS.registerSimpleItem(form.itemId(material)));
            }
            MATERIAL_ITEMS.put(material, byForm);
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

    /** Все предметы материалов в порядке объявления материалов и форм. */
    public static Stream<DeferredItem<Item>> materialItems() {
        return Materials.all().stream().flatMap(m -> MATERIAL_ITEMS.get(m).values().stream());
    }

    public static Stream<Map.Entry<Material, Map<MaterialForm, DeferredItem<Item>>>> byMaterial() {
        return Materials.all().stream().map(m -> Map.entry(m, MATERIAL_ITEMS.get(m)));
    }
}
