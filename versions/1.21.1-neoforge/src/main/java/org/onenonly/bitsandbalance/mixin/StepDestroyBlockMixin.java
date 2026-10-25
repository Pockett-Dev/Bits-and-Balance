package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.onenonly.bitsandbalance.Config;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — Quad Step Anti-Flicker (Fabric)
 *
 * Prevents the brief AIR flash when crouching-mining one quadrant out of a
 * {@link QuadStepBlock}. Restores remaining quadrants before the vanilla drop
 * logic runs.
 */
@Mixin(net.minecraft.server.level.ServerPlayerGameMode.class)
public abstract class StepDestroyBlockMixin {

    @Shadow @Final protected ServerPlayer player;
    @Shadow protected ServerLevel level;

    @Unique private boolean bitsandbalance$needsPostDestroyQuadSync;
    @Unique private long bitsandbalance$needsPostDestroyQuadSyncPosLong;
    @Unique private @Nullable BlockPos bitsandbalance$lockedSableStepPos;
    @Unique private int bitsandbalance$lockedSableStepIndex = -1;
    @Unique private boolean bitsandbalance$lockedSableStepTargetedOnly;
    @Unique private @Nullable BlockPos bitsandbalance$lockedSableVerticalStepPos;
    @Unique private int bitsandbalance$lockedSableVerticalStepIndex = -1;
    @Unique private boolean bitsandbalance$lockedSableVerticalStepTargetedOnly;

    /**
     * Locks the virtual Step quadrant selected when mining begins. A Sable body may move or rotate
     * while destroy progress accumulates, so resolving the part again only when
     * {@code destroyBlock} runs can target a different quadrant than the one the player mined.
     */
    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void bitsandbalance$lockSableStepPart(BlockPos pos,
                                                  ServerboundPlayerActionPacket.Action action,
                                                  Direction direction,
                                                  int maxBuildHeight,
                                                  int sequence,
                                                  org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
            bitsandbalance$clearLockedSableStepPart();
            bitsandbalance$clearLockedSableVerticalStepPart();
            return;
        }
        if (action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            return;
        }

        bitsandbalance$clearLockedSableStepPart();
        bitsandbalance$clearLockedSableVerticalStepPart();
        if (!Config.isStepRuntimeEnabled() || pos == null || level == null || player == null) {
            return;
        }

        BlockState plotState = SableCompat.plotBlockState(level, pos);
        if (plotState == null) {
            return;
        }

        BlockState[] slots = EnhancedSlabCreateCompat.captureMaterialSlots(level, pos, plotState);
        if (!bitsandbalance$hasAnySlot(slots)) {
            return;
        }

