package com.vgst54.projectgt.multiblock.part;

import javax.annotation.Nullable;

import com.vgst54.projectgt.multiblock.PartAbility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Люк мультиблока. Способность (PartAbility) определяет, что он даёт: шина ввода, шина вывода или паровой люк.
 * Шины открывают меню на 4 слота, паровой люк показывает содержимое бака.
 */
public class MultiblockPartBlock extends Block implements EntityBlock {
    /** Куда смотрит лицо с отверстием (по любой из шести сторон). */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final PartAbility ability;

    public MultiblockPartBlock(Properties properties, PartAbility ability) {
        super(properties);
        this.ability = ability;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Лицо смотрит на игрока, как у GT (повороты по всем шести сторонам). */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    public PartAbility ability() {
        return ability;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part) {
            if (ability == PartAbility.STEAM) {
                player.displayClientMessage(Component.translatable("gui.projectgt.machine.steam",
                        part.steam().getFluidAmount(), part.steam().getCapacity()), true);
            } else {
                player.openMenu(part, buf -> buf.writeBoolean(ability == PartAbility.STEAM_EXPORT_ITEMS));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part) {
            part.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiblockPartBlockEntity(pos, state);
    }
}
