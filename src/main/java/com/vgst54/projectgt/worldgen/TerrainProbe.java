package com.vgst54.projectgt.worldgen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Спрашивает у генератора мира, твёрдая ли точка. Смотрит на базовый рельеф (шумовые пещеры, море, лава),
 * а не на уже поставленные блоки, поэтому ответ не зависит от того, какие чанки сгенерированы
 * и в каком порядке. Классические вырезаемые пещеры и каньоны (карверы) сюда не попадают.
 */
@FunctionalInterface
public interface TerrainProbe {
    boolean isSolid(int x, int y, int z);

    static TerrainProbe of(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        return (x, y, z) -> {
            NoiseColumn column = generator.getBaseColumn(x, z, level, randomState);
            BlockState state = column.getBlock(y);
            return !state.isAir() && state.getFluidState().isEmpty();
        };
    }
}
