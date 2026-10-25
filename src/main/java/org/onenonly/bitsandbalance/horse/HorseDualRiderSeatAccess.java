package org.onenonly.bitsandbalance.horse;

import net.minecraft.world.entity.Entity;

import java.util.UUID;

public interface HorseDualRiderSeatAccess {
    void bitsandbalance$setRearHorseRider(UUID riderId);

    boolean bitsandbalance$isRearHorseRider(Entity passenger);

    void bitsandbalance$moveHorseRiderToFront(Entity passenger);
}
