package org.onenonly.bitsandbalance.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.onenonly.bitsandbalance.common.entity.SoulFireBurnAccess;

public class NetworkHandlerClient {

    // No registration needed - channels are registered in NetworkHandler
    // Client-side handlers are called via event system instead

    public static void handleSnowballPowderSnow(final SnowballPowderSnowPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            var entity = level.getEntity(payload.entityId());
            if (entity instanceof LivingEntity living) {
                long now = level.getGameTime();
                long existing = living.getPersistentData().getLong("bitsandbalance_powdersnow_mark_until").orElse(0L);
                long proposed = now + (long) payload.freezeTicks();
                long until = Math.max(existing, proposed);
                living.getPersistentData().putLong("bitsandbalance_powdersnow_mark_until", until);
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
                    if (sit) {
                        entity.setPose(net.minecraft.world.entity.Pose.SITTING);
                    } else if (entity.getPose() == net.minecraft.world.entity.Pose.SITTING) {
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

    public static void handleSoulFireBurnSync(final SoulFireBurnSyncPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            var entity = level.getEntity(payload.entityId());
            if (entity instanceof SoulFireBurnAccess access) {
                access.bitsandbalance$setSoulFireBurn(payload.soulFireBurn());
            }
        });
    }

    public static void handleBioluminescenceSync(final BioluminescenceSyncPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> BioluminescenceLightManager.syncClientBioluminescence(payload.entityId(), payload.remainingTicks(), payload.amplifier()));
    }

    public static void handleNavigatorCompassOpenGUI(final NavigatorCompassOpenGUIPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var mc = Minecraft.getInstance();
            if (mc == null) return;
            mc.setScreen(new org.onenonly.bitsandbalance.client.NavigatorCompassScreen(payload.x(), payload.y(), payload.z()));
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

}
