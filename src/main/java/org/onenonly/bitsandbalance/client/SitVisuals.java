package org.onenonly.bitsandbalance.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-only registry of entities that should be visually rendered in a riding/sitting pose
 * without actually being mounted on a vehicle.
 */
public final class SitVisuals {
    private static final Set<Integer> SITTING_ENTITIES = new HashSet<>();

    private SitVisuals() {}

    public static void set(int entityId, boolean sitting) {
        if (sitting) {
            SITTING_ENTITIES.add(entityId);
        } else {
            SITTING_ENTITIES.remove(entityId);
        }
    }

    public static boolean isSitting(int entityId) {
        return SITTING_ENTITIES.contains(entityId);
    }

    public static boolean isSitting(Entity e) {
        return e != null && isSitting(e.getId());
    }

    public static void setLocalPlayer(boolean sitting) {
        try {
            var mc = Minecraft.getInstance();
            if (mc == null) return;
            var p = mc.player;
            if (p == null) return;
            set(p.getId(), sitting);
        } catch (Throwable ignored) {}
    }
}
