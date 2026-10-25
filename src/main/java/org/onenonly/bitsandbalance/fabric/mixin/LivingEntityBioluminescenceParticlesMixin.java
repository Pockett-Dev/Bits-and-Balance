package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityBioluminescenceParticlesMixin {
    @Shadow @Final private static EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES;
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID;
    @Shadow @Final private Map<Holder<MobEffect>, MobEffectInstance> activeEffects;

    @Inject(method = "updateSynchronizedMobEffectParticles", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$removeBioluminescenceSwirls(CallbackInfo ci) {
        if (this.activeEffects.isEmpty()) {
            return;
        }

        boolean hasBioluminescence = false;
        boolean allAmbient = true;
        List<ParticleOptions> particleOptions = new ArrayList<>();

        for (MobEffectInstance effectInstance : this.activeEffects.values()) {
            allAmbient &= effectInstance.isAmbient();

            if (effectInstance.getEffect().equals(ModGlowGoo.bioluminescence())) {
                hasBioluminescence = true;
                continue;
            }

            if (effectInstance.isVisible()) {
                particleOptions.add(effectInstance.getParticleOptions());
            }
        }

        if (!hasBioluminescence) {
            return;
        }

        SynchedEntityData entityData = ((LivingEntity) (Object) this).getEntityData();
        entityData.set(DATA_EFFECT_PARTICLES, particleOptions);
        entityData.set(DATA_EFFECT_AMBIENCE_ID, allAmbient);
        ci.cancel();
    }
}