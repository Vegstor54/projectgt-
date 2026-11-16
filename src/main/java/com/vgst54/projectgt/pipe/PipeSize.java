package com.vgst54.projectgt.pipe;

/** Размер трубы. Определяет толщину модели и колонку в таблице пропускной способности. */
public enum PipeSize {
    TINY("tiny", "Tiny", 4),
    SMALL("small", "Small", 6),
    NORMAL("normal", "Normal", 8),
    LARGE("large", "Large", 10),
    HUGE("huge", "Huge", 12);

    private final String id;
    private final String displayName;
    private final int thickness;

    PipeSize(String id, String displayName, int thickness) {
        this.id = id;
        this.displayName = displayName;
        this.thickness = thickness;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** Толщина трубы в пикселях модели (из 16). */
    public int thickness() {
        return thickness;
    }

    /** Половина толщины в долях блока: ядро трубы занимает 0.5 ± halfWidth. */
    public double halfWidth() {
        return thickness / 32.0;
    }
}
