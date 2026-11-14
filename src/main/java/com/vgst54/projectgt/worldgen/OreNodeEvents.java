package com.vgst54.projectgt.worldgen;

import com.vgst54.projectgt.ProjectGT;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Сбрасывает кэш узлов при остановке сервера, чтобы другой мир с тем же сидом не получил чужие жилы. */
@EventBusSubscriber(modid = ProjectGT.MODID)
public final class OreNodeEvents {
    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        OreNodes.clearCache();
    }

    private OreNodeEvents() {}
}
