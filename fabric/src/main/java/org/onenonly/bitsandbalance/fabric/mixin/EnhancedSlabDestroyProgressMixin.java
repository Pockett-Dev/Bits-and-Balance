package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
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

        SlabType targetedHalf = getTargetedHalfFromRayToFullCube(player, pos);

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

    private static SlabType getTargetedHalfFromRayToFullCube(Player player, BlockPos pos) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));

        BlockHitResult clipped = Shapes.block().clip(start, end, pos);
        if (clipped != null && clipped.getBlockPos().equals(pos)) {
            return EnhancedSlabHelper.getTargetedHalf(pos, clipped.getLocation());
        }

        double relativeY = player.getEyeY() - pos.getY();
        return relativeY > 0.5 ? SlabType.TOP : SlabType.BOTTOM;
    }
}
