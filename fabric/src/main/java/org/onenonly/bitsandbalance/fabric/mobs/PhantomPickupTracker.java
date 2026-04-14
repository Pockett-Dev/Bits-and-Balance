package org.onenonly.bitsandbalance.fabric.mobs;

public interface PhantomPickupTracker {
    long bitsandbalance$getPhantomPickupTime();

    int bitsandbalance$getPhantomPickupVehicleId();

    boolean bitsandbalance$didHitPhantomDuringCarry();

    void bitsandbalance$setPhantomPickupState(long pickupTime, int vehicleId);

    void bitsandbalance$setPhantomHitDuringCarry(boolean hitDuringCarry);

    void bitsandbalance$clearPhantomPickupState();
}