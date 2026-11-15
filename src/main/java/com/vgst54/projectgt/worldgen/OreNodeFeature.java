package com.vgst54.projectgt.worldgen;

import java.util.List;

import com.vgst54.projectgt.material.Material;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
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
        var dimension = level.getLevel().dimension();
        boolean nether = dimension == Level.NETHER;
        TerrainProbe probe = TerrainProbe.of(level.getLevel());
        if (!nether && dimension != Level.OVERWORLD) return false;

        int minX = cp.getMinBlockX(), maxX = cp.getMaxBlockX();
        int minZ = cp.getMinBlockZ(), maxZ = cp.getMaxBlockZ();
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight() - 1;

        int cellX = Math.floorDiv(minX, OreNodes.CELL);
        int cellZ = Math.floorDiv(minZ, OreNodes.CELL);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                List<Vein> veins = OreNodes.veinsOfCell(seed, cellX + dx, cellZ + dz, nether, probe);
                for (Vein v : veins) {
                    double reach = v.reach();
                    if (v.cx() + reach < minX || v.cx() - reach > maxX) continue;
                    if (v.cz() + reach < minZ || v.cz() - reach > maxZ) continue;
                    placed |= carve(level, v, pos, minX, maxX, minZ, maxZ, minY, maxY);
                    placed |= markSurface(level, v, probe, pos, minX, maxX, minZ, maxZ);
                }
            }
        }
        return placed;
    }

    /**
     * Пятно на поверхности над жилой: гравий, в центре гуще руда из состава жилы. Край рваный.
     * Колонки берутся из текущего чанка, поэтому соседние чанки рисуют свои части пятна независимо.
     */
    private static boolean markSurface(WorldGenLevel level, Vein v, TerrainProbe probe, BlockPos.MutableBlockPos pos,
                                       int minX, int maxX, int minZ, int maxZ) {
        if (!v.surfaceMark()) return false;
        if (probe.surfaceY(Mth.floor(v.cx()), Mth.floor(v.cz())) - v.cy() > OreNodes.SURFACE_MAX_DEPTH) return false;

        double radius = OreNodes.SURFACE_RADIUS_BASE + OreNodes.SURFACE_RADIUS_PER_LENGTH * v.a();
        int x0 = Math.max(minX, (int) Math.floor(v.cx() - radius));
        int x1 = Math.min(maxX, (int) Math.ceil(v.cx() + radius));
        int z0 = Math.max(minZ, (int) Math.floor(v.cz() - radius));
        int z1 = Math.min(maxZ, (int) Math.ceil(v.cz() + radius));

        boolean any = false;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                double d = Math.hypot(x + 0.5 - v.cx(), z + 0.5 - v.cz()) / radius;   // 0 в центре, 1 на краю
                if (d > 1.0) continue;
                // Рваный край: в центре пятно сплошное, к краю остаются отдельные блоки
                if (OreNodes.hash01(v.seed() ^ 0x7f4a7c15L, x, 0, z) > 1.0 - 0.8 * d) continue;

                pos.set(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, z);
                BlockState surface = level.getBlockState(pos);
                if (surface.isAir() || !surface.getFluidState().isEmpty() || surface.is(Blocks.BEDROCK)) continue;

                // Руды больше в центре; минерал по кластерам 2x2, как под землёй
                BlockState place = Blocks.GRAVEL.defaultBlockState();
                if (OreNodes.hash01(v.seed() ^ 0x2545F491L, x, 1, z) < 0.45 - 0.25 * d) {
                    Material mineral = v.mix().pick(OreNodes.hash01(v.seed() ^ 0x5bd1e995L, x >> 1, 7, z >> 1));
                    Block ore = OreLookup.surfaceOre(mineral);
                    if (ore != null) place = ore.defaultBlockState();
                }
                level.setBlock(pos, place, 2);
                any = true;
            }
        }
        return any;
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

                    // Вкрапления вмещающей породы: плотность растёт от края к ядру, но сплошной не бывает
                    double density = OreNodes.DENSITY_EDGE + (OreNodes.DENSITY_CORE - OreNodes.DENSITY_EDGE) * richness;
                    if (OreNodes.hash01(v.seed(), x, y, z) > density) continue;

                    pos.set(x, y, z);
                    // Минерал из состава жилы; выбор по кластерам 2x2x2, чтобы получались пятна, а не соль с перцем
                    Material mineral = v.mix().pick(OreNodes.hash01(v.seed() ^ 0x5bd1e995L, x >> 1, y >> 1, z >> 1));
                    Block ore = OreLookup.oreFor(mineral, level.getBlockState(pos));
                    if (ore == null) continue; // не наша вмещающая порода (воздух, вода, руда и т.п.)

                    level.setBlock(pos, ore.defaultBlockState(), 2);
                    any = true;
                }
            }
        }
        return any;
    }
}
