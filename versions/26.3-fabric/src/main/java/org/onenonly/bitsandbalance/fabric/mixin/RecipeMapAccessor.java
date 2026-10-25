package org.onenonly.bitsandbalance.fabric.mixin;

import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(RecipeMap.class)
public interface RecipeMapAccessor {
    @Mutable
    @Accessor("byType")
    void bitsandbalance$setByType(Multimap<RecipeType<?>, RecipeHolder<?>> byType);

    @Mutable
    @Accessor("byKey")
    void bitsandbalance$setByKey(Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey);
}
