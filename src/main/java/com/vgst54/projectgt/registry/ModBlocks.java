package com.vgst54.projectgt.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.block.StoneType;
import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.material.Materials;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
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

    private static final List<OreBlock> ORES = new ArrayList<>();

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

    private ModBlocks() {}

    /** Все блоки руды в порядке: материал, затем тип камня. */
    public static Stream<OreBlock> ores() {
        return Collections.unmodifiableList(ORES).stream();
    }
}
