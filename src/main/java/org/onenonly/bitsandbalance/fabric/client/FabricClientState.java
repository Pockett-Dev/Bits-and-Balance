package org.onenonly.bitsandbalance.fabric.client;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricClientState {
    private FabricClientState() {
    }

    public static volatile boolean sitting = false;
    public static final Set<UUID> SYNCED_SITTING_PLAYERS = ConcurrentHashMap.newKeySet();
}
