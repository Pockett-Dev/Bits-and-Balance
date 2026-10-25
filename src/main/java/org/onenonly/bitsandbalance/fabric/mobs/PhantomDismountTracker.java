package org.onenonly.bitsandbalance.fabric.mobs;

import java.util.UUID;

public interface PhantomDismountTracker {
    UUID bitsandbalance$getLastRider();

    long bitsandbalance$getLastDismountTime();

    void bitsandbalance$recordDismount(UUID riderUuid, long dismountTime);
}
