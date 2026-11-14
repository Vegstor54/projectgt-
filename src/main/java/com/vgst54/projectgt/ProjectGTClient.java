package com.vgst54.projectgt;

import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.registry.ModBlocks;
import com.vgst54.projectgt.registry.ModItems;

import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// Клиентская часть мода. Не загружается на dedicated-сервере.
@Mod(value = ProjectGT.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ProjectGT.MODID, value = Dist.CLIENT)
public class ProjectGTClient {
    public ProjectGTClient(ModContainer container) {
        // Экран настроек: Mods > Project-GT+++ > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    // Блоки руды: накладка с жилами (tintindex 0) красится в цвет материала.
    @SubscribeEvent
    static void onOreBlockColors(RegisterColorHandlersEvent.Block event) {
        ModBlocks.ores().forEach(ore -> event.register(
                (state, level, pos, tintIndex) -> tintIndex == 0 ? ore.material().tint() : -1,
                ore.block().get()));
    }

    @SubscribeEvent
    static void onOreItemColors(RegisterColorHandlersEvent.Item event) {
        ModBlocks.ores().forEach(ore -> event.register(
                (stack, tintIndex) -> tintIndex == 0 ? ore.material().tint() : -1,
                ore.item().get()));
    }

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
