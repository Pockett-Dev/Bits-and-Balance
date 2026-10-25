package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.horse.HorseDualRiderSeatAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public abstract class HorseDualRidersPositionMixin {
    @Unique
    private static final double DRIVER_FORWARD_OFFSET = 0.2D;

    @Unique
    private static final double SECOND_RIDER_BACK_OFFSET = -0.55D;

    @Inject(method = "getPassengerAttachmentPoint", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$offsetRearHorseRiderAttachment(Entity passenger, EntityDimensions dimensions, float scale, CallbackInfoReturnable<Vec3> cir) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        if (!Config.enableHorseDualRiders || horse.getType() != EntityType.HORSE) {
            return;
        }

        double seatOffset = bitsandbalance$seatOffset(horse, passenger);
        if (seatOffset == 0.0D) {
            return;
        }

        Vec3 rotatedOffset = new Vec3(0.0D, 0.0D, seatOffset).yRot(-horse.getYRot() * ((float) Math.PI / 180.0F));
        cir.setReturnValue(cir.getReturnValue().add(rotatedOffset));
    }

    @Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V", at = @At("TAIL"))
    private void bitsandbalance$syncRearHorseRiderFallback(Entity passenger, Entity.MoveFunction moveFunction, CallbackInfo ci) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        // Track the rear seat for positioning; vanilla owns rider body rotation.
        if (Config.enableHorseDualRiders
                && horse.getType() == EntityType.HORSE
                && horse.getPassengers().size() >= 2
                && horse.getPassengers().indexOf(passenger) == 1) {
            ((HorseDualRiderSeatAccess) horse).bitsandbalance$setRearHorseRider(passenger.getUUID());
        }

    }

    @Unique
    private boolean bitsandbalance$isRearSeatPassenger(AbstractHorse horse, Entity passenger) {
        return ((HorseDualRiderSeatAccess) horse).bitsandbalance$isRearHorseRider(passenger)
                || (horse.getPassengers().size() >= 2 && horse.getPassengers().indexOf(passenger) == 1);
    }

    @Unique
    private double bitsandbalance$seatOffset(AbstractHorse horse, Entity passenger) {
        if (horse.getPassengers().size() > 1) {
            int passengerIndex = horse.getPassengers().indexOf(passenger);
            if (passengerIndex == 0) {
                return DRIVER_FORWARD_OFFSET;
            }
            if (passengerIndex == 1) {
                return SECOND_RIDER_BACK_OFFSET;
            }
        }

        return bitsandbalance$isRearSeatPassenger(horse, passenger) ? SECOND_RIDER_BACK_OFFSET : 0.0D;
    }
}
