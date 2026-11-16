package com.vgst54.projectgt.pipe;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Формы труб: ядро + «руки» по маске соединений (6 бит, порядок Direction.values()). */
public final class PipeShapes {
    private PipeShapes() {}

    /** Все 64 формы для трубы данной толщины (в пикселях). */
    public static VoxelShape[] build(int thickness) {
        double lo = 8 - thickness / 2.0;
        double hi = 8 + thickness / 2.0;
        VoxelShape core = Block.box(lo, lo, lo, hi, hi, hi);
        VoxelShape[] arms = new VoxelShape[6];
        arms[Direction.DOWN.ordinal()] = Block.box(lo, 0, lo, hi, lo, hi);
        arms[Direction.UP.ordinal()] = Block.box(lo, hi, lo, hi, 16, hi);
        arms[Direction.NORTH.ordinal()] = Block.box(lo, lo, 0, hi, hi, lo);
        arms[Direction.SOUTH.ordinal()] = Block.box(lo, lo, hi, hi, hi, 16);
        arms[Direction.WEST.ordinal()] = Block.box(0, lo, lo, lo, hi, hi);
        arms[Direction.EAST.ordinal()] = Block.box(hi, lo, lo, 16, hi, hi);
        VoxelShape[] shapes = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (int i = 0; i < 6; i++) {
                if ((mask & (1 << i)) != 0) {
                    shape = Shapes.or(shape, arms[i]);
                }
            }
            shapes[mask] = shape;
        }
        return shapes;
    }

    /** Маска торчащих «рук» из состояния блока. */
    public static int mask(BlockState state) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir))) {
                mask |= 1 << dir.ordinal();
            }
        }
        return mask;
    }
}