        if (plotState.getBlock() instanceof QuadStepBlock) {
            Direction.Axis axis = plotState.hasProperty(QuadStepBlock.AXIS)
                    ? plotState.getValue(QuadStepBlock.AXIS)
                    : Direction.Axis.X;
            int targetedIndex = bitsandbalance$resolveStepIndex(
                    slots,
                    QuadStepBlock.bitsandbalance$getTargetedIndex(player, pos, axis)
            );
            if (bitsandbalance$slot(slots, targetedIndex) != null) {
                bitsandbalance$lockedSableStepPos = pos.immutable();
                bitsandbalance$lockedSableStepIndex = targetedIndex;
                // Sable exposes an individual KneeSlab part as the block being mined even when
                // equal-material parts share one QuadStep owner. Preserve that virtual target:
                // batching all matching slots here can remove the owner's remaining support and
                // split the physics object when the player only broke one visible Step.
                bitsandbalance$lockedSableStepTargetedOnly = true;
            }
        } else if (plotState.getBlock() instanceof QuadVerticalStepBlock) {
            int targetedIndex = bitsandbalance$resolveVerticalStepIndex(
                    slots,
                    QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, pos)
            );
            if (bitsandbalance$slot(slots, targetedIndex) != null) {
                bitsandbalance$lockedSableVerticalStepPos = pos.immutable();
                bitsandbalance$lockedSableVerticalStepIndex = targetedIndex;
                bitsandbalance$lockedSableVerticalStepTargetedOnly = true;
            }
        }
    }

    @Unique
    private static void bitsandbalance$forceBreakerQuadRefresh(ServerLevel level, ServerPlayer breaker, long posLong) {
        if (level == null || breaker == null || breaker.connection == null) return;
        try {
            BlockPos p = BlockPos.of(posLong);
            BlockState stateNow = SableCompat.plotBlockState(level, p);
            if (stateNow == null) {
                stateNow = level.getBlockState(p);
            }
            try {
                breaker.connection.send(new ClientboundBlockUpdatePacket(p, stateNow));
            } catch (Throwable ignored) { }
            BlockEntity beNow = SableCompat.plotBlockEntity(level, p);
            if (beNow == null) {
                beNow = level.getBlockEntity(p);
            }
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

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$resetPostDestroyQuadSync(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        bitsandbalance$needsPostDestroyQuadSync = false;
        bitsandbalance$needsPostDestroyQuadSyncPosLong = 0L;
        bitsandbalance$trySablePlotQuadrantBreak(pos, cir);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void bitsandbalance$postDestroyQuadSync(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        bitsandbalance$clearLockedSableStepPart(pos);
        bitsandbalance$clearLockedSableVerticalStepPart(pos);
        if (!bitsandbalance$needsPostDestroyQuadSync) return;
        if (pos == null || pos.asLong() != bitsandbalance$needsPostDestroyQuadSyncPosLong) return;
        final long posLong = bitsandbalance$needsPostDestroyQuadSyncPosLong;
        final ServerLevel capturedLevel = this.level;
        final ServerPlayer breaker = this.player;

        // Do an immediate breaker refresh now that vanilla's destroyBlock finished.
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

    @Inject(method = "removeBlock", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$quadrantBreakRedirect(BlockPos pos, BlockState state, boolean canHarvest, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.isStepRuntimeEnabled()) return;

        BlockState current = SableCompat.plotBlockState(level, pos);
        if (current == null) {
            current = level.getBlockState(pos);
        }

        if (current.getBlock() instanceof QuadStepBlock) {
            BlockState mined = QuadStepBlock.bitsandbalance$peekCachedMinedSlab(pos);
            BlockState[] remaining = QuadStepBlock.bitsandbalance$peekCachedRemainingSlabs(pos);
            int removedCount = QuadStepBlock.bitsandbalance$peekCachedRemovedCount(pos);
            if (mined == null || remaining == null) {
                QuadStepBlock.CachedBreak fallback = bitsandbalance$buildStepCachedBreak(level, player, pos, current);
                if (fallback == null) return;
                mined = fallback.minedSlab;
                remaining = fallback.remainingSlabs;
                removedCount = fallback.removedCount;
            }

            BlockEntity be = SableCompat.plotBlockEntity(level, pos);
            if (be == null) {
                be = level.getBlockEntity(pos);
            }
            QuadStepBlockEntity quad = be instanceof QuadStepBlockEntity q ? q : null;

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
                if (!SableCompat.rememberPlotEnhancedSlabMaterials(level, pos, current, remaining, Block.UPDATE_ALL)) {
                    EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), remaining);
                    if (quad != null) {
                        quad.bitsandbalance$setAllSlabsSilent(remaining);
                        quad.bitsandbalance$syncToClients();
                    }
                }
                level.sendBlockUpdated(pos, current, current, Block.UPDATE_ALL);
                bitsandbalance$needsPostDestroyQuadSync = true;
                bitsandbalance$needsPostDestroyQuadSyncPosLong = pos.asLong();
            }
            SableCompat.notifyEnhancedSlabPartialRemoval(level, pos);

            // Drops/stats for crouch mining
            if (!player.isCreative()) {
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
            cir.setReturnValue(false);
            return;
        }

        if (current.getBlock() instanceof QuadVerticalStepBlock) {
            BlockState mined = QuadVerticalStepBlock.bitsandbalance$peekCachedMinedSlab(pos);
            BlockState[] remaining = QuadVerticalStepBlock.bitsandbalance$peekCachedRemainingSlabs(pos);
            int removedCount = QuadVerticalStepBlock.bitsandbalance$peekCachedRemovedCount(pos);
            if (mined == null || remaining == null) {
                QuadVerticalStepBlock.CachedBreak fallback = bitsandbalance$buildVerticalStepCachedBreak(level, player, pos, current);
                if (fallback == null) return;
                mined = fallback.minedSlab;
                remaining = fallback.remainingSlabs;
                removedCount = fallback.removedCount;
            }

            BlockEntity be = SableCompat.plotBlockEntity(level, pos);
            if (be == null) {
                be = level.getBlockEntity(pos);
            }
            QuadVerticalStepBlockEntity quad = be instanceof QuadVerticalStepBlockEntity q ? q : null;

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
                if (!SableCompat.rememberPlotEnhancedSlabMaterials(level, pos, current, remaining, Block.UPDATE_ALL)) {
                    EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), remaining);
                    if (quad != null) {
                        quad.bitsandbalance$setAllSlabsSilent(remaining);
                        quad.bitsandbalance$syncToClients();
                    }
                }
                level.sendBlockUpdated(pos, current, current, Block.UPDATE_ALL);
                bitsandbalance$needsPostDestroyQuadSync = true;
                bitsandbalance$needsPostDestroyQuadSyncPosLong = pos.asLong();
            }
            SableCompat.notifyEnhancedSlabPartialRemoval(level, pos);

            if (!player.isCreative()) {
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
            cir.setReturnValue(false);
            return;
        }
    }

    @Unique
    private void bitsandbalance$trySablePlotQuadrantBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.isStepRuntimeEnabled() || pos == null || level == null || player == null) {
            return;
        }

        BlockState current = SableCompat.plotBlockState(level, pos);
        if (current == null) {
            return;
        }

        if (current.getBlock() instanceof QuadStepBlock) {
            QuadStepBlock.CachedBreak cached = bitsandbalance$buildStepCachedBreak(level, player, pos, current);
            if (cached == null) {
                return;
            }
            bitsandbalance$applySablePlotStepBreak(pos, current, cached);
            QuadStepBlock.bitsandbalance$clearCachedBreak();
            bitsandbalance$clearLockedSableStepPart(pos);
            cir.setReturnValue(true);
            return;
        }

        if (current.getBlock() instanceof QuadVerticalStepBlock) {
            QuadVerticalStepBlock.CachedBreak cached = bitsandbalance$buildVerticalStepCachedBreak(level, player, pos, current);
            if (cached == null) {
                return;
            }
            bitsandbalance$applySablePlotVerticalStepBreak(pos, current, cached);
            QuadVerticalStepBlock.bitsandbalance$clearCachedBreak();
            bitsandbalance$clearLockedSableVerticalStepPart(pos);
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void bitsandbalance$applySablePlotStepBreak(BlockPos pos,
                                                        BlockState current,
                                                        QuadStepBlock.CachedBreak cached) {
        int remainingCount = bitsandbalance$countSlots(cached.remainingSlabs);
        if (remainingCount <= 1) {
            QuadStepBlock.bitsandbalance$restoreRemaining(level, pos, cached);
        } else if (!SableCompat.rememberPlotEnhancedSlabMaterials(level, pos, current, cached.remainingSlabs, Block.UPDATE_ALL)) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), cached.remainingSlabs);
            BlockEntity be = SableCompat.plotBlockEntity(level, pos);
            if (be instanceof QuadStepBlockEntity quad) {
                quad.bitsandbalance$setAllSlabsSilent(cached.remainingSlabs);
                quad.bitsandbalance$syncToClients();
            }
        }
        level.sendBlockUpdated(pos, current, SableCompat.plotBlockState(level, pos) == null ? current : SableCompat.plotBlockState(level, pos), Block.UPDATE_ALL);
        SableCompat.notifyEnhancedSlabPartialRemoval(level, pos);
        bitsandbalance$dropStepPartialBreak(pos, current, cached.minedSlab, cached.removedCount);
        bitsandbalance$schedulePostDestroyQuadSync(pos);
    }

    @Unique
    private void bitsandbalance$applySablePlotVerticalStepBreak(BlockPos pos,
                                                                BlockState current,
                                                                QuadVerticalStepBlock.CachedBreak cached) {
        int remainingCount = bitsandbalance$countSlots(cached.remainingSlabs);
        if (remainingCount <= 1) {
            QuadVerticalStepBlock.bitsandbalance$restoreRemaining(level, pos, cached);
        } else if (!SableCompat.rememberPlotEnhancedSlabMaterials(level, pos, current, cached.remainingSlabs, Block.UPDATE_ALL)) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), cached.remainingSlabs);
            BlockEntity be = SableCompat.plotBlockEntity(level, pos);
            if (be instanceof QuadVerticalStepBlockEntity quad) {
                quad.bitsandbalance$setAllSlabsSilent(cached.remainingSlabs);
                quad.bitsandbalance$syncToClients();
            }
        }
        level.sendBlockUpdated(pos, current, SableCompat.plotBlockState(level, pos) == null ? current : SableCompat.plotBlockState(level, pos), Block.UPDATE_ALL);
        SableCompat.notifyEnhancedSlabPartialRemoval(level, pos);
        bitsandbalance$dropVerticalStepPartialBreak(pos, current, cached.minedSlab, cached.removedCount);
        bitsandbalance$schedulePostDestroyQuadSync(pos);
    }

    @Unique
    private void bitsandbalance$dropStepPartialBreak(BlockPos pos,
                                                     BlockState current,
                                                     @Nullable BlockState mined,
                                                     int removedCount) {
        if (player.isCreative() || mined == null) {
            return;
        }
        boolean correctTool = !mined.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(mined);
        if (correctTool) {
            for (int i = 0; i < Math.max(1, removedCount); i++) {
                ItemStack drop = StepBlock.bitsandbalance$makeStepDropItem(mined);
                if (!drop.isEmpty()) {
                    Block.popResource(level, pos, drop);
                }
            }
        }
        player.awardStat(Stats.BLOCK_MINED.get(current.getBlock()));
        player.causeFoodExhaustion(0.005F);
    }

    @Unique
    private void bitsandbalance$dropVerticalStepPartialBreak(BlockPos pos,
                                                             BlockState current,
                                                             @Nullable BlockState mined,
                                                             int removedCount) {
        if (player.isCreative() || mined == null) {
            return;
        }
        boolean correctTool = !mined.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(mined);
        if (correctTool) {
            for (int i = 0; i < Math.max(1, removedCount); i++) {
                ItemStack drop = VerticalStepBlock.bitsandbalance$makeVerticalStepDropItem(mined);
                if (!drop.isEmpty()) {
                    Block.popResource(level, pos, drop);
                }
            }
        }
        player.awardStat(Stats.BLOCK_MINED.get(current.getBlock()));
        player.causeFoodExhaustion(0.005F);
    }

    @Unique
    private void bitsandbalance$schedulePostDestroyQuadSync(BlockPos pos) {
        bitsandbalance$needsPostDestroyQuadSync = true;
        bitsandbalance$needsPostDestroyQuadSyncPosLong = pos.asLong();
        bitsandbalance$forceBreakerQuadRefresh(level, player, pos.asLong());
        try {
            var server = level.getServer();
            if (server != null) {
                server.execute(new net.minecraft.server.TickTask(1, () -> {
                    try {
                        bitsandbalance$forceBreakerQuadRefresh(level, player, pos.asLong());
                    } catch (Throwable ignored) {
                    }
                }));
            }
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private @Nullable QuadStepBlock.CachedBreak bitsandbalance$buildStepCachedBreak(ServerLevel level,
                                                                                    ServerPlayer player,
                                                                                    BlockPos pos,
                                                                                    BlockState current) {
        BlockState[] slots = EnhancedSlabCreateCompat.captureMaterialSlots(level, pos, current);
        boolean lockedTarget = bitsandbalance$lockedSableStepPos != null
                && bitsandbalance$lockedSableStepPos.equals(pos)
                && bitsandbalance$slot(slots, bitsandbalance$lockedSableStepIndex) != null;
        if (!bitsandbalance$hasAnySlot(slots)
                || (!lockedTarget && !player.isShiftKeyDown() && !bitsandbalance$requiresTargetedBreaking(slots))) {
            return null;
        }

        int preferredIndex = lockedTarget
                ? bitsandbalance$lockedSableStepIndex
                : QuadStepBlock.bitsandbalance$getTargetedIndex(
                        player,
                        pos,
                        current.hasProperty(QuadStepBlock.AXIS)
                                ? current.getValue(QuadStepBlock.AXIS)
                                : Direction.Axis.X
                );
        int targetedIndex = bitsandbalance$resolveStepIndex(slots, preferredIndex);
        BlockState mined = bitsandbalance$slot(slots, targetedIndex);
        if (mined == null) {
            return null;
        }

        boolean targetedOnly = lockedTarget
                ? bitsandbalance$lockedSableStepTargetedOnly
                : player.isShiftKeyDown();
        QuadStepBlock.CachedBreak cached = new QuadStepBlock.CachedBreak();
        cached.posLong = pos.asLong();
        cached.targetedIndex = targetedIndex;
        cached.minedSlab = mined;
        cached.remainingSlabs = bitsandbalance$buildRemainingSlots(slots, targetedIndex, targetedOnly);
        cached.waterlogged = current.hasProperty(QuadStepBlock.WATERLOGGED) && current.getValue(QuadStepBlock.WATERLOGGED);
        cached.axis = current.hasProperty(QuadStepBlock.AXIS) ? current.getValue(QuadStepBlock.AXIS) : net.minecraft.core.Direction.Axis.X;
        cached.removedCount = targetedOnly ? 1 : bitsandbalance$countMatching(slots, mined);
        return cached;
    }

    @Unique
    private void bitsandbalance$clearLockedSableStepPart() {
        bitsandbalance$lockedSableStepPos = null;
        bitsandbalance$lockedSableStepIndex = -1;
        bitsandbalance$lockedSableStepTargetedOnly = false;
    }

    @Unique
    private void bitsandbalance$clearLockedSableStepPart(BlockPos pos) {
        if (bitsandbalance$lockedSableStepPos != null
                && bitsandbalance$lockedSableStepPos.equals(pos)) {
            bitsandbalance$clearLockedSableStepPart();
        }
    }

    @Unique
    private @Nullable QuadVerticalStepBlock.CachedBreak bitsandbalance$buildVerticalStepCachedBreak(ServerLevel level,
                                                                                                    ServerPlayer player,
                                                                                                    BlockPos pos,
                                                                                                    BlockState current) {
        BlockState[] slots = EnhancedSlabCreateCompat.captureMaterialSlots(level, pos, current);
        boolean lockedTarget = bitsandbalance$lockedSableVerticalStepPos != null
                && bitsandbalance$lockedSableVerticalStepPos.equals(pos)
                && bitsandbalance$slot(slots, bitsandbalance$lockedSableVerticalStepIndex) != null;
        if (!bitsandbalance$hasAnySlot(slots)
                || (!lockedTarget && !player.isShiftKeyDown() && !bitsandbalance$requiresTargetedBreaking(slots))) {
            return null;
        }

        int preferredIndex = lockedTarget
                ? bitsandbalance$lockedSableVerticalStepIndex
                : QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, pos);
        int targetedIndex = bitsandbalance$resolveVerticalStepIndex(slots, preferredIndex);
        BlockState mined = bitsandbalance$slot(slots, targetedIndex);
        if (mined == null) {
            return null;
        }

        boolean targetedOnly = lockedTarget
                ? bitsandbalance$lockedSableVerticalStepTargetedOnly
                : player.isShiftKeyDown();
        QuadVerticalStepBlock.CachedBreak cached = new QuadVerticalStepBlock.CachedBreak();
        cached.posLong = pos.asLong();
        cached.targetedIndex = targetedIndex;
        cached.minedSlab = mined;
        cached.remainingSlabs = bitsandbalance$buildRemainingSlots(slots, targetedIndex, targetedOnly);
        cached.waterlogged = current.hasProperty(QuadVerticalStepBlock.WATERLOGGED) && current.getValue(QuadVerticalStepBlock.WATERLOGGED);
        cached.removedCount = targetedOnly ? 1 : bitsandbalance$countMatching(slots, mined);
        return cached;
    }

    @Unique
    private void bitsandbalance$clearLockedSableVerticalStepPart() {
        bitsandbalance$lockedSableVerticalStepPos = null;
        bitsandbalance$lockedSableVerticalStepIndex = -1;
        bitsandbalance$lockedSableVerticalStepTargetedOnly = false;
    }

    @Unique
    private void bitsandbalance$clearLockedSableVerticalStepPart(BlockPos pos) {
        if (bitsandbalance$lockedSableVerticalStepPos != null
                && bitsandbalance$lockedSableVerticalStepPos.equals(pos)) {
            bitsandbalance$clearLockedSableVerticalStepPart();
        }
    }

    @Unique
    private static boolean bitsandbalance$hasAnySlot(@Nullable BlockState[] slots) {
        if (slots == null) return false;
        for (BlockState slot : slots) {
            if (slot != null) return true;
        }
        return false;
    }

    @Unique
    private static boolean bitsandbalance$requiresTargetedBreaking(@Nullable BlockState[] slots) {
        BlockState first = null;
        if (slots == null) return false;
        for (BlockState slot : slots) {
            if (slot == null) continue;
            if (first == null) {
                first = slot;
            } else if (!first.equals(slot)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static @Nullable BlockState bitsandbalance$slot(@Nullable BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    @Unique
    private static BlockState[] bitsandbalance$buildRemainingSlots(@Nullable BlockState[] slots,
                                                                    int targetedIndex,
                                                                    boolean targetedOnly) {
        BlockState[] remaining = new BlockState[4];
        if (slots != null) {
            System.arraycopy(slots, 0, remaining, 0, Math.min(4, slots.length));
        }
        BlockState selected = bitsandbalance$slot(slots, targetedIndex);
        if (selected == null) {
            return remaining;
        }
        for (int i = 0; i < remaining.length; i++) {
            if (remaining[i] == null) continue;
            if (targetedOnly) {
                if (i == targetedIndex) {
                    remaining[i] = null;
                }
            } else if (selected.equals(remaining[i])) {
                remaining[i] = null;
            }
        }
        return remaining;
    }

    @Unique
    private static int bitsandbalance$countMatching(@Nullable BlockState[] slots, @Nullable BlockState selected) {
        if (slots == null || selected == null) {
            return 0;
        }
        int count = 0;
        for (BlockState slot : slots) {
            if (selected.equals(slot)) {
                count++;
            }
        }
        return count;
    }

    @Unique
    private static int bitsandbalance$countSlots(@Nullable BlockState[] slots) {
        if (slots == null) {
            return 0;
        }
        int count = 0;
        for (BlockState slot : slots) {
            if (slot != null) {
                count++;
            }
        }
        return count;
    }

    @Unique
    private static int bitsandbalance$resolveStepIndex(@Nullable BlockState[] slots, int preferredIndex) {
        int index = bitsandbalance$resolveIndex(slots, preferredIndex);
        if (index >= 0) return index;

        boolean positive = preferredIndex >= 0 && preferredIndex < 4 && QuadStepBlock.isPositive(preferredIndex);
        boolean top = preferredIndex >= 0 && preferredIndex < 4 && QuadStepBlock.isTop(preferredIndex);
        index = bitsandbalance$resolveIndex(slots, QuadStepBlock.toIndex(positive, !top));
        if (index >= 0) return index;
        index = bitsandbalance$resolveIndex(slots, QuadStepBlock.toIndex(!positive, top));
        if (index >= 0) return index;
        index = bitsandbalance$resolveIndex(slots, QuadStepBlock.toIndex(!positive, !top));
        return index >= 0 ? index : bitsandbalance$firstOccupiedIndex(slots);
    }

    @Unique
    private static int bitsandbalance$resolveVerticalStepIndex(@Nullable BlockState[] slots, int preferredIndex) {
        int index = bitsandbalance$resolveIndex(slots, preferredIndex);
        if (index >= 0) return index;

        boolean east = preferredIndex >= 0 && preferredIndex < 4 && QuadVerticalStepBlock.isEast(preferredIndex);
        boolean south = preferredIndex >= 0 && preferredIndex < 4 && QuadVerticalStepBlock.isSouth(preferredIndex);
        index = bitsandbalance$resolveIndex(slots, QuadVerticalStepBlock.toIndex(east, !south));
        if (index >= 0) return index;
        index = bitsandbalance$resolveIndex(slots, QuadVerticalStepBlock.toIndex(!east, south));
        if (index >= 0) return index;
        index = bitsandbalance$resolveIndex(slots, QuadVerticalStepBlock.toIndex(!east, !south));
        return index >= 0 ? index : bitsandbalance$firstOccupiedIndex(slots);
    }

    @Unique
    private static int bitsandbalance$resolveIndex(@Nullable BlockState[] slots, int index) {
        return bitsandbalance$slot(slots, index) == null ? -1 : index;
    }

    @Unique
    private static int bitsandbalance$firstOccupiedIndex(@Nullable BlockState[] slots) {
        if (slots == null) return -1;
        for (int i = 0; i < Math.min(4, slots.length); i++) {
            if (slots[i] != null) return i;
        }
        return -1;
    }
}

