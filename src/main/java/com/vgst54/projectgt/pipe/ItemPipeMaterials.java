package com.vgst54.projectgt.pipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.Materials;

/**
 * Материалы предметных труб (значения из таблицы GTNH). Для каждого размера (Tiny..Huge) задаётся
 * пропускная способность stacks за period секунд и routing value; null значит «такого размера нет».
 * Restrictive не отдельный блок: это режим любой трубы (ключ с shift), routing value ×100.
 *
 * Остальные материалы из таблицы (Brass, Electrum, Cobalt, Aluminium, PVC, Platinum, Bedrockium, Osmium,
 * Quantium, Fluxed Electrum, Black Plutonium) добавим вместе с самими материалами в Materials.
 */
public final class ItemPipeMaterials {
    private static final List<ItemPipeType> TYPES = new ArrayList<>();

    /** Параметры одного размера. */
    private record Spec(int stacks, int periodSeconds, int routingValue) {}

    private static Spec spec(int stacks, int periodSeconds, int routingValue) {
        return new Spec(stacks, periodSeconds, routingValue);
    }

    static {
        add(Materials.TIN,
                spec(1, 8, 262_144), spec(1, 4, 131_072), spec(1, 2, 65_536), spec(1, 1, 32_768), spec(2, 1, 16_384));
        add(Materials.NICKEL,
                null, null, spec(1, 1, 32_768), spec(2, 1, 16_384), spec(4, 1, 8_192));
    }

    private ItemPipeMaterials() {}

    private static void add(Material material, Spec... bySize) {
        PipeSize[] sizes = PipeSize.values();
        if (bySize.length != sizes.length) {
            throw new IllegalArgumentException(material.id() + ": need " + sizes.length + " size specs");
        }
        for (int i = 0; i < sizes.length; i++) {
            Spec spec = bySize[i];
            if (spec == null) {
                continue;
            }
            TYPES.add(new ItemPipeType(material, sizes[i], spec.stacks(), spec.periodSeconds(), spec.routingValue()));
        }
    }

    public static List<ItemPipeType> all() {
        return Collections.unmodifiableList(TYPES);
    }
}
