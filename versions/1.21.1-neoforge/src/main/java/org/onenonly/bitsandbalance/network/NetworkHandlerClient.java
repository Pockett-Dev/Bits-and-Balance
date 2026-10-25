package org.onenonly.bitsandbalance.network;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.client.BioluminescenceLevelRendererAccess;
import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.onenonly.bitsandbalance.common.combat.PowderSnowMarks;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHangIntentClientCache;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;


public class NetworkHandlerClient {

    // No registration needed - channels are registered in NetworkHandler
    // Client-side handlers are called via event system instead

    public static void handleSnowballPowderSnow(final SnowballPowderSnowPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            var entity = level.getEntity(payload.entityId());
            if (entity instanceof LivingEntity living) {
                try {
                    living.setIsInPowderSnow(true);
                } catch (Throwable ignored) {
                }
                long now = level.getGameTime();
                PowderSnowMarks.extendUntil(living, now + (long) payload.freezeTicks());
            }
        });
    }

    public static void handleSitSync(final org.onenonly.bitsandbalance.network.SyncSitPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var level = mc.level;
            if (level == null) return;
            var entity = level.getPlayerByUUID(payload.uuid());
            boolean sit = payload.sitting();
            if (sit) {
                org.onenonly.bitsandbalance.client.ClientState.SYNCED_SITTING_PLAYERS.add(payload.uuid());
            } else {
                org.onenonly.bitsandbalance.client.ClientState.SYNCED_SITTING_PLAYERS.remove(payload.uuid());
            }
            if (entity != null) {
                try {
                    if (sit && !entity.isPassenger()) {
                        entity.setPose(net.minecraft.world.entity.Pose.SITTING);
                    } else if (!sit && !entity.isPassenger() && entity.getPose() == net.minecraft.world.entity.Pose.SITTING) {
                        entity.setPose(net.minecraft.world.entity.Pose.STANDING);
                    }
                } catch (Throwable ignored) {
                }
                try { entity.refreshDimensions(); } catch (Throwable ignored) {}
                if (mc.player != null && mc.player.getUUID().equals(payload.uuid())) {
                    org.onenonly.bitsandbalance.client.ClientState.sitting = sit;
                }
            }
        });
    }

    public static void handleNavigatorCompassOpenGUI(final NavigatorCompassOpenGUIPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.player != null) {
                // Open the Navigator Compass GUI on the client
                minecraft.setScreen(new org.onenonly.bitsandbalance.client.NavigatorCompassScreen(payload.x(), payload.y(), payload.z()));
            }
        });
    }

    public static void handlePlayerMention(final PlayerMentionPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // This is handled on the client side by PlayerMentionHandler
            // The payload is received but the actual mention processing happens
            // in the chat event handler to avoid duplication
            BitsAndBalance.LOGGER.debug("Received player mention notification for player: " + payload.mentionedPlayer());
        });
    }

    public static void handleBioluminescenceSync(final BioluminescenceSyncPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> BioluminescenceLightManager.syncClientBioluminescence(
                payload.entityId(),
                payload.remainingTicks(),
                payload.amplifier()
        ));
    }

    public static void handleBiomeTitlesStructure(final BiomeTitlesStructurePayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.onenonly.bitsandbalance.common.client.BiomeTitlesClient.setInsideStructure(payload.insideStructure()));
    }

    public static void handleSecondChanceToast(final SecondChanceToastPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(org.onenonly.bitsandbalance.common.client.SecondChanceToast::show);
    }

    public static void handleYearnEcho(final YearnEchoPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.onenonly.bitsandbalance.common.client.YearnEchoClient.start(
                payload.x(), payload.y(), payload.z()));
    }

    public static void handlePonderLightAudit(final PonderLightAuditPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.onenonly.bitsandbalance.common.client.PonderLightAuditClient.start(
                payload.x(), payload.y(), payload.z(), payload.emission()));
    }

    public static void handleCandleBundleColorsUpdate(final CandleBundleColorsUpdatePayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            org.onenonly.bitsandbalance.common.candle.CandleBundleColorClientCache.apply(payload.pos(), payload.c0(), payload.c1(), payload.c2(), payload.c3());
            var mc = Minecraft.getInstance();
            var level = mc.level;
            if (level != null) {
                var pos = net.minecraft.core.BlockPos.of(payload.pos());
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
        });
    }

    public static void handleCandleBundleContentsUpdate(final org.onenonly.bitsandbalance.network.CandleBundleContentsUpdatePayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;

            org.onenonly.bitsandbalance.common.candle.CandleBundleContentsClientCache.apply(
                payload.pos(),
                payload.s0(),
                payload.s1(),
                payload.s2(),
                payload.s3()
            );

            var pos = net.minecraft.core.BlockPos.of(payload.pos());
            var be = level.getBlockEntity(pos);
            if (be instanceof org.onenonly.bitsandbalance.common.blockentity.CandleBundleBlockEntity bundleBe) {
                bundleBe.setCandles(java.util.List.of(payload.s0(), payload.s1(), payload.s2(), payload.s3()));
            }
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        });
    }

    public static void handleEnhancedSlabHangIntentUpdate(final EnhancedSlabHangIntentUpdatePayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            EnhancedSlabHangIntentClientCache.apply(payload.pos(), payload.explicit());

            var level = Minecraft.getInstance().level;
            if (level != null) {
                var pos = net.minecraft.core.BlockPos.of(payload.pos());
                EnhancedSlabHelper.invalidateVisualYOffsetCacheAround(level, pos);
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
        });
    }

    public static void handleCandleBundleColorsBulk(final CandleBundleColorsBulkPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            org.onenonly.bitsandbalance.common.candle.CandleBundleColorClientCache.clear();
            long[] pos = payload.positions();
            int[] c0 = payload.c0();
            int[] c1 = payload.c1();
            int[] c2 = payload.c2();
            int[] c3 = payload.c3();
            int n = pos.length;
            n = Math.min(n, c0.length);
            n = Math.min(n, c1.length);
            n = Math.min(n, c2.length);
            n = Math.min(n, c3.length);
            for (int i = 0; i < n; i++) {
                org.onenonly.bitsandbalance.common.candle.CandleBundleColorClientCache.apply(pos[i], c0[i], c1[i], c2[i], c3[i]);
            }
        });
    }

    public static void handleEnhancedSlabHangIntentBulk(final EnhancedSlabHangIntentBulkPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            EnhancedSlabHangIntentClientCache.clear();
            for (long posLong : payload.positions()) {
                EnhancedSlabHangIntentClientCache.apply(posLong, true);
            }
            EnhancedSlabHelper.bumpVisualYOffsetCacheEpoch();
        });
    }

    public static void handleEnhancedSlabMaterialUpdate(final EnhancedSlabMaterialUpdatePayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level != null && !payload.dimensionId().equals(EnhancedSlabMaterialSavedData.dimensionId(level))) {
                return;
            }
            EnhancedSlabMaterialSavedData.setActiveClientDimension(payload.dimensionId());
            BlockState[] slabs = payload.toSlabs();
            boolean changed = EnhancedSlabMaterialSavedData.updateClient(payload.dimensionId(), payload.pos(), slabs);
            if (level != null && changed) {
                LongOpenHashSet changedPositions = new LongOpenHashSet(1);
                changedPositions.add(payload.pos());
                refreshEnhancedSlabMaterialBlocks(level, changedPositions);
            }
        });
    }

    public static void handleEnhancedSlabMaterialBulk(final EnhancedSlabMaterialBulkPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level != null && !payload.dimensionId().equals(EnhancedSlabMaterialSavedData.dimensionId(level))) {
                return;
            }
            EnhancedSlabMaterialSavedData.setActiveClientDimension(payload.dimensionId());
            long[] positions = payload.positions();
            int count = Math.min(positions == null ? 0 : positions.length, payload.slabTags() == null ? 0 : payload.slabTags().length);
            BlockState[][] slabsByPosition = new BlockState[count][];
            for (int i = 0; i < count; i++) {
                slabsByPosition[i] = payload.toSlabs(i);
            }
            LongOpenHashSet changedPositions = EnhancedSlabMaterialSavedData.replaceClientChunkRange(
                    payload.dimensionId(),
                    payload.centerChunkX(),
                    payload.centerChunkZ(),
                    payload.chunkRadius(),
                    positions,
                    slabsByPosition);
            if (level != null && !changedPositions.isEmpty()) {
                refreshEnhancedSlabMaterialBlocks(level, changedPositions);
            }
        });
    }

    private static void refreshEnhancedSlabMaterialBlocks(Level level, LongOpenHashSet changedPositions) {
        LongOpenHashSet dirtySections = new LongOpenHashSet(Math.max(8, changedPositions.size()));
        LongIterator changedIterator = changedPositions.iterator();
        while (changedIterator.hasNext()) {
            BlockPos pos = BlockPos.of(changedIterator.nextLong());
            EnhancedSlabMaterialSavedData.hydrateClientBlockEntityAt(level, pos);
            addSection(dirtySections, pos);
            for (Direction direction : Direction.values()) {
                addSection(dirtySections, pos.relative(direction));
            }
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == level && client.levelRenderer instanceof BioluminescenceLevelRendererAccess access) {
            LongIterator sectionIterator = dirtySections.iterator();
            while (sectionIterator.hasNext()) {
                long section = sectionIterator.nextLong();
                access.bitsandbalance$scheduleChunkRebuild(
                        SectionPos.x(section),
                        SectionPos.y(section),
                        SectionPos.z(section),
                        true
                );
            }
        }

        changedIterator = changedPositions.iterator();
        while (changedIterator.hasNext()) {
            long posLong = changedIterator.nextLong();
            BlockPos pos = BlockPos.of(posLong);
            BlockState state = level.getBlockState(pos);
            SableCompat.refreshPlotBlockState(level, pos, state, state, Block.UPDATE_ALL);
            EnhancedSlabCreateCompat.invalidateClientContraptionsForSource(level, posLong);
        }
    }

    private static void addSection(LongOpenHashSet sections, BlockPos pos) {
        sections.add(SectionPos.asLong(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()),
                SectionPos.blockToSectionCoord(pos.getZ())
        ));
    }

}
