package org.onenonly.bitsandbalance.mixin;


import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the vanilla Night Vision blink with a smooth fade-out near expiration.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F", at = @At("HEAD"), cancellable = true)
    private static void rebalance$nightVisionFade(LivingEntity living, float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (!Config.enableNightVisionFade) return;
        if (living == null) return;

        MobEffectInstance effect = living.getEffect(MobEffects.NIGHT_VISION);
        if (effect == null) return;

        if (effect.isInfiniteDuration()) {
            cir.setReturnValue(1.0f);
            return;
        }

        int duration = effect.getDuration();
        if (duration <= 0) {
            // Ensure no last-frame flash when the effect hits 0 duration
            cir.setReturnValue(0.0f);
            return;
        }

        int fadeTicks = Math.max(1, Config.nightVisionFadeSeconds * 20);
        float remainingTicks = duration - partialTicks;
        float scale = effect.endsWithin(fadeTicks) ? (remainingTicks / (float) fadeTicks) : 1.0f;
        if (scale < 0.0f) scale = 0.0f;
        if (scale > 1.0f) scale = 1.0f;

        cir.setReturnValue(scale);
    }
}
