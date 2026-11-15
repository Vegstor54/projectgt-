package com.vgst54.projectgt.worldgen;

import static com.vgst54.projectgt.worldgen.Mix.p;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vgst54.projectgt.material.Materials;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

/**
 * Генерация жил по сетке, как в GregTech. Мир делится на ячейки GRID_CHUNKS x GRID_CHUNKS чанков (7 x 7 = 112 блоков).
 * В каждой ячейке одна жила: центр ячейки плюс случайный сдвиг до +-JITTER блоков по X и Z, тип по весам.
 * Ячейка кратна чанку, поэтому чанк целиком лежит в одной ячейке, а до жил соседних ячеек хватает 3 x 3.
 *
 * (Класс называется OreNodes по старой схеме с узлами; узлов больше нет.)
 *
 * Каждая жила содержит смесь минералов в заданных долях (см. Type), не больше 4 минералов в одном типе.
 * Всё считается детерминированно от сида мира, поэтому каждый чанк независимо восстанавливает свои жилы.
 */
public final class OreNodes {
    // ---- Крутилки баланса ----
    /** Размер ячейки сетки в чанках. */
    public static final int GRID_CHUNKS = 7;
    public static final int CELL = GRID_CHUNKS * 16;
    /** Максимальный сдвиг центра жилы от центра ячейки по X и по Z (блоков, в любую сторону). */
    public static final double JITTER = 20;
    /** Шанс жилы в ячейке Верхнего мира и Незера. */
    public static final float CELL_CHANCE = 1.0f;
    public static final float NETHER_CELL_CHANCE = 0.6f;
    /** Попыток найти для центра жилы твёрдое место (не воздух, не пещера, не жидкость); потом ячейка остаётся пустой. */
    public static final int PLACE_ATTEMPTS = 8;

    /**
     * Плотность руды внутри жилы: на краю линзы EDGE, в самом центре CORE (доля блоков руды).
     * Остальное остаётся вмещающей породой, то есть вкрапления камня есть даже в ядре.
     */
    public static final double DENSITY_EDGE = 0.25, DENSITY_CORE = 0.80;

    /** Какая доля жил получает пятно гравия и руды на поверхности. */
    public static final float SURFACE_CHANCE = 0.40f;
    /** Пятно не ставим, если жила глубже этого под поверхностью. */
    public static final int SURFACE_MAX_DEPTH = 128;
    /** Радиус пятна = BASE + PER_LENGTH * длина жилы. */
    public static final double SURFACE_RADIUS_BASE = 2.5, SURFACE_RADIUS_PER_LENGTH = 0.1;

    /** Форма жилы: полуоси линзы и угол падения как «минимум + случайная добавка», в блоках и градусах. */
    private record Shape(double a, double aVar, double b, double bVar, double c, double cVar,
                         double dip, double dipVar, boolean surface) {}

    private static final Shape STEEP = new Shape(18, 22, 10, 12, 2.5, 2.0, 50, 35, true);   // крутые жилы 36-80 в длину
    private static final Shape PLATE = new Shape(14, 16, 10, 10, 2.5, 2.0, 5, 25, true);    // пологие пласты 28-60
    private static final Shape GEM = new Shape(6, 6, 5, 4, 1.5, 1.0, 30, 55, false);        // самоцветы 12-24
    private static final Shape QUARTZ_SHAPE = new Shape(7, 7, 5, 5, 1.5, 1.5, 20, 65, false);
    private static final Shape SULFUR_SHAPE = new Shape(12, 12, 8, 8, 2, 2, 20, 65, false);

    /** Тип жилы: диапазон высот, форма, вес при выборе и состав. Доли минералов в процентах. */
    private record Type(int yMin, int yMax, Shape shape, int weight, Mix mix) {}

    // Каждый минерал встречается только в одной жиле. Мелких жил нет.
    private static final List<Type> OVERWORLD = List.of(
            // Гидротермальные (крутые жилы)
            new Type(-48, 16, STEEP, 10, Mix.of(     // олово и вольфрам
                    p(Materials.CASSITERITE, 70), p(Materials.WOLFRAMITE, 30))),
            new Type(-48, 16, STEEP, 10, Mix.of(     // магнетит
                    p(Materials.MAGNETITE, 65), p(Materials.VANADIUM_MAGNETITE, 25), p(Materials.NATIVE_GOLD, 10))),
            new Type(-32, 24, STEEP, 12, Mix.of(     // медная жила: халькопирит и пирит + ванильные медь и железо
                    p(Materials.CHALCOPYRITE, 40), p(Materials.PYRITE, 25), p(Materials.COPPER, 20), p(Materials.IRON, 15))),
            new Type(16, 56, STEEP, 5, Mix.of(       // окисленная медь у поверхности
                    p(Materials.MALACHITE, 100))),
            new Type(-16, 56, STEEP, 12, Mix.of(     // свинец и цинк
                    p(Materials.GALENA, 55), p(Materials.SPHALERITE, 45))),
            new Type(-8, 56, STEEP, 6, Mix.of(       // серебро
                    p(Materials.ARGENTITE, 100))),
            // Слоистые (пологие пласты)
            new Type(0, 56, PLATE, 8, Mix.of(        // красный и бурый железняк
                    p(Materials.HEMATITE, 60), p(Materials.LIMONITE, 40))),
            new Type(-48, 0, PLATE, 4, Mix.of(       // хром и никель
                    p(Materials.CHROMITE, 60), p(Materials.PENTLANDITE, 40))),
            new Type(16, 64, PLATE, 5, Mix.of(
                    p(Materials.BAUXITE, 100))),
            new Type(-8, 48, PLATE, 4, Mix.of(       // марганец с сидеритом
                    p(Materials.PYROLUSITE, 70), p(Materials.SIDERITE, 30))),
            new Type(0, 64, PLATE, 8, Mix.of(        // угольный пласт: только уголь
                    p(Materials.COAL, 100))),
            // Самоцветы
            new Type(-48, 24, GEM, 3, Mix.of(
                    p(Materials.SAPPHIRE, 100))));

