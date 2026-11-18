package com.vgst54.projectgt.machine;

/**
 * Откуда машина берёт энергию. Машина не знает, чем питается: электрический буфер или бак пара.
 * Все рецепты считаются в EU, паровая реализация сама переводит EU в mB через {@link SteamPressure}.
 */
public interface EnergySource {
    /**
     * Забирает до eu энергии.
     *
     * @param simulate true значит только проверить, ничего не списывая
     * @return сколько EU реально отдано (от 0 до eu)
     */
    long extract(long eu, boolean simulate);
}
