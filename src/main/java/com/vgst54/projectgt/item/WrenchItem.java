package com.vgst54.projectgt.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;

/**
 * Ключ. Сама логика живёт в блоках, которые умеют с ним работать (FluidPipeBlock):
 * ПКМ включает и выключает сторону, ПКМ с sneak задаёт направление потока.
 */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    /**
     * Без этого ванильная логика при sneak с предметом в руке пропускает useItemOn блока,
     * и shift+ПКМ ключом вообще не доходил бы до трубы.
     */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }
}
