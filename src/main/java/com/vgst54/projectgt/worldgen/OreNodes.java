package com.vgst54.projectgt.worldgen;

import static com.vgst54.projectgt.worldgen.Mix.p;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vgst54.projectgt.material.Materials;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

/**
 * Рудные узлы. Мир делится на ячейки CELL x CELL. В ячейке с шансом NODE_CHANCE есть узел одного из видов:
 *
 *  ГИДРОТЕРМАЛЬНЫЙ (гранитный массив, жилы крутые, зональность по расстоянию от центра):
 *    ядро -> олово и вольфрам; дальше медь (с пиритом и золотом); потом свинец и цинк; на краю серебро.
 *
 *  СЛОИСТЫЙ (осадочные и магматические пласты, залежи пологие, колец нет):
 *    узел целиком одного типа из набора LAYERED (магнетит с золотом, гематит с лимонитом, хром-никель, боксит, марганец).
 *
 *  НИЖНИЙ МИР (Незер): небольшие жилы кварца и серы со сфалеритом.
 *
 * Кроме того, в любом узле Верхнего мира изредка попадаются мелкие «акцентные» жилы сапфира.
 * Каждая жила содержит СМЕСЬ минералов в заданных долях (см. Type), не больше 4 минералов в одном типе.
 * Жилы стараются не налезать друг на друга (MIN_SEPARATION).
 * Всё считается детерминированно от сида мира, поэтому каждый чанк независимо восстанавливает свои жилы.
 */
public final class OreNodes {
    // ---- Крутилки баланса ----
    public static final int CELL = 512;
    public static final float NODE_CHANCE = 0.65f;
    /** Доля слоистых узлов среди узлов Верхнего мира. */
    public static final float LAYERED_SHARE = 0.30f;
    /** Шанс узла в ячейке Нижнего мира. */
    public static final float NETHER_NODE_CHANCE = 0.80f;
    /** Шанс того, что очередная жила Верхнего мира окажется мелкой жилой сапфира. */
    public static final float ACCENT_CHANCE = 0.06f;

    public static final double HYDRO_RADIUS_MIN = 110, HYDRO_RADIUS_VAR = 90;
    public static final int HYDRO_VEINS_MIN = 40, HYDRO_VEINS_VAR = 26;

    public static final double LAYER_RADIUS_MIN = 90, LAYER_RADIUS_VAR = 70;
    public static final int LAYER_VEINS_MIN = 14, LAYER_VEINS_VAR = 11;

    public static final double NETHER_RADIUS_MIN = 80, NETHER_RADIUS_VAR = 50;
    public static final int NETHER_VEINS_MIN = 24, NETHER_VEINS_VAR = 13;

    /**
     * Плотность руды внутри жилы: на краю линзы EDGE, в самом центре CORE (доля блоков руды).
     * Остальное остаётся вмещающей породой, то есть вкрапления камня есть даже в ядре.
     */
    public static final double DENSITY_EDGE = 0.25, DENSITY_CORE = 0.80;

    /** Попыток найти место жиле, не налезающее на соседние; после неудач ставим как есть. */
    public static final int PLACE_ATTEMPTS = 16;
    /** Минимальное расстояние между центрами жил как доля суммы их радиусов. */
    public static final double MIN_SEPARATION = 0.5;

    /** Степень распределения жил по радиусу: 0.5 = равномерно по площади, больше = гуще к центру. */
    public static final double CENTER_BIAS = 0.6;

    /** Границы колец гидротермального узла (доля радиуса). */
    public static final double RING_TIN = 0.25, RING_COPPER = 0.50, RING_LEADZINC = 0.80;

    /** Вид узла. */
    public enum Kind {
        HYDROTHERMAL("гидротермальный"), LAYERED("слоистый"), NETHER("незер");

        private final String title;

