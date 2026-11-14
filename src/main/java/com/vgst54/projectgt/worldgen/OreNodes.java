package com.vgst54.projectgt.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.Materials;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

/**
 * Рудные узлы. Мир делится на ячейки CELL x CELL. В ячейке с шансом NODE_CHANCE есть узел
 * (условный гранитный массив). Вокруг центра узла по расстоянию раскладываются жилы:
 *   ядро  -> олово, середина -> медь, край -> железо (пока нет свинца и цинка).
 * Всё считается детерминированно от сида мира, поэтому любой чанк может независимо
 * восстановить жилы, которые его задевают.
 */
public final class OreNodes {
    // ---- Крутилки баланса ----
    public static final int CELL = 512;
    public static final float NODE_CHANCE = 0.65f;
    public static final double RADIUS_MIN = 110, RADIUS_VAR = 90;
    public static final int VEINS_MIN = 35, VEINS_VAR = 26;
    /** Границы колец зональности (доля радиуса узла). */
    public static final double RING_TIN = 0.28, RING_COPPER = 0.58;

    private record CellKey(long seed, int cellX, int cellZ) {}

    private static final Map<CellKey, List<Vein>> CACHE = new ConcurrentHashMap<>();

    public static List<Vein> veinsOfCell(long seed, int cellX, int cellZ) {
        if (CACHE.size() > 256) CACHE.clear();
        return CACHE.computeIfAbsent(new CellKey(seed, cellX, cellZ), OreNodes::generate);
    }

    private static List<Vein> generate(CellKey key) {
        RandomSource r = new XoroshiroRandomSource(mix(mix(key.seed() ^ key.cellX()) ^ key.cellZ()));
        if (r.nextFloat() > NODE_CHANCE) return List.of();

        double nodeX = key.cellX() * (double) CELL + 96 + r.nextDouble() * (CELL - 192);
        double nodeZ = key.cellZ() * (double) CELL + 96 + r.nextDouble() * (CELL - 192);
        double radius = RADIUS_MIN + r.nextDouble() * RADIUS_VAR;
        int count = VEINS_MIN + r.nextInt(VEINS_VAR);

        List<Vein> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double t = Math.pow(r.nextDouble(), 0.8);          // 0 = центр узла, 1 = край
            double ang = r.nextDouble() * Math.PI * 2;
            double vx = nodeX + Math.cos(ang) * t * radius;
            double vz = nodeZ + Math.sin(ang) * t * radius;

            double ring = t + (r.nextDouble() - 0.5) * 0.1;    // размытие границ колец
            Material material;
            int yMin, yMax;
            if (ring < RING_TIN) {
                material = Materials.TIN;
                yMin = -48; yMax = 16;
            } else if (ring < RING_COPPER) {
                material = Materials.COPPER;
                yMin = -32; yMax = 40;
            } else {
                material = Materials.IRON;
                yMin = -16; yMax = 56;
            }
            double vy = yMin + r.nextDouble() * (yMax - yMin);

            // Размеры линзы
            double a = 10 + r.nextDouble() * 20;   // вдоль простирания
            double b = 6 + r.nextDouble() * 10;    // вниз по падению
            double c = 1.3 + r.nextDouble() * 1.7; // толщина

            // Ориентация: простирание theta, падение dip (от горизонтали)
            double theta = r.nextDouble() * Math.PI;
            double dip = Math.toRadians(50 + r.nextDouble() * 35);
            double cs = Math.cos(theta), sn = Math.sin(theta);
            double cd = Math.cos(dip), sd = Math.sin(dip);
            // U = (cs, 0, sn); W = (-sn*cd, -sd, cs*cd); N = U x W
            out.add(new Vein(r.nextLong(), vx, vy, vz, a, b, c,
                    cs, sn,
                    -sn * cd, -sd, cs * cd,
                    sn * sd, -cd, -cs * sd,
                    material));
        }
        return List.copyOf(out);
    }

    // ---- Детерминированный хэш для позиций (не зависит от порядка генерации чанков) ----
    public static long mix(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static double hash01(long seed, int x, int y, int z) {
        long h = mix(seed ^ x);
        h = mix(h ^ (y * 31L));
        h = mix(h ^ (z * 131071L));
        return (h >>> 11) * 0x1.0p-53;
    }

    private OreNodes() {}
}
