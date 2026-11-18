package com.vgst54.projectgt;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.vgst54.projectgt.registry.ModBiomeModifiers;
import com.vgst54.projectgt.registry.ModBlockEntities;
import com.vgst54.projectgt.registry.ModBlocks;
import com.vgst54.projectgt.registry.ModCreativeTabs;
import com.vgst54.projectgt.registry.ModItems;
import com.vgst54.projectgt.registry.ModMachineRecipes;
import com.vgst54.projectgt.worldgen.ModFeatures;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(ProjectGT.MODID)
public class ProjectGT {
    public static final String MODID = "projectgt";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ProjectGT(IEventBus modEventBus, ModContainer modContainer) {
        // ModBlocks регистрирует и предметы-блоки в ModItems.ITEMS, поэтому блоки инициализируем первыми.
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.TYPES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModBiomeModifiers.SERIALIZERS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModMachineRecipes.TYPES.register(modEventBus);
        ModMachineRecipes.SERIALIZERS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
