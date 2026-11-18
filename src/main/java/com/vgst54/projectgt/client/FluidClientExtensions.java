package com.vgst54.projectgt.client;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.registry.ModFluids;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Текстуры жидкостей: нужны, чтобы пар рисовался в интерфейсах и баках. */
@EventBusSubscriber(modid = ProjectGT.MODID, value = Dist.CLIENT)
public final class FluidClientExtensions {
    private static final ResourceLocation STEAM_STILL =
            ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "block/steam_still");
    private static final ResourceLocation STEAM_FLOW =
            ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "block/steam_flow");

    private FluidClientExtensions() {}

    @SubscribeEvent
    static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STEAM_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return STEAM_FLOW;
            }
        }, ModFluids.STEAM_TYPE.get());
    }
}
