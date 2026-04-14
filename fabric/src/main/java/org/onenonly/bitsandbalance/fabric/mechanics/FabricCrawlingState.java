package org.onenonly.bitsandbalance.fabric.mechanics;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricCrawlingState {
    private FabricCrawlingState() {
    }

    private static final ConcurrentHashMap<UUID, Boolean> CRAWLING = new ConcurrentHashMap<>();

    public static boolean isCrawling(UUID uuid) {
        if (uuid == null) return false;
        return Boolean.TRUE.equals(CRAWLING.get(uuid));
    }

    public static void setCrawling(UUID uuid, boolean crawling) {
        if (uuid == null) return;
        if (crawling) {
            CRAWLING.put(uuid, Boolean.TRUE);
        } else {
            CRAWLING.remove(uuid);
        }
    }

    public static void clear(UUID uuid) {
        if (uuid != null) {
            CRAWLING.remove(uuid);
        }
    }

    public static void clearAll() {
        CRAWLING.clear();
    }
}
