package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;

public final class FabricBioluminescenceSync {
    private FabricBioluminescenceSync() {
    }

    public static void initServer() {
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            if (!(trackedEntity instanceof LivingEntity living)) {
                return;
            }

            int remainingTicks = getBioluminescenceRemainingTicks(living);
            if (remainingTicks > 0) {
                ServerPlayNetworking.send(
                        player,
                        new BioluminescenceSyncPayload(
                                living.getId(),
                                remainingTicks,
                                getBioluminescenceAmplifier(living)
                        )
                );
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> {
                if (!(handler.getPlayer() instanceof LivingEntity living)) {
                    return;
                }

                int remainingTicks = getBioluminescenceRemainingTicks(living);
                if (remainingTicks > 0) {
                    ServerPlayNetworking.send(
                            handler.getPlayer(),
                            new BioluminescenceSyncPayload(
                                    living.getId(),
                                    remainingTicks,
                                    getBioluminescenceAmplifier(living)
                            )
                    );
                }
            });
        });
    }

    public static void syncIfServer(LivingEntity entity, int remainingTicks) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }

        BioluminescenceSyncPayload payload = new BioluminescenceSyncPayload(
                entity.getId(),
                remainingTicks,
                getBioluminescenceAmplifier(entity)
        );
        if (entity instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, payload);
        }
        for (ServerPlayer trackingPlayer : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(trackingPlayer, payload);
        }
    }

    private static int getBioluminescenceRemainingTicks(LivingEntity entity) {
        MobEffectInstance bioluminescence = entity.getEffect(ModGlowGoo.bioluminescence());
        return bioluminescence != null ? bioluminescence.getDuration() : 0;
    }

    private static int getBioluminescenceAmplifier(LivingEntity entity) {
        MobEffectInstance bioluminescence = entity.getEffect(ModGlowGoo.bioluminescence());
        return bioluminescence != null ? bioluminescence.getAmplifier() : 0;
    }
}
