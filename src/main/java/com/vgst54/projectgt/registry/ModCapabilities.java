package com.vgst54.projectgt.registry;

import com.vgst54.projectgt.ProjectGT;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Выдаёт трубам IFluidHandler и IItemHandler, чтобы к ним могли подключаться баки, воронки, машины и другие трубы. */
@EventBusSubscriber(modid = ProjectGT.MODID)
public final class ModCapabilities {
    private ModCapabilities() {}

    @SubscribeEvent
    static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.FLUID_PIPE.get(),
                (pipe, side) -> pipe.handler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ITEM_PIPE.get(),
                (pipe, side) -> pipe.handler(side));
    }
}
