package com.vgst54.projectgt.machine;

/**
 * Тир машин. Напряжение в EU/t на один ампер, шаг ×4 как в GregTech.
 * SV (steam voltage) стоит на месте ULV: у него обычное напряжение 8 EU/t, исключений в логике нет.
 * Паровые машины просто берут энергию из пара (см. {@link SteamPressure}).
 */
public enum Tier {
    SV("SV", 8),
    LV("LV", 32),
    MV("MV", 128),
    HV("HV", 512),
    EV("EV", 2_048),
    IV("IV", 8_192),
    LUV("LuV", 32_768),
    ZPM("ZPM", 131_072),
    UV("UV", 524_288),
    UHV("UHV", 2_097_152);

    private final String displayName;
    private final long voltage;

    Tier(String displayName, long voltage) {
        this.displayName = displayName;
        this.voltage = voltage;
    }

    public String displayName() {
        return displayName;
    }

    /** Максимальное напряжение тира, EU/t на один ампер. */
    public long voltage() {
        return voltage;
    }

    /** Паровой ли тир (работает от пара, а не от EU-буфера). */
    public boolean isSteam() {
        return this == SV;
    }

    /** Самый низкий тир, который тянет рецепт с таким расходом EU/t. UHV, если не тянет никто. */
    public static Tier forVoltage(long eut) {
        for (Tier tier : values()) {
            if (eut <= tier.voltage) {
                return tier;
            }
        }
        return UHV;
    }
}
