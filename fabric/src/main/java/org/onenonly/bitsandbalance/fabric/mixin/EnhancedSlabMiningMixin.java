package org.onenonly.bitsandbalance.fabric.mixin;

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
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tweaks: Enhanced Slab Behavior — KneeSlab-like Mining (Fabric)
 *
 * In survival mode, only the targeted half of a double slab is broken.
 *
 * Targets Block.class because playerDestroy is inherited from BlockBehaviour
 * and not overridden by SlabBlock; instanceof guards ensure the logic only
 * fires for slab blocks.
 *
 * Mining-speed adjustment (getDestroyProgress) lives in
 * {@link EnhancedSlabDestroyProgressMixin} which targets BlockBehaviour.class
 * where that method is actually declared.
 */
@Mixin(Block.class)
public abstract class EnhancedSlabMiningMixin {

    @Inject(method = "playerDestroy", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$playerDestroy(Level level, Player player, BlockPos pos,
                                               BlockState state, BlockEntity blockEntity,
                                               ItemStack tool, CallbackInfo ci) {
        if (!((Object) this instanceof SlabBlock)) return;
        if (!FabricTweaksConfig.enableEnhancedSlabs) return;
        if (!FabricTweaksConfig.enhancedSlabsKneeSlabMining) return;
        if (player.isCreative()) return;
        if (!EnhancedSlabHelper.isDoubleSlab(state)) return;
        if (level.isClientSide()) return;

        HitResult hitResult = player.pick(player.blockInteractionRange(), 0.0F, false);
        if (!(hitResult instanceof BlockHitResult blockHit)) return;
        if (!blockHit.getBlockPos().equals(pos)) return;

        SlabType targetedHalf = EnhancedSlabHelper.getTargetedHalf(pos, blockHit.getLocation());
        BlockState remainingState = EnhancedSlabHelper.getRemainingHalfState(state, targetedHalf);

        BlockState minedHalfState = EnhancedSlabHelper.getHalfSlabState(state, targetedHalf);
        Block.dropResources(minedHalfState, level, pos, null, player, tool);

        level.setBlock(pos, remainingState, Block.UPDATE_ALL);

        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(state.getBlock()));
        player.causeFoodExhaustion(0.005F);

        ci.cancel();
    }
}