        Kind(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    /** Тип жилы: диапазон высот и состав. Доли в процентах (сумма не обязана быть 100). */
    private record Type(int yMin, int yMax, Mix mix) {}

    // ---- Гидротермальные ----
    private static final Type CORE = new Type(-48, 16, Mix.of(
            p(Materials.CASSITERITE, 65), p(Materials.WOLFRAMITE, 25), p(Materials.PYRITE, 10)));
    private static final Type COPPER_DEEP = new Type(-32, 24, Mix.of(
            p(Materials.CHALCOPYRITE, 60), p(Materials.PYRITE, 25), p(Materials.NATIVE_GOLD, 10), p(Materials.MALACHITE, 5)));
    private static final Type COPPER_OXIDIZED = new Type(16, 56, Mix.of(   // окисленная зона у поверхности
            p(Materials.MALACHITE, 55), p(Materials.CHALCOPYRITE, 30), p(Materials.NATIVE_GOLD, 10), p(Materials.PYRITE, 5)));
    private static final Type LEAD_ZINC = new Type(-16, 56, Mix.of(
            p(Materials.GALENA, 45), p(Materials.SPHALERITE, 40), p(Materials.PYRITE, 10), p(Materials.ARGENTITE, 5)));
    private static final Type SILVER_EDGE = new Type(-8, 56, Mix.of(
            p(Materials.ARGENTITE, 35), p(Materials.GALENA, 30), p(Materials.PYRITE, 20), p(Materials.NATIVE_GOLD, 15)));

    // ---- Слоистые ----
    private static final List<Type> LAYERED = List.of(
            // магнетитовые залежи: самородное золото и ванадиевый магнетит
            new Type(-48, 16, Mix.of(p(Materials.MAGNETITE, 70), p(Materials.NATIVE_GOLD, 20),
                    p(Materials.VANADIUM_MAGNETITE, 10))),
            // красный и бурый железняк у поверхности, с малахитом
            new Type(0, 56, Mix.of(p(Materials.HEMATITE, 50), p(Materials.LIMONITE, 35),
                    p(Materials.MALACHITE, 15))),
            // ультраосновные породы
            new Type(-48, 0, Mix.of(p(Materials.CHROMITE, 55), p(Materials.PENTLANDITE, 30),
                    p(Materials.VANADIUM_MAGNETITE, 15))),
            // бокситовая кора выветривания
            new Type(16, 64, Mix.of(p(Materials.BAUXITE, 80), p(Materials.LIMONITE, 20))),
            // марганцевые залежи
            new Type(-8, 48, Mix.of(p(Materials.PYROLUSITE, 70), p(Materials.SIDERITE, 15), p(Materials.LIMONITE, 15))));

    // ---- Акцентная жила Верхнего мира ----
    private static final Type SAPPHIRE = new Type(-48, 24, Mix.of(
            p(Materials.SAPPHIRE, 75), p(Materials.BAUXITE, 25)));

    // ---- Нижний мир ----
    private static final Type QUARTZ_VEIN = new Type(10, 110, Mix.of(p(Materials.QUARTZ, 100)));
    private static final Type SULFUR_VEIN = new Type(10, 110, Mix.of(
            p(Materials.SULFUR, 70), p(Materials.SPHALERITE, 30)));

    private record CellKey(long seed, int cellX, int cellZ, boolean nether) {}

    /** Рудный узел: центр, радиус, вид и все его жилы. NONE = в ячейке узла нет. */
    public record Node(double x, double z, double radius, Kind kind, List<Vein> veins) {
        public static final Node NONE = new Node(0, 0, 0, Kind.HYDROTHERMAL, List.of());

        public boolean exists() {
            return radius > 0;
        }
    }

    private static final Map<CellKey, Node> CACHE = new ConcurrentHashMap<>();

    public static Node nodeOfCell(long seed, int cellX, int cellZ, boolean nether) {
        if (CACHE.size() > 256) CACHE.clear();
        return CACHE.computeIfAbsent(new CellKey(seed, cellX, cellZ, nether), OreNodes::generate);
    }

    public static List<Vein> veinsOfCell(long seed, int cellX, int cellZ, boolean nether) {
        return nodeOfCell(seed, cellX, cellZ, nether).veins();
    }

    /** Существующие узлы в ячейках вокруг точки (радиус searchCells ячеек), от ближнего к дальнему. */
    public static List<Node> nodesNear(long seed, double x, double z, int searchCells, boolean nether) {
        int cx = Math.floorDiv((int) Math.floor(x), CELL);
        int cz = Math.floorDiv((int) Math.floor(z), CELL);
        List<Node> found = new ArrayList<>();
        for (int dx = -searchCells; dx <= searchCells; dx++) {
            for (int dz = -searchCells; dz <= searchCells; dz++) {
                Node n = nodeOfCell(seed, cx + dx, cz + dz, nether);
                if (n.exists()) found.add(n);
            }
        }
        found.sort(Comparator.comparingDouble(n -> Math.hypot(n.x() - x, n.z() - z)));
        return found;
    }

    private static Node generate(CellKey key) {
        long salt = key.nether() ? 0x4E45544845524CL : 0L;
        RandomSource r = new XoroshiroRandomSource(mix(mix(key.seed() ^ key.cellX()) ^ key.cellZ()) ^ salt);

        Kind kind;
        if (key.nether()) {
            if (r.nextFloat() > NETHER_NODE_CHANCE) return Node.NONE;
            kind = Kind.NETHER;
        } else {
            if (r.nextFloat() > NODE_CHANCE) return Node.NONE;
            kind = r.nextFloat() < LAYERED_SHARE ? Kind.LAYERED : Kind.HYDROTHERMAL;
        }

        double nodeX = key.cellX() * (double) CELL + 96 + r.nextDouble() * (CELL - 192);
        double nodeZ = key.cellZ() * (double) CELL + 96 + r.nextDouble() * (CELL - 192);

        double radius;
        int count;
        Type layerType = null;   // слоистый узел целиком одного типа: не больше 4 минералов на узел
        switch (kind) {
            case LAYERED -> {
                radius = LAYER_RADIUS_MIN + r.nextDouble() * LAYER_RADIUS_VAR;
                count = LAYER_VEINS_MIN + r.nextInt(LAYER_VEINS_VAR);
                layerType = LAYERED.get(r.nextInt(LAYERED.size()));
            }
            case NETHER -> {
                radius = NETHER_RADIUS_MIN + r.nextDouble() * NETHER_RADIUS_VAR;
                count = NETHER_VEINS_MIN + r.nextInt(NETHER_VEINS_VAR);
            }
            default -> {
                radius = HYDRO_RADIUS_MIN + r.nextDouble() * HYDRO_RADIUS_VAR;
                count = HYDRO_VEINS_MIN + r.nextInt(HYDRO_VEINS_VAR);
            }
        }

        List<Vein> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // Тип, не зависящий от кольца: акцент, жилы Незера, слоистый узел
            Type fixed = null;
            if (kind != Kind.NETHER && r.nextFloat() < ACCENT_CHANCE) {
                fixed = SAPPHIRE;
            } else if (kind == Kind.NETHER) {
                fixed = r.nextBoolean() ? QUARTZ_VEIN : SULFUR_VEIN;
            } else if (kind == Kind.LAYERED) {
                fixed = layerType;
            }

            // Размеры и наклон
            double a, b, c, dipDeg;
            if (fixed == SAPPHIRE) {
                a = 6 + r.nextDouble() * 6;     // длина 12-24
                b = 5 + r.nextDouble() * 4;
                c = 1.5 + r.nextDouble() * 1.0; // толщина 3-5
                dipDeg = 30 + r.nextDouble() * 55;
            } else if (fixed == QUARTZ_VEIN) {
                a = 7 + r.nextDouble() * 7;     // небольшая жила
                b = 5 + r.nextDouble() * 5;
                c = 1.5 + r.nextDouble() * 1.5;
                dipDeg = 20 + r.nextDouble() * 65;
            } else if (fixed == SULFUR_VEIN) {
                a = 12 + r.nextDouble() * 12;
                b = 8 + r.nextDouble() * 8;
                c = 2.0 + r.nextDouble() * 2.0;
                dipDeg = 20 + r.nextDouble() * 65;
            } else if (kind == Kind.LAYERED) {
                // Пологие пласты
                a = 14 + r.nextDouble() * 16;   // длина 28-60
                b = 10 + r.nextDouble() * 10;   // ширина 20-40
                c = 2.5 + r.nextDouble() * 2.0; // толщина 5-9
                dipDeg = 5 + r.nextDouble() * 25;
            } else {
                // Крутые жилы
                a = 18 + r.nextDouble() * 22;   // длина 36-80
                b = 10 + r.nextDouble() * 12;   // высота 20-44
                c = 2.5 + r.nextDouble() * 2.0; // толщина 5-9
                dipDeg = 50 + r.nextDouble() * 35;
            }
            double reach = Math.max(a, Math.max(b, c)) + 1;

            // Позиция: несколько попыток найти место, не налезающее на уже стоящие жилы
            double vx = 0, vy = 0, vz = 0;
            Type type = null;
            for (int attempt = 0; attempt < PLACE_ATTEMPTS; attempt++) {
                double t = Math.pow(r.nextDouble(), CENTER_BIAS);   // 0 = центр узла, 1 = край
                double ang = r.nextDouble() * Math.PI * 2;
                vx = nodeX + Math.cos(ang) * t * radius;
                vz = nodeZ + Math.sin(ang) * t * radius;
                type = fixed != null ? fixed : hydroType(t + (r.nextDouble() - 0.5) * 0.1, r);
                vy = type.yMin() + r.nextDouble() * (type.yMax() - type.yMin());
                if (!overlaps(out, vx, vy, vz, reach)) break;
            }

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
                    type.mix()));
        }
        return new Node(nodeX, nodeZ, radius, kind, List.copyOf(out));
    }

    private static boolean overlaps(List<Vein> placed, double x, double y, double z, double reach) {
        for (Vein o : placed) {
            double dx = o.cx() - x, dy = o.cy() - y, dz = o.cz() - z;
            double min = MIN_SEPARATION * (o.reach() + reach);
            if (dx * dx + dy * dy + dz * dz < min * min) return true;
        }
        return false;
    }

    /** Тип жилы гидротермального узла по расстоянию от центра (ring: 0 = центр, 1 = край). */
    private static Type hydroType(double ring, RandomSource r) {
        if (ring < RING_TIN) {
            return CORE;
        } else if (ring < RING_COPPER) {
            return r.nextFloat() < 0.30f ? COPPER_OXIDIZED : COPPER_DEEP;
        } else if (ring < RING_LEADZINC) {
            return LEAD_ZINC;
        } else {
            return SILVER_EDGE;
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
