package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;

public final class BioluminescenceSyncBridge {
    private static final String CLIENT_LIGHT_MANAGER =
            "org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager";
    private static final String LAMB_DYNAMIC_LIGHTS_BRIDGE =
            "org.onenonly.bitsandbalance.common.client.BioluminescenceLambDynamicLightsBridge";
    private static final String[] SYNC_HELPERS = {
            "org.onenonly.bitsandbalance.fabric.network.FabricBioluminescenceSync",
            "org.onenonly.bitsandbalance.network.BioluminescenceSync"
    };

    private BioluminescenceSyncBridge() {
    }

    public static void syncIfServer(LivingEntity entity, int remainingTicks) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }

        for (String helperClassName : SYNC_HELPERS) {
            try {
                Class<?> helperClass = Class.forName(helperClassName);
                Method method = helperClass.getMethod("syncIfServer", LivingEntity.class, int.class);
                method.invoke(null, entity, remainingTicks);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    public static void syncIfClient(LivingEntity entity, int remainingTicks, int amplifier) {
        if (entity == null || !entity.level().isClientSide()) {
            return;
        }

        try {
            Class<?> lightManagerClass = Class.forName(CLIENT_LIGHT_MANAGER);
            Method syncMethod = lightManagerClass.getMethod(
                    "syncClientBioluminescence",
                    int.class,
                    int.class,
                    int.class
            );
            syncMethod.invoke(null, entity.getId(), remainingTicks, amplifier);

            Method observeMethod = lightManagerClass.getMethod("observeEntity", Entity.class);
            observeMethod.invoke(null, entity);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return;
        }

        try {
            Class<?> lambBridgeClass = Class.forName(LAMB_DYNAMIC_LIGHTS_BRIDGE);
            lambBridgeClass.getMethod("syncNow").invoke(null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }
}
