package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.common.mechanics.DreamProjection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if mc != 1.21.1
import net.minecraft.server.level.ServerLevel;
//? endif

@Mixin(LivingEntity.class)
public abstract class LivingEntityDreamBodyMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$doNotPushDreamAvatars(CallbackInfoReturnable<Boolean> cir) {
        if (DreamProjection.isVisualAvatar((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$ignoreDreamAvatarPush(Entity other, CallbackInfo ci) {
        if (DreamProjection.isVisualAvatar((LivingEntity) (Object) this)
                || DreamProjection.isVisualAvatar(other)) {
            ci.cancel();
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void bitsandbalance$endDreamOnBodyDeath(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && DreamProjection.isProjected(player)) {
            DreamProjection.onLeave(player);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bitsandbalance$removeOrphanedDreamBody(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide()) {
            DreamProjection.tickBodyView(entity);
        }
        if (DreamProjection.isOrphanedBody(entity)) {
            entity.discard();
        }
    }

    //? if mc == 1.21.1
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$redirectBodyDamage(DamageSource source, float amount,
                                                     CallbackInfoReturnable<Boolean> cir) {
    //? else
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$redirectBodyDamage(ServerLevel level, DamageSource source, float amount,
                                                     CallbackInfoReturnable<Boolean> cir) {
    //? endif
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide()
                && (DreamProjection.isBody(entity) || DreamProjection.isGhost(entity))) {
            cir.setReturnValue(DreamProjection.hurtBody(entity, source, amount));
        }
    }
}
