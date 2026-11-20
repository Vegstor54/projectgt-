package com.vgst54.projectgt.compat.jade;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.machine.SteamMachineBlockEntity;
import com.vgst54.projectgt.machine.recipe.MachineRecipe;
import com.vgst54.projectgt.machine.recipe.RecipeProcessor;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

/**
 * Данные паровой машины для Jade. Сервер шлёт id рецепта, прогресс, длительность и параллели,
 * клиент по id находит рецепт и рисует выходы и полосу прогресса.
 */
enum MachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "steam_machine");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof SteamMachineBlockEntity machine)) {
            return;
        }
        data.putBoolean("VentBlocked", machine.needsVenting() && machine.isVentBlocked());
        RecipeProcessor processor = machine.processor();
        ResourceLocation id = processor.recipeId();
        if (id == null) {
            return;
        }
        data.putString("Recipe", id.toString());
        data.putInt("Progress", processor.progress());
        data.putInt("Duration", processor.duration());
        data.putInt("Parallels", processor.parallels());
        data.putBoolean("NoPower", processor.isNoPower());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.getBoolean("VentBlocked")) {
            tooltip.add(Component.translatable("jade.projectgt.vent_blocked").withStyle(ChatFormatting.RED));
        }
        if (!data.contains("Recipe")) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(data.getString("Recipe"));
        if (id == null || accessor.getLevel() == null) {
            return;
        }
        var holder = accessor.getLevel().getRecipeManager().byKey(id);
        if (holder.isEmpty() || !(holder.get() instanceof RecipeHolder<?> h) || !(h.value() instanceof MachineRecipe recipe)) {
            return;
        }
        IElementHelper helper = IElementHelper.get();
        int parallels = Math.max(1, data.getInt("Parallels"));

        tooltip.add(Component.translatable("jade.projectgt.recipe"));
        for (ItemStack output : recipe.outputs()) {
            tooltip.add(helper.smallItem(output));
            tooltip.append(Component.literal(" " + (output.getCount() * parallels) + "x ").append(output.getHoverName()));
        }
        if (parallels > 1) {
            tooltip.add(Component.translatable("jade.projectgt.parallels", parallels));
        }

        int progress = data.getInt("Progress");
        int duration = Math.max(1, data.getInt("Duration"));
        float fraction = Math.min(1f, progress / (float) duration);
        Component text = Component.translatable("jade.projectgt.progress",
                Math.round(fraction * 100), String.format("%.1f", progress / 20.0), String.format("%.1f", duration / 20.0));
        tooltip.add(helper.progress(fraction, text, helper.progressStyle().color(0xFF3FA13F), BoxStyle.getNestedBox(), false));
        if (data.getBoolean("NoPower")) {
            tooltip.add(Component.translatable("jade.projectgt.no_power").withStyle(ChatFormatting.RED));
        }
    }
}
