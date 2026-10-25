package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.horse.HorseDualRiderSeatAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public abstract class HorseDualRidersInteractMixin {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$mountSecondHorseRider(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        HorseDualRiderSeatAccess seatAccess = (HorseDualRiderSeatAccess) horse;
        if (!Config.enableHorseDualRiders
                || horse.getType() != EntityType.HORSE
                || player.isSecondaryUseActive()
                || horse.isBaby()) {
            return;
        }

        if (horse.hasPassenger(player)) {
            if (horse.getPassengers().size() == 1 && seatAccess.bitsandbalance$isRearHorseRider(player)) {
                seatAccess.bitsandbalance$setRearHorseRider(null);
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            return;
        }

        if (horse.getPassengers().size() != 1) {
            return;
        }

        Entity existingPassenger = horse.getPassengers().get(0);
        boolean frontSeatIsEmpty = seatAccess.bitsandbalance$isRearHorseRider(existingPassenger);

        player.setYRot(horse.getYRot());
        player.setXRot(horse.getXRot());
        if (!frontSeatIsEmpty) {
            seatAccess.bitsandbalance$setRearHorseRider(player.getUUID());
        }

        if (!horse.level().isClientSide()) {
            player.startRiding(horse);
            if (frontSeatIsEmpty) {
                seatAccess.bitsandbalance$moveHorseRiderToFront(player);
            }
        }

        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
