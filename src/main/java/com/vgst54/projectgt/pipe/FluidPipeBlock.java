package com.vgst54.projectgt.pipe;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.vgst54.projectgt.item.WrenchItem;
import com.vgst54.projectgt.registry.ModBlockEntities;
import com.vgst54.projectgt.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
    /** Стрелка (выход) на стороне: только для модели, настоящее состояние лежит в блок-сущности (flowMask). */
    public static final Map<Direction, BooleanProperty> ARROW = new EnumMap<>(Direction.class);

    static {
        for (Direction dir : Direction.values()) {
            ARROW.put(dir, BooleanProperty.create("arrow_" + dir.getName()));
        }
    }

    private final FluidPipeType type;
    /** Формы по маске соединений (6 бит, порядок Direction.values()). */
    private final VoxelShape[] shapes = new VoxelShape[64];

    public FluidPipeBlock(Properties properties, FluidPipeType type) {
        super(properties);
        this.type = type;

        BlockState state = stateDefinition.any();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), false);
            state = state.setValue(ARROW.get(dir), false);
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
            builder.add(ARROW.get(dir));
        }
    }

    /**
     * Контур и цель наведения. С ключом в руке труба «целится» как полный блок, чтобы сетка 3×3
     * была доступна на всей грани, а не только там, где торчит тонкая труба.
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context.isHoldingItem(ModItems.WRENCH.get())) {
            return Shapes.block();
        }
        return pipeShape(state);
    }

    /** Коллизия всегда настоящая, по форме трубы (ключ на неё не влияет). */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return pipeShape(state);
    }

    private VoxelShape pipeShape(BlockState state) {
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
     * Состояние с «руками» целиком берётся из блок-сущности: рука торчит на открытой стороне (openMask),
     * стрелка рисуется там, где она стоит (flowMask). Соседи на «руки» не влияют: автоконнекта нет.
     */
    private BlockState computeState(Level level, BlockPos pos) {
        int open = 0;
        int arrows = 0;
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            open = pipe.openMask();
            arrows = pipe.flowMask();
        }
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            int bit = 1 << dir.ordinal();
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), (open & bit) != 0);
            state = state.setValue(ARROW.get(dir), (arrows & bit) != 0);
        }
        return state;
    }

    /** Пересчитывает состояние блока по блок-сущности и применяет, если изменилось. */
    public void refreshState(Level level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        BlockState updated = computeState(level, pos);
        if (updated != current) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    /**
     * Автоконнект только с shift и только к блоку, на который ставят трубу (грань, по которой кликнули).
     * Сосед должен быть трубой или уметь принимать жидкость, обычные блоки не подключаются.
     * Здесь только отмечаем сторону в состоянии; в блок-сущность она попадёт в setPlacedBy.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            Direction clicked = context.getClickedFace();
            Direction toward = clicked.getOpposite();
            BlockPos neighbor = context.getClickedPos().relative(toward);
            Level level = context.getLevel();
            if (level.getBlockState(neighbor).getBlock() instanceof FluidPipeBlock
                    || level.getCapability(Capabilities.FluidHandler.BLOCK, neighbor, clicked) != null) {
                state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(toward), true);
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe)) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (!state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir))) {
                continue;
            }
            pipe.openSide(dir);
            // Сосед тоже труба: открываем и его сторону к нам, иначе жидкость не пройдёт.
            BlockPos neighborPos = pos.relative(dir);
            if (level.getBlockEntity(neighborPos) instanceof FluidPipeBlockEntity other
                    && level.getBlockState(neighborPos).getBlock() instanceof FluidPipeBlock otherBlock) {
                other.openSide(dir.getOpposite());
                otherBlock.refreshState(level, neighborPos);
            }
        }
        refreshState(level, pos);
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
            Direction side = PipeWrenchGrid.directionAt(hit.getDirection(), local.x, local.y, local.z);
            Component sideName = Component.literal(side.getName());
            if (player.isShiftKeyDown()) {
                boolean arrow = pipe.toggleArrow(side);
                player.displayClientMessage(Component.translatable(
                        arrow ? "message.projectgt.pipe.arrow_on" : "message.projectgt.pipe.arrow_off", sideName), true);
            } else {
                boolean open = pipe.toggleSide(side);
                player.displayClientMessage(Component.translatable(
                        open ? "message.projectgt.pipe.side_enabled" : "message.projectgt.pipe.side_disabled",
                        sideName), true);
            }
            refreshState(level, pos);
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
