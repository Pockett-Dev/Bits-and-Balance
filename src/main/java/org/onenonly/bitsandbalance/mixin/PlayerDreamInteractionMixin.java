package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
//? if mc != 1.21.1 && mc != 1.21.11
import net.minecraft.world.phys.Vec3;
//? endif
import org.onenonly.bitsandbalance.common.mechanics.DreamProjection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDreamInteractionMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$blockGhostAttack(Entity target, CallbackInfo ci) {
        if (DreamProjection.isProjected((Player) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    //? if mc == 1.21.1 || mc == 1.21.11
    private void bitsandbalance$blockGhostEntityUse(Entity target, InteractionHand hand,
                                                     CallbackInfoReturnable<InteractionResult> cir) {
    //? else
    private void bitsandbalance$blockGhostEntityUse(Entity target, InteractionHand hand, Vec3 hit,
                                                     CallbackInfoReturnable<InteractionResult> cir) {
    //? endif
        Player player = (Player) (Object) this;
        if (DreamProjection.isProjected(player)
                || (!player.level().isClientSide() && DreamProjection.isBody(target))) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
