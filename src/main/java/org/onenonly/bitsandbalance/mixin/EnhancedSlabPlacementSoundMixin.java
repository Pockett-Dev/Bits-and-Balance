package org.onenonly.bitsandbalance.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
//? if neoforge
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
//? endif
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabStepPlacementHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Preserve vanilla sound playback/hooks, but give them the source slab for generated Steps. */
@Mixin(BlockItem.class)
public abstract class EnhancedSlabPlacementSoundMixin {
    @ModifyReceiver(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            //? if neoforge
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/level/block/SoundType;", remap = false))
    private BlockState bitsandbalance$sourceSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
            //? else
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private BlockState bitsandbalance$sourceSoundType(BlockState state) {
            //? endif
        return EnhancedSlabStepPlacementHelper.placementSoundState(state);
    }

    @ModifyArg(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            //? if neoforge
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BlockItem;getPlaceSound(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/sounds/SoundEvent;", remap = false),
            //? else
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BlockItem;getPlaceSound(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/sounds/SoundEvent;"),
            //? endif
            index = 0)
    private BlockState bitsandbalance$sourcePlaceSound(BlockState state) {
        return EnhancedSlabStepPlacementHelper.placementSoundState(state);
    }
}
