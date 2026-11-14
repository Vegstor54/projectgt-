package com.vgst54.projectgt.worldgen;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Вызывается один раз на чанк. Берёт жилы соседних ячеек и рисует в текущем чанке только те
 * блоки, что попадают в его границы (в соседние чанки не пишет).
 */
public class OreNodeFeature extends Feature<NoneFeatureConfiguration> {
    public OreNodeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ChunkPos cp = new ChunkPos(ctx.origin());
        long seed = level.getSeed();

        int minX = cp.getMinBlockX(), maxX = cp.getMaxBlockX();
        int minZ = cp.getMinBlockZ(), maxZ = cp.getMaxBlockZ();
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight() - 1;

        int cellX = Math.floorDiv(minX, OreNodes.CELL);
        int cellZ = Math.floorDiv(minZ, OreNodes.CELL);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                List<Vein> veins = OreNodes.veinsOfCell(seed, cellX + dx, cellZ + dz);
                for (Vein v : veins) {
                    double reach = v.reach();
                    if (v.cx() + reach < minX || v.cx() - reach > maxX) continue;
                    if (v.cz() + reach < minZ || v.cz() - reach > maxZ) continue;
                    placed |= carve(level, v, pos, minX, maxX, minZ, maxZ, minY, maxY);
                }
            }
        }
        return placed;
    }

    private static boolean carve(WorldGenLevel level, Vein v, BlockPos.MutableBlockPos pos,
                                 int minX, int maxX, int minZ, int maxZ, int minY, int maxY) {
        double reach = v.reach();
        int x0 = Math.max(minX, (int) Math.floor(v.cx() - reach));
        int x1 = Math.min(maxX, (int) Math.ceil(v.cx() + reach));
        int z0 = Math.max(minZ, (int) Math.floor(v.cz() - reach));
        int z1 = Math.min(maxZ, (int) Math.ceil(v.cz() + reach));
        int y0 = Math.max(minY, (int) Math.floor(v.cy() - reach));
        int y1 = Math.min(maxY, (int) Math.ceil(v.cy() + reach));

        boolean any = false;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    double rx = x + 0.5 - v.cx();
                    double ry = y + 0.5 - v.cy();
                    double rz = z + 0.5 - v.cz();

                    double u = rx * v.ux() + rz * v.uz();
                    double w = rx * v.wx() + ry * v.wy() + rz * v.wz();
                    double n = rx * v.nx() + ry * v.ny() + rz * v.nz();

                    double d2 = (u * u) / (v.a() * v.a()) + (w * w) / (v.b() * v.b()) + (n * n) / (v.c() * v.c());
                    if (d2 > 1.0) continue;

                    // richness: 1 в центре линзы, 0 на краю
                    double richness = 1.0 - Math.sqrt(d2);

                    // На краях жила рваная, в ядре сплошная
                    if (OreNodes.hash01(v.seed(), x, y, z) > 0.30 + 0.70 * richness) continue;

                    pos.set(x, y, z);
                    Block ore = OreLookup.oreFor(v.material(), level.getBlockState(pos));
                    if (ore == null) continue; // не наша вмещающая порода (воздух, вода, руда и т.п.)

                    level.setBlock(pos, ore.defaultBlockState(), 2);
                    any = true;
                }
            }
        }
        return any;
    }
}
