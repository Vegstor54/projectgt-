package com.vgst54.projectgt.worldgen;

import com.vgst54.projectgt.material.Material;

/** Состав жилы: минералы и их доли (веса считаются в процентах от суммы). */
public final class Mix {
    public record Part(Material material, int weight) {}

    private final Material[] materials;
    private final double[] cumulative; // нормированные накопленные доли, последняя = 1.0
    private final int[] weights;
    private final int total;

    private Mix(Part[] parts) {
        materials = new Material[parts.length];
        weights = new int[parts.length];
        cumulative = new double[parts.length];
        int sum = 0;
        for (Part p : parts) sum += p.weight();
        total = sum;
        int acc = 0;
        for (int i = 0; i < parts.length; i++) {
            materials[i] = parts[i].material();
            weights[i] = parts[i].weight();
            acc += parts[i].weight();
            cumulative[i] = (double) acc / sum;
        }
    }

    public static Mix of(Part... parts) {
        return new Mix(parts);
    }

    public static Part p(Material material, int weight) {
        return new Part(material, weight);
    }

    /** Минерал по случайному числу u из [0, 1). */
    public Material pick(double u) {
        for (int i = 0; i < cumulative.length; i++) {
            if (u < cumulative[i]) return materials[i];
        }
        return materials[materials.length - 1];
    }

    /** Главный минерал (с наибольшей долей). */
    public Material main() {
        int best = 0;
        for (int i = 1; i < weights.length; i++) {
            if (weights[i] > weights[best]) best = i;
        }
        return materials[best];
    }

    /** Например: "Cassiterite 65%, Wolframite 25%, Pyrite 10%". */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < materials.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(materials[i].displayName()).append(' ').append(Math.round(100.0 * weights[i] / total)).append('%');
        }
        return sb.toString();
    }
}
