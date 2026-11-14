package com.vgst54.projectgt;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.vgst54.projectgt.registry.ModBiomeModifiers;
import com.vgst54.projectgt.registry.ModCreativeTabs;
import com.vgst54.projectgt.registry.ModItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ProjectGT.MODID)
public class ProjectGT {
    public static final String MODID = "projectgt";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);

    public ProjectGT(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModBiomeModifiers.SERIALIZERS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
