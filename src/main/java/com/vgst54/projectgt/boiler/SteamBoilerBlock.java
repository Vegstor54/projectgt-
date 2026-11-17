package com.vgst54.projectgt.boiler;

import java.util.List;

import javax.annotation.Nullable;

import com.vgst54.projectgt.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * Блок одноблочного парового котла.
 * ПКМ с ведром/ёмкостью заливает воду (и топливо у жидкостного), с твёрдым топливом кладёт его в слот
 * (угольный), пустой рукой показывает статус.
 */
public class SteamBoilerBlock extends Block implements EntityBlock {
    private final BoilerType type;

    public SteamBoilerBlock(Properties properties, BoilerType type) {
        super(properties);
        this.type = type;
    }

    public BoilerType type() {
        return type;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }
        // Ведро и другие ёмкости: заливка воды/топлива и (если у ёмкости есть место) забор пара.
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof SteamBoilerBlockEntity boiler && type.kind() == BoilerKind.SOLID) {
            ItemStack remainder = boiler.insertFuel(stack);
            if (remainder.getCount() != stack.getCount()) {
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, remainder);
                }
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SteamBoilerBlockEntity boiler) {
            player.displayClientMessage(boiler.status(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SteamBoilerBlockEntity boiler) {
            boiler.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamBoilerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != ModBlockEntities.STEAM_BOILER.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((SteamBoilerBlockEntity) be).serverTick();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.projectgt.boiler.steam_output", type.steamPerTick()));
        tooltip.add(Component.translatable("tooltip.projectgt.boiler.warmup", type.warmupTicks() / 20));
    }
}
