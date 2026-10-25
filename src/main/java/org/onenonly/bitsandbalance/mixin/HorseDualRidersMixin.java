package org.onenonly.bitsandbalance.mixin;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.horse.HorseDualRiderSeatAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(Entity.class)
public abstract class HorseDualRidersMixin implements HorseDualRiderSeatAccess {
    @Shadow
    private ImmutableList<Entity> passengers;

    @Unique
    private UUID bitsandbalance$rearHorseRiderId;

    @Override
    public void bitsandbalance$setRearHorseRider(UUID riderId) {
        this.bitsandbalance$rearHorseRiderId = riderId;
    }

    @Override
    public boolean bitsandbalance$isRearHorseRider(Entity passenger) {
        return passenger instanceof Player
                && this.bitsandbalance$rearHorseRiderId != null
                && this.bitsandbalance$rearHorseRiderId.equals(passenger.getUUID());
    }

    @Override
    public void bitsandbalance$moveHorseRiderToFront(Entity passenger) {
        Entity vehicle = (Entity) (Object) this;
        List<Entity> currentPassengers = vehicle.getPassengers();
        if (currentPassengers.isEmpty() || currentPassengers.get(0) == passenger || !currentPassengers.contains(passenger)) {
            return;
        }

        ArrayList<Entity> reorderedPassengers = new ArrayList<>(currentPassengers);
        reorderedPassengers.remove(passenger);
        reorderedPassengers.add(0, passenger);
        this.passengers = ImmutableList.copyOf(reorderedPassengers);
    }

    @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$allowSecondHorseRider(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
        Entity vehicle = (Entity) (Object) this;
        if (!Config.enableHorseDualRiders || vehicle.getType() != EntityType.HORSE || !(passenger instanceof Player)) {
            return;
        }

        if (vehicle.getPassengers().size() < 2) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getPassengerRidingPosition", at = @At("RETURN"))
    private void bitsandbalance$trackFallbackRearHorseRider(Entity passenger, CallbackInfoReturnable<Vec3> cir) {
        Entity vehicle = (Entity) (Object) this;
        if (!Config.enableHorseDualRiders || vehicle.getType() != EntityType.HORSE) {
            return;
        }

        List<Entity> passengers = vehicle.getPassengers();
        if (this.bitsandbalance$rearHorseRiderId == null && passengers.size() >= 2 && passengers.indexOf(passenger) == 1 && passenger instanceof Player) {
            this.bitsandbalance$rearHorseRiderId = passenger.getUUID();
        }
    }

    @Inject(method = "removePassenger", at = @At("RETURN"))
    private void bitsandbalance$clearRearSeatWhenRearRiderDismounts(Entity passenger, CallbackInfo ci) {
        Entity vehicle = (Entity) (Object) this;
        if (this.bitsandbalance$rearHorseRiderId == null || vehicle.getType() != EntityType.HORSE) {
            return;
        }

        if (this.bitsandbalance$rearHorseRiderId.equals(passenger.getUUID()) || vehicle.getPassengers().isEmpty()) {
            this.bitsandbalance$rearHorseRiderId = null;
        }
    }
}
