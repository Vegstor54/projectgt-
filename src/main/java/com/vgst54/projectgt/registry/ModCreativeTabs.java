package com.vgst54.projectgt.registry;

import com.vgst54.projectgt.ProjectGT;
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

    private ModCreativeTabs() {}
}
