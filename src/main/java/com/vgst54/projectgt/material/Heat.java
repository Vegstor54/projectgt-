package com.vgst54.projectgt.material;

/**
 * Температуры источников тепла. Металл можно получить там, где потолок источника
 * не ниже его meltingPoint. Потолки для установок выше печи добавим вместе с самими установками.
 */
public final class Heat {
    /**
     * Потолок обычной ванильной печи и плавильной печи. Железо (1538) выше: ему нужна установка
     * потеплее. Чтобы разрешить железо в обычной печи, подними значение, например до 1600.
     */
    public static final int FURNACE_MAX = 1100;

    /** Можно ли получить этот металл в обычной печи. Для null и для материала без точки плавления ограничения нет. */
    public static boolean furnaceCanSmelt(Material metal) {
        return metal == null || metal.meltingPoint() <= FURNACE_MAX;
    }

    private Heat() {}
}
