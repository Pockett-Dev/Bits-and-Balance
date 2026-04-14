package org.onenonly.bitsandbalance.fabric.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.lang.reflect.Constructor;

public final class FabricRecipeSerializerFactory {
    private FabricRecipeSerializerFactory() {
    }

    public static <T extends Recipe<?>> RecipeSerializer<T> create(
            MapCodec<T> codec,
            StreamCodec<RegistryFriendlyByteBuf, T> streamCodec
    ) {
        try {
            Constructor<?> constructor = RecipeSerializer.class.getDeclaredConstructor(MapCodec.class, StreamCodec.class);
            constructor.setAccessible(true);
            @SuppressWarnings("unchecked")
            RecipeSerializer<T> serializer = (RecipeSerializer<T>) constructor.newInstance(codec, streamCodec);
            return serializer;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to construct RecipeSerializer via runtime constructor", exception);
        }
    }
}