package com.vgst54.projectgt.registry;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.material.MaterialForm;
import com.vgst54.projectgt.material.Materials;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;

@EventBusSubscriber(modid = ProjectGT.MODID)
public final class CoalDustFuelHandler {
    private CoalDustFuelHandler() {}

    @SubscribeEvent
    static void onFurnaceFuel(FurnaceFuelBurnTimeEvent event) {
        if (event.getItemStack().is(ModItems.get(Materials.COAL, MaterialForm.DUST).get())) {
            event.setBurnTime(1600);
        }
    }
}