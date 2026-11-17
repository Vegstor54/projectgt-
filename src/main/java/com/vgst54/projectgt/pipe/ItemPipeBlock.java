package com.vgst54.projectgt.pipe;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.vgst54.projectgt.item.WrenchItem;
import com.vgst54.projectgt.registry.ModBlockEntities;

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
 * Блок предметной трубы. Как и у жидкостных, «руки» в состоянии берутся из блок-сущности
 * (открытые стороны), соседи на них не влияют. Автоконнект только при установке с shift к блоку,
 * на который ставят; дальше стороны открывает ключ (клик по стороне рядом с трубой соединяет обе).
 */
public class ItemPipeBlock extends Block implements EntityBlock {
    public static final Map<Direction, BooleanProperty> RESTRICTIVE = new EnumMap<>(Direction.class);

    static {
        for (Direction dir : Direction.values()) {
            RESTRICTIVE.put(dir, BooleanProperty.create("restrictive_" + dir.getName()));
        }
    }

    private final ItemPipeType type;
    private final VoxelShape[] shapes;

    public ItemPipeBlock(Properties properties, ItemPipeType type) {
        super(properties);
        this.type = type;
        BlockState state = stateDefinition.any();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), false);
            state = state.setValue(RESTRICTIVE.get(dir), false);
        }
        registerDefaultState(state);
        this.shapes = PipeShapes.build(type.size().thickness());
    }

    public ItemPipeType type() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction dir : Direction.values()) {
            builder.add(PipeBlock.PROPERTY_BY_DIRECTION.get(dir));
            builder.add(RESTRICTIVE.get(dir));
        }
    }

    /** С ключом в руке труба целится как полный блок (сетка 3×3 на всей грани), без ключа настоящая форма. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (WrenchItem.isHolding(context)) {
            return Shapes.block();
        }
        return shapes[PipeShapes.mask(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[PipeShapes.mask(state)];
    }

    // --- Соединения ---

    private BlockState computeState(Level level, BlockPos pos) {
        ItemPipeBlockEntity pipe = level.getBlockEntity(pos) instanceof ItemPipeBlockEntity itemPipe ? itemPipe : null;
        int open = pipe == null ? 0 : pipe.openMask();
        int restrictive = pipe == null ? 0 : pipe.restrictiveMask();
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), (open & (1 << dir.ordinal())) != 0);
            state = state.setValue(RESTRICTIVE.get(dir), (restrictive & (1 << dir.ordinal())) != 0);
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
     * Автоконнект только с shift и только к блоку, на который ставят трубу. Сосед должен быть
     * предметной трубой или иметь инвентарь, обычные блоки не подключаются.
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
            if (level.getBlockState(neighbor).getBlock() instanceof ItemPipeBlock
                    || level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, clicked) != null) {
                state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(toward), true);
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe)) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (!state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir))) {
                continue;
            }
            pipe.openSide(dir);
            // Сосед тоже труба: открываем и его сторону к нам, иначе предметы не пройдут.
            BlockPos neighborPos = pos.relative(dir);
            if (level.getBlockEntity(neighborPos) instanceof ItemPipeBlockEntity other
                    && level.getBlockState(neighborPos).getBlock() instanceof ItemPipeBlock otherBlock) {
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
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
            Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            Direction side = PipeWrenchGrid.directionAt(hit.getDirection(), local.x, local.y, local.z);
            boolean open;
            if (player.isShiftKeyDown()) {
                boolean restrictive = pipe.toggleRestrictive(side);
                open = true;
                player.displayClientMessage(Component.translatable(
                        restrictive ? "message.projectgt.item_pipe.restrictive_on" : "message.projectgt.item_pipe.restrictive_off",
                        Component.literal(side.getName())), true);
            } else {
                open = pipe.toggleSide(side);
                player.displayClientMessage(Component.translatable(
                        open ? "message.projectgt.pipe.side_enabled" : "message.projectgt.pipe.side_disabled",
                        Component.literal(side.getName())), true);
            }
            // Соседняя труба зеркально открывает или закрывает соединение.
            BlockPos neighborPos = pos.relative(side);
            if (level.getBlockEntity(neighborPos) instanceof ItemPipeBlockEntity other
                    && level.getBlockState(neighborPos).getBlock() instanceof ItemPipeBlock otherBlock) {
                other.setSideOpen(side.getOpposite(), open);
                otherBlock.refreshState(level, neighborPos);
            }
            refreshState(level, pos);
        }
        if (!level.isClientSide) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    // --- Блок-сущность ---

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != ModBlockEntities.ITEM_PIPE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((ItemPipeBlockEntity) be).serverTick();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.projectgt.item_pipe.throughput", type.throughputText()));
        tooltip.add(Component.translatable("tooltip.projectgt.item_pipe.routing_value", type.routingValueText()));
    }
}
