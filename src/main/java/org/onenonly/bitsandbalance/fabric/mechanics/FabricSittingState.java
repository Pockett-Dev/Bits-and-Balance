package org.onenonly.bitsandbalance.fabric.mechanics;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricSittingState {
    private FabricSittingState() {
    }

    private static final Set<UUID> SITTING_PLAYERS = ConcurrentHashMap.newKeySet();

    public static boolean isSitting(UUID uuid) {
        return uuid != null && SITTING_PLAYERS.contains(uuid);
    }

    public static void setSitting(UUID uuid, boolean sitting) {
        if (uuid == null) return;
        if (sitting) {
            SITTING_PLAYERS.add(uuid);
        } else {
            SITTING_PLAYERS.remove(uuid);
        }
    }

    public static Set<UUID> snapshot() {
        return Set.copyOf(SITTING_PLAYERS);
    }

    public static void clear(UUID uuid) {
        if (uuid != null) {
            SITTING_PLAYERS.remove(uuid);
        }
    }

    public static void clearAll() {
        SITTING_PLAYERS.clear();
    }
}
