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
 * Рудные узлы. Мир делится на ячейки CELL x CELL. В ячейке с шансом NODE_CHANCE есть узел одного из видов:
 *
 *  ГИДРОТЕРМАЛЬНЫЙ (гранитный массив, жилы крутые, зональность по расстоянию от центра):
 *    ядро -> касситерит, вольфрамит; дальше медь (халькопирит, малахит у поверхности, пирит);
 *    потом галенит и сфалерит; на краю аргентит, галенит, пирит.
 *
 *  СЛОИСТЫЙ (осадочные и магматические пласты, залежи пологие, колец нет):
 *    у узла два минерала из набора LAYERED (железняки, хромит, пентландит, боксит, пиролюзит).
 *
 * Всё считается детерминированно от сида мира, поэтому каждый чанк независимо восстанавливает свои жилы.
 */
public final class OreNodes {
    // ---- Крутилки баланса ----
    public static final int CELL = 512;
    public static final float NODE_CHANCE = 0.65f;
    /** Доля слоистых узлов среди всех. */
    public static final float LAYERED_SHARE = 0.30f;

    public static final double HYDRO_RADIUS_MIN = 110, HYDRO_RADIUS_VAR = 90;
    public static final int HYDRO_VEINS_MIN = 40, HYDRO_VEINS_VAR = 26;

    public static final double LAYER_RADIUS_MIN = 90, LAYER_RADIUS_VAR = 70;
    public static final int LAYER_VEINS_MIN = 14, LAYER_VEINS_VAR = 11;

    /** Границы колец гидротермального узла (доля радиуса). */
    public static final double RING_TIN = 0.25, RING_COPPER = 0.50, RING_LEADZINC = 0.80;

    /** Минерал и диапазон высот, где он образуется. */
    private record Mineral(Material material, int yMin, int yMax) {}

    private static final Mineral CASSITERITE  = new Mineral(Materials.CASSITERITE, -48, 16);
    private static final Mineral WOLFRAMITE   = new Mineral(Materials.WOLFRAMITE, -48, 8);
    private static final Mineral CHALCOPYRITE = new Mineral(Materials.CHALCOPYRITE, -32, 24);
    private static final Mineral MALACHITE    = new Mineral(Materials.MALACHITE, 16, 56);   // окисленная зона у поверхности
    private static final Mineral PYRITE       = new Mineral(Materials.PYRITE, -32, 40);
    private static final Mineral GALENA       = new Mineral(Materials.GALENA, -16, 56);
    private static final Mineral SPHALERITE   = new Mineral(Materials.SPHALERITE, -16, 56);
    private static final Mineral ARGENTITE    = new Mineral(Materials.ARGENTITE, -8, 56);

    private static final List<Mineral> LAYERED = List.of(
            new Mineral(Materials.HEMATITE, -40, 24),
            new Mineral(Materials.MAGNETITE, -48, 16),
            new Mineral(Materials.LIMONITE, 8, 64),     // бурый железняк: окисленный, у поверхности
            new Mineral(Materials.SIDERITE, -16, 40),
            new Mineral(Materials.CHROMITE, -48, 0),
            new Mineral(Materials.PENTLANDITE, -48, 0),
            new Mineral(Materials.BAUXITE, 16, 64),
            new Mineral(Materials.PYROLUSITE, -8, 48));

    private record CellKey(long seed, int cellX, int cellZ) {}

    /** Рудный узел: центр, радиус, вид и все его жилы. NONE = в ячейке узла нет. */
    public record Node(double x, double z, double radius, boolean layered, List<Vein> veins) {
        public static final Node NONE = new Node(0, 0, 0, false, List.of());

        public boolean exists() {
            return radius > 0;
        }
    }

    private static final Map<CellKey, Node> CACHE = new ConcurrentHashMap<>();

    public static Node nodeOfCell(long seed, int cellX, int cellZ) {
        if (CACHE.size() > 256) CACHE.clear();
        return CACHE.computeIfAbsent(new CellKey(seed, cellX, cellZ), OreNodes::generate);
    }

    public static List<Vein> veinsOfCell(long seed, int cellX, int cellZ) {
        return nodeOfCell(seed, cellX, cellZ).veins();
    }

