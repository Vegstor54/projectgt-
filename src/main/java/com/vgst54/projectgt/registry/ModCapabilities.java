package com.vgst54.projectgt.registry;

import com.vgst54.projectgt.ProjectGT;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Выдаёт трубам и котлам IFluidHandler и IItemHandler, чтобы к ним могли подключаться баки, воронки, машины и другие трубы. */
@EventBusSubscriber(modid = ProjectGT.MODID)
public final class ModCapabilities {
    private ModCapabilities() {}

    @SubscribeEvent
    static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.FLUID_PIPE.get(),
                (pipe, side) -> pipe.handler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ITEM_PIPE.get(),
                (pipe, side) -> pipe.handler(side));
        // Котёл: вода/топливо внутрь, пар наружу; слот топлива (null у котлов без твёрдого топлива).
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.STEAM_BOILER.get(),
                (boiler, side) -> boiler.fluidHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.STEAM_BOILER.get(),
                (boiler, side) -> boiler.itemHandler());
        // Паровая машина: пар внутрь, предметы во вход и из выхода.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.STEAM_MACHINE.get(),
                (machine, side) -> machine.fluidHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.STEAM_MACHINE.get(),
                (machine, side) -> machine.itemHandler());
        // Люки мультиблока: шины отдают предметы (ввод только принимает, вывод только отдаёт), паровой люк принимает пар.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MULTIBLOCK_PART.get(),
                (part, side) -> part.outsideItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MULTIBLOCK_PART.get(),
                (part, side) -> part.outsideFluid());
    }
}
