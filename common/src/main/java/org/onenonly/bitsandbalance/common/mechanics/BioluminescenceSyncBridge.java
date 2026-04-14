package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;

public final class BioluminescenceSyncBridge {
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
}