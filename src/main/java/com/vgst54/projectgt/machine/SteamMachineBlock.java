package com.vgst54.projectgt.machine;

import javax.annotation.Nullable;

import com.vgst54.projectgt.machine.recipe.MachineType;
import com.vgst54.projectgt.registry.ModBlockEntities;

import com.vgst54.projectgt.item.WrenchItem;
import com.vgst54.projectgt.pipe.PipeWrenchGrid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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
 * Одноблочная паровая машина: вид (дробитель, экстрактор, компрессор, кузнечный молот) × давление.
 * Лицевая сторона смотрит на игрока при установке; пока идёт рецепт, лицо анимированное. ПКМ открывает меню.
 */
public class SteamMachineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Сторона вывода предметов (и вытяжки пара): по умолчанию сзади, меняется ключом. */
    public static final DirectionProperty OUTPUT = DirectionProperty.create("output", net.minecraft.core.Direction.values());
    /** Машина работает: лицо рисуется анимированным (оверлей _active). */
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

    private final MachineType machine;
    private final SteamPressure pressure;

    public SteamMachineBlock(Properties properties, MachineType machine, SteamPressure pressure) {
        super(properties);
        this.machine = machine;
        this.pressure = pressure;
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(ACTIVE, false)
                .setValue(OUTPUT, net.minecraft.core.Direction.SOUTH));
    }

    public MachineType machine() {
        return machine;
    }

    public SteamPressure pressure() {
        return pressure;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, OUTPUT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing).setValue(OUTPUT, facing.getOpposite());
    }

    /**
     * Ключ: ПКМ по клетке сетки задаёт сторону вывода (не лицо), ПКМ с sneak по клетке задаёт лицо (по горизонтали).
     * Сторона под курсором определяется так же, как на трубах (PipeWrenchGrid).
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof WrenchItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }
        if (player.isShiftKeyDown()) {
            // Shift: клетка сетки задаёт, куда смотрит лицо (только по горизонтали). Если там сторона вывода,
            // вывод уходит на прежнюю сторону лица, чтобы лицо и вывод не совпали.
            Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            Direction facing = PipeWrenchGrid.directionAt(hit.getDirection(), local.x, local.y, local.z);
            if (!facing.getAxis().isHorizontal()) {
                player.displayClientMessage(Component.translatable("message.projectgt.machine.face_horizontal"), true);
                return ItemInteractionResult.SUCCESS;
            }
            Direction output = state.getValue(OUTPUT);
            if (output == facing) {
                output = state.getValue(FACING);
            }
            level.setBlock(pos, state.setValue(FACING, facing).setValue(OUTPUT, output), Block.UPDATE_ALL);
            player.displayClientMessage(Component.translatable("message.projectgt.machine.rotated", facing.getName()), true);
        } else {
            Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            Direction side = PipeWrenchGrid.directionAt(hit.getDirection(), local.x, local.y, local.z);
            if (side == state.getValue(FACING)) {
                player.displayClientMessage(Component.translatable("message.projectgt.machine.output_not_front"), true);
                return ItemInteractionResult.SUCCESS;
            }
            level.setBlock(pos, state.setValue(OUTPUT, side), Block.UPDATE_ALL);
            player.displayClientMessage(Component.translatable("message.projectgt.machine.output_side", side.getName()), true);
        }
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SteamMachineBlockEntity machineEntity) {
            player.openMenu(machineEntity, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SteamMachineBlockEntity machineEntity) {
            machineEntity.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != ModBlockEntities.STEAM_MACHINE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((SteamMachineBlockEntity) be).serverTick();
    }
}