    /** Существующие узлы в ячейках вокруг точки (радиус searchCells ячеек), от ближнего к дальнему. */
    public static List<Node> nodesNear(long seed, double x, double z, int searchCells) {
        int cx = Math.floorDiv((int) Math.floor(x), CELL);
        int cz = Math.floorDiv((int) Math.floor(z), CELL);
        List<Node> found = new ArrayList<>();
        for (int dx = -searchCells; dx <= searchCells; dx++) {
            for (int dz = -searchCells; dz <= searchCells; dz++) {
                Node n = nodeOfCell(seed, cx + dx, cz + dz);
                if (n.exists()) found.add(n);
            }
        }
        found.sort(java.util.Comparator.comparingDouble(n -> Math.hypot(n.x() - x, n.z() - z)));
        return found;
    }

    private static Node generate(CellKey key) {
        RandomSource r = new XoroshiroRandomSource(mix(mix(key.seed() ^ key.cellX()) ^ key.cellZ()));
        if (r.nextFloat() > NODE_CHANCE) return Node.NONE;
        boolean layered = r.nextFloat() < LAYERED_SHARE;

        double nodeX = key.cellX() * (double) CELL + 96 + r.nextDouble() * (CELL - 192);
        double nodeZ = key.cellZ() * (double) CELL + 96 + r.nextDouble() * (CELL - 192);

        double radius;
        int count;
        Mineral layerA = null, layerB = null;
        if (layered) {
            radius = LAYER_RADIUS_MIN + r.nextDouble() * LAYER_RADIUS_VAR;
            count = LAYER_VEINS_MIN + r.nextInt(LAYER_VEINS_VAR);
            int i = r.nextInt(LAYERED.size());
            int j = (i + 1 + r.nextInt(LAYERED.size() - 1)) % LAYERED.size();
            layerA = LAYERED.get(i);
            layerB = LAYERED.get(j);
        } else {
            radius = HYDRO_RADIUS_MIN + r.nextDouble() * HYDRO_RADIUS_VAR;
            count = HYDRO_VEINS_MIN + r.nextInt(HYDRO_VEINS_VAR);
        }

        List<Vein> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double t = Math.pow(r.nextDouble(), 0.8);          // 0 = центр узла, 1 = край
            double ang = r.nextDouble() * Math.PI * 2;
            double vx = nodeX + Math.cos(ang) * t * radius;
            double vz = nodeZ + Math.sin(ang) * t * radius;

            Mineral m;
            double a, b, c, dipDeg;
            if (layered) {
                m = r.nextBoolean() ? layerA : layerB;
                // Пологие широкие пласты
                a = 24 + r.nextDouble() * 26;   // длина 48-100
                b = 16 + r.nextDouble() * 16;   // ширина 32-64
                c = 2.5 + r.nextDouble() * 2.0; // толщина 5-9
                dipDeg = 5 + r.nextDouble() * 25;
            } else {
                m = hydroMineral(t + (r.nextDouble() - 0.5) * 0.1, r);
                // Крутые жилы
                a = 18 + r.nextDouble() * 22;   // длина 36-80
                b = 10 + r.nextDouble() * 12;   // высота 20-44
                c = 2.5 + r.nextDouble() * 2.0; // толщина 5-9
                dipDeg = 50 + r.nextDouble() * 35;
            }
            double vy = m.yMin() + r.nextDouble() * (m.yMax() - m.yMin());

            // Ориентация: простирание theta, падение dip (от горизонтали)
            double theta = r.nextDouble() * Math.PI;
            double dip = Math.toRadians(dipDeg);
            double cs = Math.cos(theta), sn = Math.sin(theta);
            double cd = Math.cos(dip), sd = Math.sin(dip);
            // U = (cs, 0, sn); W = (-sn*cd, -sd, cs*cd); N = U x W
            out.add(new Vein(r.nextLong(), vx, vy, vz, a, b, c,
                    cs, sn,
                    -sn * cd, -sd, cs * cd,
                    sn * sd, -cd, -cs * sd,
                    m.material()));
        }
        return new Node(nodeX, nodeZ, radius, layered, List.copyOf(out));
    }

    /** Минерал гидротермального узла по расстоянию от центра (ring: 0 = центр, 1 = край). */
    private static Mineral hydroMineral(double ring, RandomSource r) {
        double p = r.nextDouble();
        if (ring < RING_TIN) {
            return p < 0.60 ? CASSITERITE : WOLFRAMITE;
        } else if (ring < RING_COPPER) {
            return p < 0.25 ? MALACHITE : p < 0.80 ? CHALCOPYRITE : PYRITE;
        } else if (ring < RING_LEADZINC) {
            return p < 0.40 ? GALENA : p < 0.80 ? SPHALERITE : ARGENTITE;
        } else {
            return p < 0.50 ? ARGENTITE : p < 0.75 ? GALENA : PYRITE;
        }
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
