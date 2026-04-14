package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — Quad Step Anti-Flicker (NeoForge)
 *
 * Prevents the brief AIR flash when mining one quadrant out of a {@link QuadStepBlock}
 * in crouch mode. Instead of removing the block entirely, we replace it with the
 * remaining quadrants before vanilla's drop logic runs.
 */
@Mixin(net.minecraft.server.level.ServerPlayerGameMode.class)
public abstract class StepDestroyBlockMixin {

    @Shadow @Final protected ServerPlayer player;
    @Shadow protected ServerLevel level;
    @Unique private boolean bitsandbalance$needsPostDestroyQuadSync;
    @Unique private long bitsandbalance$needsPostDestroyQuadSyncPosLong;

    @Unique
    private static void bitsandbalance$forceBreakerQuadRefresh(ServerLevel level, ServerPlayer breaker, long posLong) {
        if (level == null || breaker == null || breaker.connection == null) return;
        try {
            BlockPos p = BlockPos.of(posLong);
            BlockState stateNow = level.getBlockState(p);
            try {
                breaker.connection.send(new ClientboundBlockUpdatePacket(p, stateNow));
            } catch (Throwable ignored) { }
            BlockEntity beNow = level.getBlockEntity(p);
            if (beNow instanceof QuadStepBlockEntity
                    || beNow instanceof QuadVerticalStepBlockEntity
                    || beNow instanceof StepBlockEntity
                    || beNow instanceof VerticalStepBlockEntity) {
                Packet<ClientGamePacketListener> pkt = beNow.getUpdatePacket();
                if (pkt != null) {
                    try { breaker.connection.send(pkt); } catch (Throwable ignored) { }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void bitsandbalance$resetPostDestroyQuadSync(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        bitsandbalance$needsPostDestroyQuadSync = false;
        bitsandbalance$needsPostDestroyQuadSyncPosLong = 0L;
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void bitsandbalance$postDestroyQuadSync(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!bitsandbalance$needsPostDestroyQuadSync) return;
        if (pos == null || pos.asLong() != bitsandbalance$needsPostDestroyQuadSyncPosLong) return;
        final long posLong = bitsandbalance$needsPostDestroyQuadSyncPosLong;
        final ServerLevel capturedLevel = this.level;
        final ServerPlayer breaker = this.player;

        bitsandbalance$forceBreakerQuadRefresh(capturedLevel, breaker, posLong);

        try {
            var server = capturedLevel.getServer();
            if (server == null) return;
            server.execute(new net.minecraft.server.TickTask(1, () -> {
                try {
                    bitsandbalance$forceBreakerQuadRefresh(capturedLevel, breaker, posLong);
                } catch (Throwable ignored) {
                }
            }));
        } catch (Throwable ignored) {
        }
    }

    @Redirect(
            method = "destroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"
            )
    )
    private boolean bitsandbalance$quadrantBreakRedirect(ServerLevel level, BlockPos pos, boolean move) {
        if (!Config.enableEnhancedSlabs) return level.removeBlock(pos, move);
        if (!Config.enhancedSlabsSteps) return level.removeBlock(pos, move);

        BlockState current = level.getBlockState(pos);

        if (current.getBlock() instanceof QuadStepBlock) {
            BlockState mined = QuadStepBlock.bitsandbalance$peekCachedMinedSlab(pos);
            BlockState[] remaining = QuadStepBlock.bitsandbalance$peekCachedRemainingSlabs(pos);
            int removedCount = QuadStepBlock.bitsandbalance$peekCachedRemovedCount(pos);
            if (mined == null || remaining == null) return level.removeBlock(pos, move);
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof QuadStepBlockEntity quad)) return level.removeBlock(pos, move);

            int remainingCount = 0;
            for (BlockState slab : remaining) {
                if (slab != null) remainingCount++;
            }

            if (remainingCount <= 1) {
                QuadStepBlock.CachedBreak cached = new QuadStepBlock.CachedBreak();
                cached.posLong = pos.asLong();
                cached.minedSlab = mined;
                cached.remainingSlabs = remaining;
                cached.waterlogged = current.getValue(QuadStepBlock.WATERLOGGED);
                cached.axis = current.getValue(QuadStepBlock.AXIS);
                cached.removedCount = removedCount;
                QuadStepBlock.bitsandbalance$restoreRemaining(level, pos, cached);
                bitsandbalance$needsPostDestroyQuadSync = true;
                bitsandbalance$needsPostDestroyQuadSyncPosLong = pos.asLong();
            } else {
                quad.bitsandbalance$setAllSlabsSilent(remaining);
                bitsandbalance$needsPostDestroyQuadSync = true;
                bitsandbalance$needsPostDestroyQuadSyncPosLong = pos.asLong();
            }

            if (!player.isCreative()) {
                ItemStack tool = player.getMainHandItem();
                try { tool.mineBlock(level, mined, pos, player); } catch (Throwable ignored) { }

                boolean correctTool = !mined.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(mined);
                if (correctTool) {
                    for (int i = 0; i < Math.max(1, removedCount); i++) {
                        ItemStack drop = StepBlock.bitsandbalance$makeStepDropItem(mined);
                        if (!drop.isEmpty()) Block.popResource(level, pos, drop);
                    }
                }
                player.awardStat(Stats.BLOCK_MINED.get(current.getBlock()));
                player.causeFoodExhaustion(0.005F);
            }

            QuadStepBlock.bitsandbalance$clearCachedBreak();
            return false;
        }

        if (current.getBlock() instanceof QuadVerticalStepBlock) {
            BlockState mined = QuadVerticalStepBlock.bitsandbalance$peekCachedMinedSlab(pos);
            BlockState[] remaining = QuadVerticalStepBlock.bitsandbalance$peekCachedRemainingSlabs(pos);
            int removedCount = QuadVerticalStepBlock.bitsandbalance$peekCachedRemovedCount(pos);
            if (mined == null || remaining == null) return level.removeBlock(pos, move);
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof QuadVerticalStepBlockEntity quad)) return level.removeBlock(pos, move);

            int remainingCount = 0;
            for (BlockState slab : remaining) {
                if (slab != null) remainingCount++;
            }

            if (remainingCount <= 1) {
                QuadVerticalStepBlock.CachedBreak cached = new QuadVerticalStepBlock.CachedBreak();
                cached.posLong = pos.asLong();
                cached.minedSlab = mined;
                cached.remainingSlabs = remaining;
                cached.waterlogged = current.getValue(QuadVerticalStepBlock.WATERLOGGED);
                cached.removedCount = removedCount;
                QuadVerticalStepBlock.bitsandbalance$restoreRemaining(level, pos, cached);
                bitsandbalance$needsPostDestroyQuadSync = true;
                bitsandbalance$needsPostDestroyQuadSyncPosLong = pos.asLong();
            } else {
                quad.bitsandbalance$setAllSlabsSilent(remaining);
                bitsandbalance$needsPostDestroyQuadSync = true;
                bitsandbalance$needsPostDestroyQuadSyncPosLong = pos.asLong();
            }

            if (!player.isCreative()) {
                ItemStack tool = player.getMainHandItem();
                try { tool.mineBlock(level, mined, pos, player); } catch (Throwable ignored) { }

                boolean correctTool = !mined.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(mined);
                if (correctTool) {
                    for (int i = 0; i < Math.max(1, removedCount); i++) {
                        ItemStack drop = VerticalStepBlock.bitsandbalance$makeVerticalStepDropItem(mined);
                        if (!drop.isEmpty()) Block.popResource(level, pos, drop);
                    }
                }
                player.awardStat(Stats.BLOCK_MINED.get(current.getBlock()));
                player.causeFoodExhaustion(0.005F);
            }

            QuadVerticalStepBlock.bitsandbalance$clearCachedBreak();
            return false;
        }

        return level.removeBlock(pos, move);
    }
}

