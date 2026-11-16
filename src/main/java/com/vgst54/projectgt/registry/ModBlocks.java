package com.vgst54.projectgt.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.block.StoneType;
import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.Materials;
import com.vgst54.projectgt.pipe.FluidPipeBlock;
import com.vgst54.projectgt.pipe.FluidPipeMaterials;
import com.vgst54.projectgt.pipe.FluidPipeType;
import com.vgst54.projectgt.pipe.ItemPipeBlock;
import com.vgst54.projectgt.pipe.ItemPipeMaterials;
import com.vgst54.projectgt.pipe.ItemPipeType;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ProjectGT.MODID);

    /** Блок руды: материал × тип камня, вместе с предметом-блоком. */
    public record OreBlock(Material material, StoneType stone, DeferredBlock<Block> block, DeferredItem<BlockItem> item) {
        public String displayName() {
            return stone.namePrefix() + material.displayName() + " Ore";
        }
    }

    /** Жидкостная труба: вид (материал × размер), блок и предмет-блок. */
    public record FluidPipe(FluidPipeType type, DeferredBlock<FluidPipeBlock> block, DeferredItem<BlockItem> item) {}

    /** Предметная труба: вид (материал × размер), блок и предмет-блок. */
    public record ItemPipe(ItemPipeType type, DeferredBlock<ItemPipeBlock> block, DeferredItem<BlockItem> item) {}

    private static final List<ItemPipe> ITEM_PIPES = new ArrayList<>();
    private static final List<OreBlock> ORES = new ArrayList<>();
    private static final List<FluidPipe> FLUID_PIPES = new ArrayList<>();

    static {
        for (Material material : Materials.all()) {
            if (!material.hasOre()) {
                continue;
            }
            for (StoneType stone : StoneType.values()) {
                String id = stone.id() + "_" + material.id() + "_ore";
                DeferredBlock<Block> block = BLOCKS.register(id, () -> new Block(
                        BlockBehaviour.Properties.ofFullCopy(stone.base())
                                .strength(stone.hardness(), 3.0f)
                                .requiresCorrectToolForDrops()));
                DeferredItem<BlockItem> item = ModItems.ITEMS.registerSimpleBlockItem(id, block);
                ORES.add(new OreBlock(material, stone, block, item));
            }
        }
    }

    static {
        for (FluidPipeType type : FluidPipeMaterials.all()) {
            DeferredBlock<FluidPipeBlock> block = BLOCKS.register(type.id(), () -> new FluidPipeBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .noOcclusion(),
                    type));
            DeferredItem<BlockItem> item = ModItems.ITEMS.registerSimpleBlockItem(type.id(), block);
            FLUID_PIPES.add(new FluidPipe(type, block, item));
        }
    }

    static {
        for (ItemPipeType type : ItemPipeMaterials.all()) {
            DeferredBlock<ItemPipeBlock> block = BLOCKS.register(type.id(), () -> new ItemPipeBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .noOcclusion(),
                    type));
            DeferredItem<BlockItem> item = ModItems.ITEMS.registerSimpleBlockItem(type.id(), block);
            ITEM_PIPES.add(new ItemPipe(type, block, item));
        }
    }

    private ModBlocks() {}

    /** Все блоки руды в порядке: материал, затем тип камня. */
    public static Stream<OreBlock> ores() {
        return Collections.unmodifiableList(ORES).stream();
    }

    /** Все предметные трубы в порядке: материал, размер. */
    public static Stream<ItemPipe> itemPipes() {
        return Collections.unmodifiableList(ITEM_PIPES).stream();
    }

    /** Все жидкостные трубы в порядке: материал, затем размер. */
    public static Stream<FluidPipe> fluidPipes() {
        return Collections.unmodifiableList(FLUID_PIPES).stream();
    }
}
