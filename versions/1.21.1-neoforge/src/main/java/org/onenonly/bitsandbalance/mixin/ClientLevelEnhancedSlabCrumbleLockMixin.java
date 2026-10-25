package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabCrackProxyBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCrumbleState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
            BlockState[] stored = EnhancedSlabMaterialSavedData.getClient(level, pos.asLong());
            locked = targetedHalf == SlabType.TOP ? bitsandbalance$slot(stored, 1) : bitsandbalance$slot(stored, 0);
            if (locked != null && locked.hasProperty(SlabBlock.TYPE)) {
                locked = locked.setValue(SlabBlock.TYPE, targetedHalf);
            }
        } else if (Config.enhancedSlabsVerticalSlabs && state.getBlock() instanceof VerticalSlabBlock) {
            locked = bitsandbalance$lockedVerticalSlabCrack(level, player, pos, state);
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

    @Unique
    private static BlockState bitsandbalance$lockedVerticalSlabCrack(ClientLevel level,
                                                                      Player player,
                                                                      BlockPos pos,
                                                                      BlockState state) {
        if (player == null || !state.getValue(VerticalSlabBlock.DOUBLE)) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(pos);
        VerticalSlabBlockEntity verticalSlab = be instanceof VerticalSlabBlockEntity vbe ? vbe : null;
        BlockState[] stored = EnhancedSlabMaterialSavedData.getClient(level, pos.asLong());
        if (!VerticalSlabBlock.bitsandbalance$shouldTargetHalf(player, state, stored == null ? verticalSlab : null, stored)) {
            return null;
        }

        Direction facing = state.getValue(VerticalSlabBlock.FACING);
        Direction targeted = VerticalSlabBlock.bitsandbalance$getTargetedHalf(player, pos, facing);
        VerticalSlabCrackProxyBlock proxy = CommonBlocks.VERTICAL_SLAB_CRACK_PROXY;
        if (proxy == null) {
            return null;
        }
        return proxy.defaultBlockState().setValue(VerticalSlabCrackProxyBlock.FACING, targeted);
    }

    private static BlockState bitsandbalance$slot(BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }
}
