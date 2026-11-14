package com.vgst54.projectgt.worldgen;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.vgst54.projectgt.Config;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Убирает ванильные руды из биомов, пока в конфиге выключена enableVanillaOreGeneration.
 * Биом-модификаторы заданы данными, поэтому читать конфиг может только свой модификатор.
 */
public final class RemoveVanillaOresModifier implements BiomeModifier {
    public static final RemoveVanillaOresModifier INSTANCE = new RemoveVanillaOresModifier();
    public static final MapCodec<RemoveVanillaOresModifier> CODEC = MapCodec.unit(INSTANCE);

    // Ключевые слова в путях ванильных placed features руд (ore_coal_upper, ore_diamond_buried, ore_debris_large...).
    private static final List<String> ORE_KEYWORDS = List.of(
            "coal", "iron", "copper", "gold", "redstone", "diamond", "lapis", "emerald", "debris", "quartz");

    private RemoveVanillaOresModifier() {}

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.REMOVE || Config.ENABLE_VANILLA_ORE_GENERATION.get()) {
            return;
        }
        var generation = builder.getGenerationSettings();
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            generation.getFeatures(step).removeIf(RemoveVanillaOresModifier::isVanillaOre);
        }
    }

    private static boolean isVanillaOre(Holder<PlacedFeature> feature) {
        return feature.unwrapKey().map(key -> {
            var id = key.location();
            if (!"minecraft".equals(id.getNamespace()) || !id.getPath().startsWith("ore_")) {
                return false;
            }
            return ORE_KEYWORDS.stream().anyMatch(id.getPath()::contains);
        }).orElse(false);
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
