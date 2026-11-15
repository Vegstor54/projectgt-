package com.vgst54.projectgt.worldgen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Спрашивает у генератора мира про базовый рельеф (шумовые пещеры, море, лава, высота поверхности),
 * а не про уже поставленные блоки, поэтому ответ не зависит от того, какие чанки сгенерированы
 * и в каком порядке. Классические вырезаемые пещеры и каньоны (карверы) сюда не попадают.
 */
public interface TerrainProbe {
    boolean isSolid(int x, int y, int z);

    /** Y первого свободного блока над поверхностью в колонке (x, z). */
    int surfaceY(int x, int z);

    static TerrainProbe of(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        return new TerrainProbe() {
            @Override
            public boolean isSolid(int x, int y, int z) {
                NoiseColumn column = generator.getBaseColumn(x, z, level, randomState);
                BlockState state = column.getBlock(y);
                return !state.isAir() && state.getFluidState().isEmpty();
            }

            @Override
            public int surfaceY(int x, int z) {
                return generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
            }
        };
    }
}
