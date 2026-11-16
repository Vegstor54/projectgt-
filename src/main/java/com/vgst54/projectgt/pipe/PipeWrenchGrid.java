package com.vgst54.projectgt.pipe;

import net.minecraft.core.Direction;

/**
 * Сетка 3×3 на грани трубы, на которую смотрит игрок с ключом (как в GTNH).
 * Центральная клетка переключает саму грань, четыре боковые переключают соседние стороны
 * относительно неё, угловые переключают обратную грань.
 * Одну и ту же математику используют клик на сервере и оверлей на клиенте, поэтому они не расходятся.
 *
 * Клетки делятся по границам ядра трубы: центр = [0.5 − halfWidth, 0.5 + halfWidth] по обеим осям грани.
 */
public final class PipeWrenchGrid {
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
    public static int band(double coordinate, double halfWidth) {
        if (coordinate < 0.5 - halfWidth) {
            return -1;
        }
        return coordinate > 0.5 + halfWidth ? 1 : 0;
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
    public static Direction directionAt(Direction face, double x, double y, double z, double halfWidth) {
        int u = band(along(tangentU(face), x, y, z), halfWidth);
        int v = band(along(tangentV(face), x, y, z), halfWidth);
        return directionOf(face, u, v);
    }
}
