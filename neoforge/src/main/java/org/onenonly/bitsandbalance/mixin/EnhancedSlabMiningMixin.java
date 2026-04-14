package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — KneeSlab-like Mining
 *
 * When mining a double slab in survival mode, only the targeted half (top or bottom)
 * is broken. The remaining half stays as a single slab.
 *
 * The mining speed is determined by the half the player is looking at, meaning:
 *   - An oak+stone double slab: mining the oak half with an axe is fast, with a pickaxe is slow.
 *   - Mining the stone half with a pickaxe is fast, with an axe is slow.
 *
 * This is survival mode only — creative mode still insta-breaks the full block.
 */
@Mixin(Block.class)
public abstract class EnhancedSlabMiningMixin {

    /**
     * Override {@code playerDestroy} (called after a block is broken) for double slabs.
     * Instead of destroying the full block, we convert it to a half slab with only the
     * un-mined portion remaining, and drop the mined half's loot.
     */
    @Inject(method = "playerDestroy", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$playerDestroy(Level level, Player player, BlockPos pos,
                                               BlockState state, BlockEntity blockEntity,
                                               ItemStack tool, CallbackInfo ci) {
        if (!Config.enableEnhancedSlabs) return;
        if (!Config.enhancedSlabsKneeSlabMining) return;
        if (!((Object) this instanceof SlabBlock)) return;
        if (!player.isShiftKeyDown()) return; // Only trigger when crouching
        if (player.isCreative()) return; // Creative mode: full break
        if (!EnhancedSlabHelper.isDoubleSlab(state)) return;
        if (level.isClientSide()) return;

        // Determine which half the player was looking at.
        // Avoid player.pick() here; playerDestroy can run after the block is gone.
        SlabType targetedHalf = getTargetedHalfFromRayToFullCube(player, pos);
        BlockState remainingState = EnhancedSlabHelper.getRemainingHalfState(state, targetedHalf);

        // Drop the mined half's items
        BlockState minedHalfState = EnhancedSlabHelper.getHalfSlabState(state, targetedHalf);
        Block.dropResources(minedHalfState, level, pos, null, player, tool);

        // Set the remaining half instead of air
        level.setBlock(pos, remainingState, Block.UPDATE_ALL);

        // Award stats and exhaust the player as if they broke a block
        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(state.getBlock()));
        player.causeFoodExhaustion(0.005F);

        ci.cancel();
    }

    /**
     * Adjust mining speed for double slabs based on the targeted half.
     * This makes tool effectiveness depend on which slab half the player is looking at.
     *
     * We hook into getDestroyProgress (called every tick during mining) to adjust the
     * effective hardness based on which half is targeted.
     */
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$getDestroyProgress(BlockState state, Player player, 
                                                     net.minecraft.world.level.BlockGetter level,
                                                     BlockPos pos,
                                                     CallbackInfoReturnable<Float> cir) {
        if (!Config.enableEnhancedSlabs) return;
        if (!Config.enhancedSlabsKneeSlabMining) return;
        if (!((Object) this instanceof SlabBlock)) return;
        if (!player.isShiftKeyDown()) return; // Only trigger when crouching
        if (player.isCreative()) return;
        if (!EnhancedSlabHelper.isDoubleSlab(state)) return;

        SlabType targetedHalf = getTargetedHalfFromRayToFullCube(player, pos);

        // Get the state of just the targeted half-slab for destroy speed calculation
        BlockState halfState = EnhancedSlabHelper.getHalfSlabState(state, targetedHalf);

        // Calculate destroy progress using the half-slab's hardness
        float hardness = halfState.getDestroySpeed(level, pos);
        if (hardness == -1.0F) {
            cir.setReturnValue(0.0F);
            return;
        }

        // Check if the player has the correct tool for the half-slab
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
