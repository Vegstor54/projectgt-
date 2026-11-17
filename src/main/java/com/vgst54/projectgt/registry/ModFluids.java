package com.vgst54.projectgt.registry;

import com.vgst54.projectgt.ProjectGT;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Жидкости мода. Пока только пар: газ, который живёт в баках, трубах и машинах.
 * Блока и ведра у него нет, в мир он не выливается.
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ProjectGT.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, ProjectGT.MODID);

    /** Пар: газ (отрицательная плотность), 373 K. Температура важна трубам с пределом нагрева. */
    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = FLUID_TYPES.register("steam",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type." + ProjectGT.MODID + ".steam")
                    .density(-1000)
                    .temperature(373)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STEAM = FLUIDS.register("steam",
            () -> new BaseFlowingFluid.Source(steamProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> STEAM_FLOWING = FLUIDS.register("flowing_steam",
            () -> new BaseFlowingFluid.Flowing(steamProperties()));

    private static BaseFlowingFluid.Properties steamProperties() {
        return new BaseFlowingFluid.Properties(STEAM_TYPE, STEAM, STEAM_FLOWING);
    }

    private ModFluids() {}
}
