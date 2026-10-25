package org.onenonly.bitsandbalance.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
//? if mc == 1.21.1
import net.minecraft.world.level.Level;
//? else
import net.minecraft.server.level.ServerLevel;
//? endif
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.content.ModKiln;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/** Restricts Kilns to kiln materials and makes those recipes cook twice as fast. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityKilnMixin {
    @ModifyExpressionValue(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    //? if mc == 1.21.1
                    target = "Lnet/minecraft/world/item/crafting/RecipeManager$CachedCheck;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"
                    //? else
                    target = "Lnet/minecraft/world/item/crafting/RecipeManager$CachedCheck;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/server/level/ServerLevel;)Ljava/util/Optional;"
                    //? endif
            )
    )
    private static Optional<? extends RecipeHolder<?>> bitsandbalance$filterKilnRecipes(
            Optional<? extends RecipeHolder<?>> original,
            @Local(argsOnly = true) AbstractFurnaceBlockEntity furnace) {
        if (!ModKiln.isKiln(furnace.getBlockState())) {
            return original;
        }
        return furnace.getItem(0).is(ModKiln.KILN_SMELTABLES) ? original : Optional.empty();
    }

    @ModifyReturnValue(
            //? if mc == 26.3
            method = "getTotalCookTime(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;)I",
            //? else
            method = "getTotalCookTime",
            //? endif
            at = @At("RETURN")
    )
    private static int bitsandbalance$halveKilnCookTime(
            int original,
            //? if mc == 1.21.1
            Level level,
            //? else
            ServerLevel level,
            //? endif
            AbstractFurnaceBlockEntity furnace) {
        return ModKiln.isKiln(furnace.getBlockState()) ? Math.max(1, original / 2) : original;
    }

    // MC 26.x resets the timer directly from the recipe after each completed item.
    @ModifyExpressionValue(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/AbstractCookingRecipe;cookingTime()I"
            ),
            require = 0
    )
    private static int bitsandbalance$halveRepeatedKilnCookTime(
            int original,
            @Local(argsOnly = true) AbstractFurnaceBlockEntity furnace) {
        return ModKiln.isKiln(furnace.getBlockState()) ? Math.max(1, original / 2) : original;
    }
}
