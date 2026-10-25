package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — Destroy Progress (Fabric)
 *
 * Mining speed for double slabs depends on which half is being targeted.
 * Targets {@link BlockBehaviour} because {@code getDestroyProgress} is declared
 * there (not on {@link net.minecraft.world.level.block.Block}).
 */
@Mixin(BlockBehaviour.class)
public abstract class EnhancedSlabDestroyProgressMixin {

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$getDestroyProgress(BlockState state, Player player,
                                                     BlockGetter level,
                                                     BlockPos pos,
                                                     CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof SlabBlock)) return;
        if (!FabricTweaksConfig.enableEnhancedSlabs) return;
        if (!FabricTweaksConfig.enhancedSlabsKneeSlabMining) return;
        if (!player.isShiftKeyDown()) return; // Only trigger when crouching
        if (player.isCreative()) return;
        if (!EnhancedSlabHelper.isDoubleSlab(state)) return;

        SlabType targetedHalf = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);

        BlockState halfState = EnhancedSlabHelper.getHalfSlabState(state, targetedHalf);

        float hardness = halfState.getDestroySpeed(level, pos);
        if (hardness == -1.0F) {
            cir.setReturnValue(0.0F);
            return;
        }

        boolean correctTool = !halfState.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(halfState);
        float speedFactor = player.getDestroySpeed(halfState);

        if (correctTool) {
            cir.setReturnValue(speedFactor / hardness / 30.0F);
        } else {
            cir.setReturnValue(speedFactor / hardness / 100.0F);
        }
    }

}
