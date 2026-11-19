package com.vgst54.projectgt.compat.jade;

import com.vgst54.projectgt.machine.SteamMachineBlock;
import com.vgst54.projectgt.machine.SteamMachineBlockEntity;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Jade: показывает у паровых машин рецепт в работе и прогресс. Jade нужен только при разработке, как опциональный. */
@WailaPlugin
public class ProjectGTJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MachineProvider.INSTANCE, SteamMachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MachineProvider.INSTANCE, SteamMachineBlock.class);
    }
}