    private static final List<Type> NETHER = List.of(
            new Type(10, 110, QUARTZ_SHAPE, 1, Mix.of(p(Materials.QUARTZ, 100))),
            new Type(10, 110, SULFUR_SHAPE, 1, Mix.of(p(Materials.SULFUR, 100))));

    private record CellKey(long seed, int cellX, int cellZ, boolean nether) {}

    private static final Map<CellKey, List<Vein>> CACHE = new ConcurrentHashMap<>();

    /** Жилы ячейки сетки (ноль или одна). */
    public static List<Vein> veinsOfCell(long seed, int cellX, int cellZ, boolean nether, TerrainProbe probe) {
        if (CACHE.size() > 4096) CACHE.clear();
        return CACHE.computeIfAbsent(new CellKey(seed, cellX, cellZ, nether), k -> generate(k, probe));
    }

    public static void clearCache() {
        CACHE.clear();
    }

    /** Все жилы в ячейках вокруг точки (radius ячеек в каждую сторону). */
    public static List<Vein> veinsNear(long seed, double x, double z, int searchCells, boolean nether, TerrainProbe probe) {
        int cx = Math.floorDiv((int) Math.floor(x), CELL);
        int cz = Math.floorDiv((int) Math.floor(z), CELL);
        List<Vein> found = new ArrayList<>();
        for (int dx = -searchCells; dx <= searchCells; dx++) {
            for (int dz = -searchCells; dz <= searchCells; dz++) {
                found.addAll(veinsOfCell(seed, cx + dx, cz + dz, nether, probe));
            }
        }
        return found;
    }

    private static List<Vein> generate(CellKey key, TerrainProbe probe) {
        boolean nether = key.nether();
        long salt = nether ? 0x4E45544845524CL : 0L;
        RandomSource r = new XoroshiroRandomSource(mix(mix(key.seed() ^ key.cellX()) ^ key.cellZ()) ^ salt);
        if (r.nextFloat() >= (nether ? NETHER_CELL_CHANCE : CELL_CHANCE)) return List.of();

        Type type = pick(nether ? NETHER : OVERWORLD, r);
        Shape s = type.shape();

        // Центр: середина ячейки плюс случайный сдвиг до +-JITTER блоков; не в воздухе, пещере или жидкости
        double centerX = key.cellX() * (double) CELL + CELL / 2.0;
        double centerZ = key.cellZ() * (double) CELL + CELL / 2.0;
        double vx = 0, vy = 0, vz = 0;
        boolean ok = false;
        for (int attempt = 0; attempt < PLACE_ATTEMPTS && !ok; attempt++) {
            vx = centerX + (r.nextDouble() * 2 - 1) * JITTER;
            vz = centerZ + (r.nextDouble() * 2 - 1) * JITTER;
            vy = type.yMin() + r.nextDouble() * (type.yMax() - type.yMin());
            ok = probe.isSolid(Mth.floor(vx), Mth.floor(vy), Mth.floor(vz));
        }
        if (!ok) return List.of();

        double a = s.a() + r.nextDouble() * s.aVar();
        double b = s.b() + r.nextDouble() * s.bVar();
        double c = s.c() + r.nextDouble() * s.cVar();
        double dip = Math.toRadians(s.dip() + r.nextDouble() * s.dipVar());
        double theta = r.nextDouble() * Math.PI;
        boolean mark = s.surface() && r.nextFloat() < SURFACE_CHANCE;

        // Ориентация: простирание theta, падение dip (от горизонтали)
        double cs = Math.cos(theta), sn = Math.sin(theta);
        double cd = Math.cos(dip), sd = Math.sin(dip);
        // U = (cs, 0, sn); W = (-sn*cd, -sd, cs*cd); N = U x W
        return List.of(new Vein(r.nextLong(), vx, vy, vz, a, b, c,
                cs, sn,
                -sn * cd, -sd, cs * cd,
                sn * sd, -cd, -cs * sd,
                type.mix(), mark));
    }

    private static Type pick(List<Type> pool, RandomSource r) {
        int total = 0;
        for (Type t : pool) total += t.weight();
        int roll = r.nextInt(total);
        for (Type t : pool) {
            roll -= t.weight();
            if (roll < 0) return t;
        }
        return pool.get(pool.size() - 1);
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
