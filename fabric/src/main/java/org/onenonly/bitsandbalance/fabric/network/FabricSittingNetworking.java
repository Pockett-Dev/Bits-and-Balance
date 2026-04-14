package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.client.FabricClientState;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricSittingState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricSittingNetworking {
    private FabricSittingNetworking() {
    }

    private static final Map<UUID, SitAnchor> SIT_ANCHORS = new ConcurrentHashMap<>();

    public static void initServer() {
        ServerPlayNetworking.registerGlobalReceiver(SitTogglePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                // If the user disables the feature (by setting toggleSitting false in configs),
                // we still allow hold-to-sit behavior. Server-side accepts both modes.

                boolean wantSit = payload.sitting();

                boolean ok = true;
                try {
                    ok = testSit(player);
                } catch (Throwable ignored) {
                }

                boolean finalSit = wantSit && ok;

                if (finalSit) {
                    // Sitting: ensure mutually exclusive with crawling/sneak
                    try { player.setShiftKeyDown(false); } catch (Throwable ignored) {}

                    // Prefer vanilla sitting pose (same pose used while riding/boating).
                    try { player.setPose(Pose.SITTING); } catch (Throwable ignored) {}
                } else {
                    // Only restore standing if we previously forced sitting.
                    try {
                        if (!player.isPassenger() && player.getPose() == Pose.SITTING) {
                            player.setPose(Pose.STANDING);
                        }
                    } catch (Throwable ignored) {}
                }

                FabricSittingState.setSitting(player.getUUID(), finalSit);

                // Record/clear anchor so movement/knockback cancels sitting.
                try {
                    if (finalSit) {
                        String dimId;
                        try {
                            dimId = player.level().dimension().identifier().toString();
                        } catch (Throwable ignored) {
                            dimId = "";
                        }
                        SIT_ANCHORS.put(player.getUUID(), new SitAnchor(dimId, player.position()));
                    } else {
                        SIT_ANCHORS.remove(player.getUUID());
                    }
                } catch (Throwable ignored) {}

                try { player.refreshDimensions(); } catch (Throwable ignored) {}
                broadcastSit(player, FabricSittingState.isSitting(player.getUUID()));
            });
        });

        // Send existing sitting states on join, and ensure player starts synced.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            server.execute(() -> {
                // Ensure we don't keep stale state from previous sessions.
                boolean sit = FabricSittingState.isSitting(player.getUUID());
                ServerPlayNetworking.send(player, new SyncSitPayload(player.getUUID(), sit));

                for (var uuid : FabricSittingState.snapshot()) {
                    if (uuid.equals(player.getUUID())) continue;
                    ServerPlayNetworking.send(player, new SyncSitPayload(uuid, true));
                }
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var player = handler.getPlayer();
            server.execute(() -> {
                FabricSittingState.clear(player.getUUID());
                SIT_ANCHORS.remove(player.getUUID());
            });
        });

        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            if (!(player instanceof ServerPlayer sp)) return;
            if (!(trackedEntity instanceof ServerPlayer trackedPlayer)) return;
            try {
                boolean sit = FabricSittingState.isSitting(trackedPlayer.getUUID());
                ServerPlayNetworking.send(sp, new SyncSitPayload(trackedPlayer.getUUID(), sit));
            } catch (Throwable ignored) {
            }
        });

        // If the player moves/gets knocked back/etc., clear sitting state.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                    var id = sp.getUUID();
                    boolean sitting = FabricSittingState.isSitting(id);
                    if (!sitting) continue;

                    boolean valid = true;
                    try { valid = testSit(sp); } catch (Throwable ignored) { valid = false; }

                    try {
                        SitAnchor anchor = SIT_ANCHORS.get(id);
                        if (anchor != null) {
                            String currentDimStr;
                            try {
                                currentDimStr = sp.level().dimension().identifier().toString();
                            } catch (Throwable ignored) {
                                currentDimStr = "";
                            }
                            if (!anchor.dimensionId.isEmpty() && !anchor.dimensionId.equals(currentDimStr)) {
                                valid = false;
                            }

                            double dx = sp.getX() - anchor.position.x;
                            double dy = sp.getY() - anchor.position.y;
                            double dz = sp.getZ() - anchor.position.z;

                            double maxXZ = 0.15D;
                            double maxY = 0.35D;
                            if ((dx * dx + dz * dz) > (maxXZ * maxXZ) || Math.abs(dy) > maxY) {
                                valid = false;
                            }
                        }
                    } catch (Throwable ignored) {
                    }

                    if (!valid) {
                        FabricSittingState.setSitting(id, false);
                        SIT_ANCHORS.remove(id);
                        try { sp.refreshDimensions(); } catch (Throwable ignored) {}
                        broadcastSit(sp, false);
                    }
                }
            } catch (Throwable ignored) {
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            FabricSittingState.clearAll();
            SIT_ANCHORS.clear();
        });
    }

    public static void initClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(SyncSitPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.sitting()) {
                    FabricClientState.SYNCED_SITTING_PLAYERS.add(payload.uuid());
                } else {
                    FabricClientState.SYNCED_SITTING_PLAYERS.remove(payload.uuid());
                }

                var player = context.client().player;
                if (player != null && player.getUUID().equals(payload.uuid())) {
                    FabricClientState.sitting = payload.sitting();
                }

                try {
                    var level = context.client().level;
                    if (level != null) {
                        var target = level.getPlayerByUUID(payload.uuid());
                        if (target != null) {
                            try {
                                if (payload.sitting() && !target.isPassenger()) {
                                    target.setPose(Pose.SITTING);
                                } else if (!payload.sitting() && !target.isPassenger() && target.getPose() == Pose.SITTING) {
                                    target.setPose(Pose.STANDING);
                                }
                            } catch (Throwable ignored) {
                            }
                            target.refreshDimensions();
                        }
                    }
                } catch (Throwable ignored) {
                }
            });
        });
    }

    private static void broadcastSit(ServerPlayer sourcePlayer, boolean sitting) {
        // Send to the source player + anyone tracking them.
        ServerPlayNetworking.send(sourcePlayer, new SyncSitPayload(sourcePlayer.getUUID(), sitting));
        for (ServerPlayer sp : PlayerLookup.tracking(sourcePlayer)) {
            ServerPlayNetworking.send(sp, new SyncSitPayload(sourcePlayer.getUUID(), sitting));
        }
    }

    /**
     * Conservative server-side sitting validation.
     * Mirrors the NeoForge intent: only allow sitting when grounded, not in disallowed states,
     * and when a reduced-height AABB would not collide.
     */
    private static boolean testSit(ServerPlayer p) {
        if (p == null) return false;
        try {
            if (p.isRemoved() || p.isDeadOrDying()) return false;
        } catch (Throwable ignored) {
        }

        try { if (p.isPassenger()) return false; } catch (Throwable ignored) {}
        try { if (p.isSleeping()) return false; } catch (Throwable ignored) {}
        try { if (p.isSwimming()) return false; } catch (Throwable ignored) {}
        try { if (p.isFallFlying()) return false; } catch (Throwable ignored) {}
        try { if (p.getPose() == Pose.SWIMMING) return false; } catch (Throwable ignored) {}
        try { if (p.isInLava() || p.isInWater()) return false; } catch (Throwable ignored) {}

        boolean onGround = false;
        try { onGround = p.onGround(); } catch (Throwable ignored) {}
        if (!onGround) {
            try {
                BlockPos below = BlockPos.containing(p.getX(), Math.floor(p.getY() - 0.001), p.getZ()).below();
                BlockState state = p.level().getBlockState(below);
                if (state.isAir()) return false;
            } catch (Throwable t) {
                return false;
            }
        }

        try {
            AABB bb = p.getBoundingBox();
            double reduce = 0.5D;
            AABB reduced = new AABB(
                    bb.minX, bb.minY, bb.minZ,
                    bb.maxX, bb.minY + Math.max(0.3D, (bb.maxY - bb.minY) - reduce), bb.maxZ
            );
            if (!p.level().noCollision(p, reduced)) return false;
        } catch (Throwable ignored) {
        }

        return true;
    }

    private record SitAnchor(String dimensionId, Vec3 position) {
    }
}
