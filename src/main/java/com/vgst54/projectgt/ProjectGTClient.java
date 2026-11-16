package com.vgst54.projectgt;

import com.vgst54.projectgt.client.WrenchOverlay;
import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.MaterialForm;
import com.vgst54.projectgt.registry.ModBlocks;
import com.vgst54.projectgt.registry.ModItems;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
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

    // Блоки руды: накладка с жилами (tintindex 0) красится в цвет руды материала.
    @SubscribeEvent
    static void onOreBlockColors(RegisterColorHandlersEvent.Block event) {
        ModBlocks.ores().forEach(ore -> event.register(
                (state, level, pos, tintIndex) -> tintIndex == 0 ? ore.material().oreTint() : -1,
                ore.block().get()));
    }

    @SubscribeEvent
    static void onOreItemColors(RegisterColorHandlersEvent.Item event) {
        ModBlocks.ores().forEach(ore -> event.register(
                (stack, tintIndex) -> tintIndex == 0 ? ore.material().oreTint() : -1,
                ore.item().get()));
    }

    // Сетка 3x3 ключа на грани трубы (только с ключом в руке).
    @SubscribeEvent
    static void onBlockHighlight(RenderHighlightEvent.Block event) {
        WrenchOverlay.render(event);
    }

    // Трубы: единая серая текстура, цвет материала задаём тонировкой (tintindex 0).
    @SubscribeEvent
    static void onPipeBlockColors(RegisterColorHandlersEvent.Block event) {
        ModBlocks.fluidPipes().forEach(pipe -> event.register(
                (state, level, pos, tintIndex) -> tintIndex == 0 ? pipe.type().material().tint() : -1,
                pipe.block().get()));
    }

    @SubscribeEvent
    static void onPipeItemColors(RegisterColorHandlersEvent.Item event) {
        ModBlocks.fluidPipes().forEach(pipe -> event.register(
                (stack, tintIndex) -> tintIndex == 0 ? pipe.type().material().tint() : -1,
                pipe.item().get()));
    }

    // Все предметы материалов используют одну серую текстуру на форму; цвет задаём тонировкой слоя 0.
    // Сырая руда красится в цвет руды, остальные формы в цвет металла.
    @SubscribeEvent
    static void onItemColors(RegisterColorHandlersEvent.Item event) {
        ModItems.byMaterial().forEach(entry -> {
            Material material = entry.getKey();
            entry.getValue().forEach((form, holder) -> {
                int tint = form == MaterialForm.RAW_ORE ? material.oreTint() : material.tint();
                event.register((stack, tintIndex) -> tintIndex == 0 ? tint : -1, holder.get());
            });
        });
    }
}
