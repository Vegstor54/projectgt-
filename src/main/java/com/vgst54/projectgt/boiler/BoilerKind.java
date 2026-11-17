package com.vgst54.projectgt.boiler;

/** Чем греется котёл. */
public enum BoilerKind {
    /** Твёрдое топливо (уголь и всё, что горит в печи). */
    SOLID("coal", "Coal"),
    /** Солнце: греется только днём под открытым небом без дождя. */
    SOLAR("solar", "Solar"),
    /** Жидкое топливо (пока лава). */
    LIQUID("liquid", "Liquid Fuel");

    private final String id;
    private final String displayName;

    BoilerKind(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }
}
