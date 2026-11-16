package com.vgst54.projectgt.pipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.Materials;

/**
 * Материалы жидкостных труб паровой эпохи (значения из таблицы GTNH).
 * Порядок throughput: Tiny, Small, Normal, Large, Huge; 0 означает «такого размера нет».
 * Новый материал трубы добавляется одной строкой add(...), нужен ./gradlew runData.
 */
public final class FluidPipeMaterials {
    private static final List<FluidPipeType> TYPES = new ArrayList<>();

    static {
        add(Materials.COPPER, 1000, 60, 120, 400, 800, 1600);
        add(Materials.BRONZE, 2000, 400, 800, 2400, 4800, 9600);
        add(Materials.STEEL, 2500, 800, 1600, 4800, 9600, 19200);
    }

    private FluidPipeMaterials() {}

    private static void add(Material material, int heatLimit, int... throughputBySize) {
        PipeSize[] sizes = PipeSize.values();
        if (throughputBySize.length != sizes.length) {
            throw new IllegalArgumentException(material.id() + ": need " + sizes.length + " throughput values");
        }
        for (int i = 0; i < sizes.length; i++) {
            if (throughputBySize[i] > 0) {
                TYPES.add(new FluidPipeType(material, sizes[i], throughputBySize[i], heatLimit));
            }
        }
    }

    public static List<FluidPipeType> all() {
        return Collections.unmodifiableList(TYPES);
    }
}
