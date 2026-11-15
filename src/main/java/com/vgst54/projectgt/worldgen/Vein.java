package com.vgst54.projectgt.worldgen;

/**
 * Одна жила: линза (эллипсоид) с тремя полуосями.
 *  a - вдоль простирания, b - вниз по падению, c - толщина (поперёк плоскости).
 * Базис (u, w, n) ортонормирован, плоскость жилы наклонена на угол падения.
 */
public record Vein(
        long seed,
        double cx, double cy, double cz,
        double a, double b, double c,
        double ux, double uz,                 // вдоль простирания (горизонтально)
        double wx, double wy, double wz,      // вниз по падению
        double nx, double ny, double nz,      // нормаль к плоскости жилы
        Mix mix,
        boolean surfaceMark) {   // ставить ли над жилой пятно гравия и руды на поверхности

    /** Радиус сферы, в которую целиком вписывается жила. */
    public double reach() {
        return Math.max(a, Math.max(b, c)) + 1;
    }
}
