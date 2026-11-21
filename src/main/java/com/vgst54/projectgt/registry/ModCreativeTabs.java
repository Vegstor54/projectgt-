package com.vgst54.projectgt.registry;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.item.ToolType;
import com.vgst54.projectgt.material.MaterialForm;
import com.vgst54.projectgt.material.Materials;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ProjectGT.MODID);

    public static final String MATERIALS_TITLE_KEY = "itemGroup." + ProjectGT.MODID + ".materials";
    public static final String ORES_TITLE_KEY = "itemGroup." + ProjectGT.MODID + ".ores";
    public static final String PIPES_TITLE_KEY = "itemGroup." + ProjectGT.MODID + ".pipes";
    public static final String TOOLS_TITLE_KEY = "itemGroup." + ProjectGT.MODID + ".tools";
    public static final String MACHINES_TITLE_KEY = "itemGroup." + ProjectGT.MODID + ".machines";

    /** Материалы: всё, кроме сырой руды. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MATERIALS = TABS.register("materials",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable(MATERIALS_TITLE_KEY))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.get(Materials.STEEL, MaterialForm.INGOT).get().getDefaultInstance())
                    .displayItems((params, output) -> ModItems.byMaterial().forEach(entry ->
                            entry.getValue().forEach((form, item) -> {
                                if (form != MaterialForm.RAW_ORE) {
                                    output.accept(item.get());
                                }
                            })))
                    .build());

    /** Руды: сырая руда (и в будущем блоки руды под каждый камень). */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ORES = TABS.register("ores",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable(ORES_TITLE_KEY))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.get(Materials.DIAMOND, MaterialForm.RAW_ORE).get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        ModItems.byMaterial().forEach(entry -> {
                            var item = entry.getValue().get(MaterialForm.RAW_ORE);
                            if (item != null) {
                                output.accept(item.get());
                            }
                        });
                        ModBlocks.ores().forEach(ore -> output.accept(ore.item().get()));
                    })
                    .build());

    /** Трубы. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PIPES = TABS.register("pipes",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable(PIPES_TITLE_KEY))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModBlocks.fluidPipes().findFirst().orElseThrow().item().get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        ModBlocks.fluidPipes().forEach(pipe -> output.accept(pipe.item().get()));
                        ModBlocks.itemPipes().forEach(pipe -> output.accept(pipe.item().get()));
                    })
                    .build());

    /** Инструменты из материалов, включая ключи. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS = TABS.register("tools",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable(TOOLS_TITLE_KEY))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.tool(Materials.STEEL, ToolType.WRENCH).get().getDefaultInstance())
                    .displayItems((params, output) -> ModItems.byMaterialTools().forEach(entry ->
                            entry.getValue().values().forEach(item -> output.accept(item.get()))))
                    .build());

    /** Машины и котлы. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MACHINES = TABS.register("machines",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable(MACHINES_TITLE_KEY))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModBlocks.boilers().findFirst().orElseThrow().item().get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        ModBlocks.boilers().forEach(boiler -> output.accept(boiler.item().get()));
                        ModBlocks.steamMachines().forEach(machine -> output.accept(machine.item().get()));
                        output.accept(ModBlocks.BRONZE_BRICKS_CASING_ITEM.get());
                        output.accept(ModBlocks.STEAM_GRINDER_ITEM.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
