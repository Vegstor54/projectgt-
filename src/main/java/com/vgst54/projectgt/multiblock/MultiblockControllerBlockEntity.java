package com.vgst54.projectgt.multiblock;

import com.vgst54.projectgt.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Блок-сущность контроллера: периодически проверяет структуру и выставляет FORMED.
 * Пока только проверка формы; люки и рецепты добавляются следующими шагами.
 */
public class MultiblockControllerBlockEntity extends BlockEntity {
    /** Раз во сколько тиков перепроверять структуру (20 = раз в секунду). */
    private static final int CHECK_INTERVAL = 20;

    public MultiblockControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MULTIBLOCK_CONTROLLER.get(), pos, state);
    }

    private MultiblockControllerBlock controllerBlock() {
        return (MultiblockControllerBlock) getBlockState().getBlock();
    }

    /** Проверяет структуру сейчас и обновляет состояние блока. */
    public MultiblockPattern.Result recheck() {
        MultiblockPattern.Result result = controllerBlock().pattern()
                .check(level, worldPosition, getBlockState().getValue(MultiblockControllerBlock.FACING));
        BlockState state = getBlockState();
        if (state.getValue(MultiblockControllerBlock.FORMED) != result.formed()) {
            level.setBlock(worldPosition, state.setValue(MultiblockControllerBlock.FORMED, result.formed()), Block.UPDATE_ALL);
        }
        return result;
    }

    public void serverTick() {
        if (level != null && (level.getGameTime() + worldPosition.asLong()) % CHECK_INTERVAL == 0) {
            recheck();
        }
    }
}
