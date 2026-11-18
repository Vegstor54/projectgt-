package com.vgst54.projectgt.client;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.registry.ModMenus;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Привязывает меню к экранам. */
@EventBusSubscriber(modid = ProjectGT.MODID, value = Dist.CLIENT)
public final class ClientMenus {
    private ClientMenus() {}

    @SubscribeEvent
    static void register(RegisterMenuScreensEvent event) {
        event.register(ModMenus.STEAM_MACHINE.get(), SteamMachineScreen::new);
        event.register(ModMenus.BOILER.get(), BoilerScreen::new);
    }
}
