package com.vgst54.projectgt.compat.jei;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.machine.recipe.MachineRecipe;
import com.vgst54.projectgt.machine.recipe.MachineType;
import com.vgst54.projectgt.registry.ModBlocks;
import com.vgst54.projectgt.registry.ModMachineRecipes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Показывает рецепты паровых машин в JEI. Мод JEI нужен только при разработке, как опциональный. */
@JeiPlugin
public class ProjectGTJeiPlugin implements IModPlugin {
    private final Map<MachineType, MachineRecipeCategory> categories = new HashMap<>();

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        for (MachineType machine : MachineType.values()) {
            ItemStack icon = ModBlocks.steamMachines()
                    .filter(m -> m.type() == machine)
                    .findFirst()
                    .map(m -> new ItemStack(m.item().get()))
                    .orElse(ItemStack.EMPTY);
            MachineRecipeCategory category = new MachineRecipeCategory(registration.getJeiHelpers().getGuiHelper(), machine, icon);
            categories.put(machine, category);
            registration.addRecipeCategories(category);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (MachineType machine : MachineType.values()) {
            List<MachineRecipe> recipes = level.getRecipeManager()
                    .getAllRecipesFor(ModMachineRecipes.type(machine).get())
                    .stream()
                    .map(RecipeHolder::value)
                    .toList();
            registration.addRecipes(categories.get(machine).getRecipeType(), recipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ModBlocks.steamMachines().forEach(m ->
                registration.addRecipeCatalyst(new ItemStack(m.item().get()), categories.get(m.type()).getRecipeType()));
    }
}
