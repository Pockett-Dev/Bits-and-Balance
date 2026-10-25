package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.fabric.combat.PowderSnowTags;
import org.onenonly.bitsandbalance.fabric.config.FabricCombatConfig;

public final class FabricSnowballNetworking {
    private FabricSnowballNetworking() {
    }

    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(SnowballPowderSnowPayload.TYPE, (payload, context) -> {
            if (!FabricCombatConfig.enableSnowballRework) {
                return;
            }

            context.client().execute(() -> {
                var mc = Minecraft.getInstance();
                var level = mc.level;
                if (level == null) return;
                var entity = level.getEntity(payload.entityId());
                if (entity instanceof LivingEntity living) {
                    long now = level.getGameTime();
                    PowderSnowTags.extendUntil(living, now + (long) payload.freezeTicks());
                }
            });
        });
    }
}
