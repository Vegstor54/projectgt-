package com.vgst54.projectgt.multiblock;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.vgst54.projectgt.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Контроллер мультиблока. Лицо смотрит на игрока (только горизонтально), структура строится за ним.
 * Форма берётся из паттерна, свойство FORMED показывает, собрана ли структура.
 */
public class MultiblockControllerBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private final Supplier<MultiblockPattern> pattern;
    private MultiblockPattern built;

    /** Паттерн задаётся поставщиком: блоки корпуса ещё не созданы на момент создания контроллера. */
    public MultiblockControllerBlock(Properties properties, Supplier<MultiblockPattern> pattern) {
        super(properties);
        this.pattern = pattern;
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(FORMED, false));
    }

    public MultiblockPattern pattern() {
        if (built == null) {
            built = pattern.get();
        }
        return built;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MultiblockControllerBlockEntity controller) {
            player.displayClientMessage(controller.recheck().message(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiblockControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.MULTIBLOCK_CONTROLLER.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((MultiblockControllerBlockEntity) be).serverTick();
    }
}
