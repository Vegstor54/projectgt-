package com.vgst54.projectgt;

import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.registry.ModItems;

import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

// Клиентская часть мода. Не загружается на dedicated-сервере.
@Mod(value = ProjectGT.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ProjectGT.MODID, value = Dist.CLIENT)
public class ProjectGTClient {

    // Все предметы материалов используют одну серую текстуру на форму; цвет задаём тонировкой слоя 0.
    @SubscribeEvent
    static void onItemColors(RegisterColorHandlersEvent.Item event) {
        ModItems.byMaterial().forEach(entry -> {
            Material material = entry.getKey();
            Item[] items = entry.getValue().values().stream().map(h -> h.get()).toArray(Item[]::new);
            event.register((stack, tintIndex) -> tintIndex == 0 ? material.tint() : -1, items);
        });
    }
}
