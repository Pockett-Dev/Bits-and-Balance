package org.onenonly.bitsandbalance.client;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-only sitting state and sync cache.
 */
public final class ClientState {
    private ClientState() {}

    // Local player desired sitting state (for quick checks)
    public static boolean sitting = false;

    // Synced set of remote players that are sitting (and the local player after sync)
    // Thread-safe: accessed from both render thread and game thread
    public static final Set<UUID> SYNCED_SITTING_PLAYERS = Collections.newSetFromMap(new ConcurrentHashMap<>());
}
