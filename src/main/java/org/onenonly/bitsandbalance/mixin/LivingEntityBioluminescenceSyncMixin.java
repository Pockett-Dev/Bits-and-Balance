package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceSyncBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if mc != 1.21.1
import java.util.Collection;
//? endif

@Mixin(LivingEntity.class)
public abstract class LivingEntityBioluminescenceSyncMixin {
    @Inject(method = "onEffectAdded", at = @At("TAIL"))
    private void bitsandbalance$syncBioluminescenceAdded(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        bitsandbalance$syncActiveBioluminescence(effect);
    }

    @Inject(method = "onEffectUpdated", at = @At("TAIL"))
    private void bitsandbalance$syncBioluminescenceUpdated(MobEffectInstance effect, boolean reapplyEffect, Entity source, CallbackInfo ci) {
        bitsandbalance$syncActiveBioluminescence(effect);
    }

    //? if mc == 1.21.1
    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void bitsandbalance$syncBioluminescenceRemoved(MobEffectInstance effect, CallbackInfo ci) {
        if (bitsandbalance$isBioluminescence(effect)) {
            bitsandbalance$syncBioluminescenceCleared();
        }
    }
    //? else
    @Inject(method = "onEffectsRemoved", at = @At("TAIL"))
    private void bitsandbalance$syncBioluminescenceRemoved(Collection<MobEffectInstance> effects, CallbackInfo ci) {
        if (effects.stream().anyMatch(LivingEntityBioluminescenceSyncMixin::bitsandbalance$isBioluminescence)) {
            bitsandbalance$syncBioluminescenceCleared();
        }
    }
    //? endif

    private void bitsandbalance$syncActiveBioluminescence(MobEffectInstance effect) {
        if (bitsandbalance$isBioluminescence(effect)) {
            LivingEntity entity = (LivingEntity) (Object) this;
            BioluminescenceSyncBridge.syncIfServer(entity, effect.getDuration());
            BioluminescenceSyncBridge.syncIfClient(entity, effect.getDuration(), effect.getAmplifier());
        }
    }

    private void bitsandbalance$syncBioluminescenceCleared() {
        LivingEntity entity = (LivingEntity) (Object) this;
        BioluminescenceSyncBridge.syncIfServer(entity, 0);
        BioluminescenceSyncBridge.syncIfClient(entity, 0, 0);
    }

    private static boolean bitsandbalance$isBioluminescence(MobEffectInstance effect) {
        return effect != null && effect.getEffect().equals(ModGlowGoo.bioluminescence());
    }
}
