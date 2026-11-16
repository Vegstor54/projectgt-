package com.vgst54.projectgt.pipe;

import net.minecraft.core.Direction;

/**
 * Сетка 3×3 на грани трубы, на которую смотрит игрок с ключом (как в GTNH).
 * Центральная клетка переключает саму грань, четыре боковые переключают соседние стороны
 * относительно неё, угловые переключают обратную грань.
 * Одну и ту же математику используют клик на сервере и оверлей на клиенте, поэтому они не расходятся.
 *
 * Центральная клетка намеренно маленькая и не зависит от толщины трубы: [0.5 − CENTER_HALF, 0.5 + CENTER_HALF]
 * по обеим осям грани. Остальное место отдано боковым и угловым клеткам, в них проще попасть.
 */
public final class PipeWrenchGrid {
    /** Половина стороны центральной клетки в долях блока (0.125 = 4 пикселя из 16). */
    public static final double CENTER_HALF = 0.125;

    private PipeWrenchGrid() {}

    /** Первая касательная ось грани (всегда положительное направление: EAST, SOUTH или UP). */
    public static Direction tangentU(Direction face) {
        return switch (face.getAxis()) {
            case Y, Z -> Direction.EAST;
            case X -> Direction.SOUTH;
        };
    }

    /** Вторая касательная ось грани (всегда положительное направление: SOUTH или UP). */
    public static Direction tangentV(Direction face) {
        return switch (face.getAxis()) {
            case Y -> Direction.SOUTH;
            case X, Z -> Direction.UP;
        };
    }

    /** Координата вдоль оси: -1 до ядра, 0 в пределах ядра, 1 после него. */
    public static int band(double coordinate) {
        if (coordinate < 0.5 - CENTER_HALF) {
            return -1;
        }
        return coordinate > 0.5 + CENTER_HALF ? 1 : 0;
    }

    /** Сторона трубы, за которую отвечает клетка (uBand, vBand) на грани face. */
    public static Direction directionOf(Direction face, int uBand, int vBand) {
        if (uBand == 0 && vBand == 0) {
            return face;
        }
        if (uBand != 0 && vBand != 0) {
            return face.getOpposite();
        }
        if (uBand != 0) {
            Direction u = tangentU(face);
            return uBand > 0 ? u : u.getOpposite();
        }
        Direction v = tangentV(face);
        return vBand > 0 ? v : v.getOpposite();
    }

    /** Значение координаты точки (в долях блока, 0..1) вдоль данной оси. */
    public static double along(Direction axisDirection, double x, double y, double z) {
        return switch (axisDirection.getAxis()) {
            case X -> x;
            case Y -> y;
            case Z -> z;
        };
    }

    /** Сторона трубы под точкой клика. x, y, z: положение попадания внутри блока (0..1). */
    public static Direction directionAt(Direction face, double x, double y, double z) {
        int u = band(along(tangentU(face), x, y, z));
        int v = band(along(tangentV(face), x, y, z));
        return directionOf(face, u, v);
    }
}
