package com.vgst54.projectgt.item;

import net.minecraft.world.item.Item;

/**
 * Ключ. Сама логика живёт в блоках, которые умеют с ним работать (FluidPipeBlock):
 * ПКМ включает и выключает сторону, ПКМ с sneak задаёт направление потока.
 */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }
}
