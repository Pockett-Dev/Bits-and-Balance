package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCrumbleState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Locks the slab-half used for the crumbling overlay for the duration of mining.
 *
 * Without this, per-frame targeted-half selection can flap (especially near the 0.5 seam),
 * causing the destroy-stage overlay to appear to flicker.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelEnhancedSlabCrumbleLockMixin {

    @Inject(method = "destroyBlockProgress", at = @At("HEAD"))
    private void bitsandbalance$lockHalfForCrumbling(int entityId, BlockPos pos, int progress, CallbackInfo ci) {
        if (!Config.enableEnhancedSlabs) return;

        if (progress < 0) {
            EnhancedSlabCrumbleState.clear(pos);
            return;
        }

        if (EnhancedSlabCrumbleState.has(pos)) {
            return;
        }

        ClientLevel level = (ClientLevel) (Object) this;
        BlockState state = level.getBlockState(pos);
        if (state == null || state.isAir()) {
            EnhancedSlabCrumbleState.clear(pos);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        SlabType targetedHalf = SlabType.BOTTOM;
        if (player != null && minecraft.hitResult instanceof BlockHitResult bhr && pos.equals(bhr.getBlockPos())) {
            targetedHalf = EnhancedSlabHelper.getTargetedHalf(pos, bhr.getLocation());
        } else if (player != null) {
            targetedHalf = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);
        }

        BlockState locked = null;

        if (Config.enhancedSlabsMixedDoubleSlabs && state.getBlock() instanceof MixedSlabBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MixedSlabBlockEntity mixedBe) {
                locked = (targetedHalf == SlabType.TOP) ? mixedBe.getTopSlab() : mixedBe.getBottomSlab();
            }
        } else if (Config.enhancedSlabsKneeSlabMining
                && player != null && player.isShiftKeyDown()
                && state.getBlock() instanceof SlabBlock
                && EnhancedSlabHelper.isDoubleSlab(state)) {
            locked = EnhancedSlabHelper.getHalfSlabState(state, targetedHalf);
        }

        if (locked != null) {
            EnhancedSlabCrumbleState.set(pos, locked);
        }
    }
}
