package com.vgst54.projectgt.registry;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.pipe.FluidPipeBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ProjectGT.MODID);

    /** Одна блок-сущность на все жидкостные трубы. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE =
            TYPES.register("fluid_pipe", () -> BlockEntityType.Builder.of(
                    FluidPipeBlockEntity::new,
                    ModBlocks.fluidPipes().map(pipe -> (Block) pipe.block().get()).toArray(Block[]::new)
            ).build(null));

    private ModBlockEntities() {}
}
