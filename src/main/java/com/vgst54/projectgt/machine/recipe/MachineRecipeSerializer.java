package com.vgst54.projectgt.machine.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * json рецепта:
 * {"type": "projectgt:forge_hammer", "inputs": [{"ingredient": ..., "count": 3}],
 *  "outputs": [{"id": "minecraft:iron_ingot", "count": 2}], "eut": 16, "duration": 1000}
 * Сериализатор свой для каждого вида машины, поэтому вид знает, какому типу рецептов принадлежит рецепт.
 */
public final class MachineRecipeSerializer implements RecipeSerializer<MachineRecipe> {
    private final MapCodec<MachineRecipe> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec;

    public MachineRecipeSerializer(MachineType machine) {
        this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                SizedIngredient.NESTED_CODEC.listOf().fieldOf("inputs").forGetter(MachineRecipe::inputs),
                ItemStack.CODEC.listOf().fieldOf("outputs").forGetter(MachineRecipe::outputs),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("eut").forGetter(MachineRecipe::eut),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration").forGetter(MachineRecipe::duration)
        ).apply(instance, (inputs, outputs, eut, duration) ->
                new MachineRecipe(machine, inputs, outputs, eut, duration)));
        this.streamCodec = StreamCodec.composite(
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::inputs,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::outputs,
                ByteBufCodecs.VAR_INT, MachineRecipe::eut,
                ByteBufCodecs.VAR_INT, MachineRecipe::duration,
                (inputs, outputs, eut, duration) -> new MachineRecipe(machine, inputs, outputs, eut, duration));
    }

    @Override
    public MapCodec<MachineRecipe> codec() {
        return codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec() {
        return streamCodec;
    }
}
