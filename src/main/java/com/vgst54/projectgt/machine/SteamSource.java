package com.vgst54.projectgt.machine;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Энергия из бака пара: {@link SteamPressure#STEAM_PER_EU} mB за 1 EU.
 * Отдаёт только целые EU, остаток пара меньше стоимости одного EU остаётся в баке.
 */
public final class SteamSource implements EnergySource {
    private final FluidTank steam;

    public SteamSource(FluidTank steam) {
        this.steam = steam;
    }

    @Override
    public long extract(long eu, boolean simulate) {
        long available = steam.getFluidAmount() / SteamPressure.STEAM_PER_EU;
        long taken = Math.min(eu, available);
        if (taken > 0 && !simulate) {
            steam.drain((int) (taken * SteamPressure.STEAM_PER_EU), IFluidHandler.FluidAction.EXECUTE);
        }
        return taken;
    }
}
