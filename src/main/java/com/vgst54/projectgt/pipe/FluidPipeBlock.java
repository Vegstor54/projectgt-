package com.vgst54.projectgt.pipe;

import java.util.List;

import javax.annotation.Nullable;

import com.vgst54.projectgt.item.WrenchItem;
import com.vgst54.projectgt.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Блок жидкостной трубы. Состояние хранит только то, к каким сторонам тянутся «руки» (для модели);
 * выключенные стороны и направление потока лежат в FluidPipeBlockEntity.
 */
public class FluidPipeBlock extends Block implements EntityBlock {
    private final FluidPipeType type;
    /** Формы по маске соединений (6 бит, порядок Direction.values()). */
    private final VoxelShape[] shapes = new VoxelShape[64];

    public FluidPipeBlock(Properties properties, FluidPipeType type) {
        super(properties);
        this.type = type;

        BlockState state = stateDefinition.any();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), false);
        }
        registerDefaultState(state);

        int t = type.size().thickness();
        double lo = 8 - t / 2.0;
        double hi = 8 + t / 2.0;
        VoxelShape core = Block.box(lo, lo, lo, hi, hi, hi);
        VoxelShape[] arms = new VoxelShape[6];
        arms[Direction.DOWN.ordinal()] = Block.box(lo, 0, lo, hi, lo, hi);
        arms[Direction.UP.ordinal()] = Block.box(lo, hi, lo, hi, 16, hi);
        arms[Direction.NORTH.ordinal()] = Block.box(lo, lo, 0, hi, hi, lo);
        arms[Direction.SOUTH.ordinal()] = Block.box(lo, lo, hi, hi, hi, 16);
        arms[Direction.WEST.ordinal()] = Block.box(0, lo, lo, lo, hi, hi);
        arms[Direction.EAST.ordinal()] = Block.box(hi, lo, lo, 16, hi, hi);
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (int i = 0; i < 6; i++) {
                if ((mask & (1 << i)) != 0) {
                    shape = Shapes.or(shape, arms[i]);
                }
            }
            shapes[mask] = shape;
        }
    }

    public FluidPipeType type() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction dir : Direction.values()) {
            builder.add(PipeBlock.PROPERTY_BY_DIRECTION.get(dir));
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir))) {
                mask |= 1 << dir.ordinal();
            }
        }
        return shapes[mask];
    }

    // --- Соединения ---

    /**
     * Состояние с «руками». Рука торчит, если сторона не выключена ключом и либо там есть сосед
     * с жидкостью, либо сторону открыли ключом вручную (тогда рука торчит и в пустоту).
     */
    private BlockState computeState(Level level, BlockPos pos) {
        int disabled = 0;
        int forced = 0;
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            disabled = pipe.disabledMask();
            forced = pipe.forcedMask();
        }
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            int bit = 1 << dir.ordinal();
            boolean connected = (disabled & bit) == 0
                    && ((forced & bit) != 0
                    || level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(dir), dir.getOpposite()) != null);
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), connected);
        }
        return state;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeState(context.getLevel(), context.getClickedPos());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        BlockState updated = computeState(level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    // --- Ключ ---

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof WrenchItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            Direction side = PipeWrenchGrid.directionAt(
                    hit.getDirection(), local.x, local.y, local.z, type.size().halfWidth());
            Component sideName = Component.literal(side.getName());
            if (player.isShiftKeyDown()) {
                pipe.toggleFlow(side);
                player.displayClientMessage(pipe.flow() == null
                        ? Component.translatable("message.projectgt.pipe.flow_cleared")
                        : Component.translatable("message.projectgt.pipe.flow_set", sideName), true);
            } else {
                boolean extended = state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side));
                boolean enabled = pipe.toggleSide(side, extended);
                player.displayClientMessage(Component.translatable(
                        enabled ? "message.projectgt.pipe.side_enabled" : "message.projectgt.pipe.side_disabled",
                        sideName), true);
                BlockState updated = computeState(level, pos);
                if (updated != state) {
                    level.setBlock(pos, updated, Block.UPDATE_ALL);
                }
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    // --- Блок-сущность ---

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != ModBlockEntities.FLUID_PIPE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((FluidPipeBlockEntity) be).serverTick();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.projectgt.pipe.throughput", type.throughput()));
        tooltip.add(Component.translatable("tooltip.projectgt.pipe.heat_limit", type.heatLimit()));
    }
}
