package com.vgst54.projectgt.registry;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.boiler.SteamBoilerBlockEntity;
import com.vgst54.projectgt.machine.SteamMachineBlockEntity;
import com.vgst54.projectgt.pipe.FluidPipeBlockEntity;
import com.vgst54.projectgt.pipe.ItemPipeBlockEntity;

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

    /** Одна блок-сущность на все предметные трубы. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE =
            TYPES.register("item_pipe", () -> BlockEntityType.Builder.of(
                    ItemPipeBlockEntity::new,
                    ModBlocks.itemPipes().map(pipe -> (Block) pipe.block().get()).toArray(Block[]::new)
            ).build(null));

    /** Одна блок-сущность на все паровые котлы. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamBoilerBlockEntity>> STEAM_BOILER =
            TYPES.register("steam_boiler", () -> BlockEntityType.Builder.of(
                    SteamBoilerBlockEntity::new,
                    ModBlocks.boilers().map(boiler -> (Block) boiler.block().get()).toArray(Block[]::new)
            ).build(null));

    /** Одна блок-сущность на все паровые машины. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamMachineBlockEntity>> STEAM_MACHINE =
            TYPES.register("steam_machine", () -> BlockEntityType.Builder.of(
                    SteamMachineBlockEntity::new,
                    ModBlocks.steamMachines().map(machine -> (Block) machine.block().get()).toArray(Block[]::new)
            ).build(null));

    private ModBlockEntities() {}
}
