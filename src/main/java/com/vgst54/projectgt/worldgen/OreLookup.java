package com.vgst54.projectgt.worldgen;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.vgst54.projectgt.block.StoneType;
import com.vgst54.projectgt.material.Material;
import com.vgst54.projectgt.registry.ModBlocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Подбирает блок руды (материал x тип камня) по вмещающей породе. Только породы Верхнего мира. */
final class OreLookup {
    private static final Set<StoneType> OVERWORLD = Set.of(
            StoneType.STONE, StoneType.DEEPSLATE, StoneType.GRANITE,
            StoneType.DIORITE, StoneType.ANDESITE, StoneType.TUFF);

    // материал.id -> (ванильный камень -> наш блок руды)
    private static volatile Map<String, Map<Block, Block>> table;

    /** Блок руды для этой породы или null, если порода не подходит. */
    static Block oreFor(Material material, BlockState host) {
        Map<Block, Block> byStone = table().get(material.id());
        return byStone == null ? null : byStone.get(host.getBlock());
    }

    private static Map<String, Map<Block, Block>> table() {
        Map<String, Map<Block, Block>> t = table;
        if (t == null) {
            synchronized (OreLookup.class) {
                t = table;
                if (t == null) {
                    Map<String, Map<Block, Block>> built = new HashMap<>();
                    ModBlocks.ores()
                            .filter(o -> OVERWORLD.contains(o.stone()))
                            .forEach(o -> built
                                    .computeIfAbsent(o.material().id(), k -> new HashMap<>())
                                    .put(o.stone().base(), o.block().get()));
                    table = t = built;
                }
            }
        }
        return t;
    }

    private OreLookup() {}
}
