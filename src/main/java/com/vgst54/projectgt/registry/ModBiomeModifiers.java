package com.vgst54.projectgt.registry;

import com.mojang.serialization.MapCodec;
import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.worldgen.RemoveVanillaOresModifier;

import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModBiomeModifiers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, ProjectGT.MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<RemoveVanillaOresModifier>> REMOVE_VANILLA_ORES =
            SERIALIZERS.register("remove_vanilla_ores", () -> RemoveVanillaOresModifier.CODEC);

    private ModBiomeModifiers() {}
}
