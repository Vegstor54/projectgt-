package com.vgst54.projectgt.worldgen;

import com.vgst54.projectgt.ProjectGT;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, ProjectGT.MODID);

    public static final DeferredHolder<Feature<?>, OreNodeFeature> ORE_NODES =
            FEATURES.register("ore_nodes", OreNodeFeature::new);

    private ModFeatures() {}
}
