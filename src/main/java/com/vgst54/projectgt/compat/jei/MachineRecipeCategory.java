package com.vgst54.projectgt.compat.jei;

import java.util.Arrays;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.machine.recipe.MachineRecipe;
import com.vgst54.projectgt.machine.recipe.MachineType;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Категория JEI для рецептов одной машины: входы, стрелка, выходы и строка с EU/t и временем. */
public class MachineRecipeCategory implements IRecipeCategory<MachineRecipe> {
    private static final int SLOT = 18;
    private static final int HEIGHT = 40;
    private static final int TEXT_COLOR = 0xFF404040;

    private final MachineType machine;
    private final RecipeType<MachineRecipe> recipeType;
    private final IDrawable icon;

    public MachineRecipeCategory(IGuiHelper guiHelper, MachineType machine, ItemStack iconStack) {
        this.machine = machine;
        this.recipeType = RecipeType.create(ProjectGT.MODID, machine.id(), MachineRecipe.class);
        this.icon = guiHelper.createDrawableItemStack(iconStack);
    }

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return Component.literal(machine.displayName());
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 130;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
        int x = 0;
        for (var input : recipe.inputs()) {
            builder.addSlot(RecipeIngredientRole.INPUT, x + 1, 1)
                    .addItemStacks(Arrays.asList(input.getItems()));
            x += SLOT;
        }
        int outX = getWidth() - SLOT * recipe.outputs().size();
        for (ItemStack output : recipe.outputs()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, outX + 1, 1).addItemStack(output.copy());
            outX += SLOT;
        }
    }

    @Override
    public void draw(MachineRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        // Стрелка между входами и выходами
        int arrowX = getWidth() / 2 - 12;
        graphics.fill(arrowX, 8, arrowX + 20, 11, 0xFF8B8B8B);
        graphics.fill(arrowX + 16, 5, arrowX + 17, 14, 0xFF8B8B8B);
        graphics.fill(arrowX + 17, 6, arrowX + 18, 13, 0xFF8B8B8B);
        graphics.fill(arrowX + 18, 7, arrowX + 19, 12, 0xFF8B8B8B);
        graphics.fill(arrowX + 19, 8, arrowX + 20, 11, 0xFF8B8B8B);

        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("jei.projectgt.eut", recipe.eut(), recipe.tier().name()),
                0, 22, TEXT_COLOR, false);
        graphics.drawString(font, Component.translatable("jei.projectgt.duration",
                String.format("%.1f", recipe.duration() / 20.0)), 0, 31, TEXT_COLOR, false);
    }

    @Override
    public ResourceLocation getRegistryName(MachineRecipe recipe) {
        return ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, machine.id());
    }
}
