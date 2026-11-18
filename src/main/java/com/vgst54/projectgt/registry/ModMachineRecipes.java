package com.vgst54.projectgt.registry;

import java.util.EnumMap;
import java.util.Map;

import com.vgst54.projectgt.ProjectGT;
import com.vgst54.projectgt.machine.recipe.MachineRecipe;
import com.vgst54.projectgt.machine.recipe.MachineRecipeSerializer;
import com.vgst54.projectgt.machine.recipe.MachineType;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Типы и сериализаторы рецептов машин: по одному на {@link MachineType}, id совпадает с id вида (projectgt:macerator). */
public final class ModMachineRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ProjectGT.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ProjectGT.MODID);

    private static final Map<MachineType, DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>>> TYPE_BY_MACHINE =
            new EnumMap<>(MachineType.class);
    private static final Map<MachineType, DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer>> SERIALIZER_BY_MACHINE =
            new EnumMap<>(MachineType.class);

    static {
        for (MachineType machine : MachineType.values()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ProjectGT.MODID, machine.id());
            TYPE_BY_MACHINE.put(machine, TYPES.register(machine.id(), () -> RecipeType.<MachineRecipe>simple(id)));
            SERIALIZER_BY_MACHINE.put(machine, SERIALIZERS.register(machine.id(), () -> new MachineRecipeSerializer(machine)));
        }
    }

    private ModMachineRecipes() {}

    public static DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> type(MachineType machine) {
        return TYPE_BY_MACHINE.get(machine);
    }

    public static DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> serializer(MachineType machine) {
        return SERIALIZER_BY_MACHINE.get(machine);
    }
}
