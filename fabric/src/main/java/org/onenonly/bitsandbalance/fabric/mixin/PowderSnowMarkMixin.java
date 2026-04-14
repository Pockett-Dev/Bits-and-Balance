package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.fabric.combat.PowderSnowTags;
import org.onenonly.bitsandbalance.fabric.config.FabricCombatConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PowderSnowMarkMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void bitsandbalance$maintainPowderSnowMark(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!FabricCombatConfig.enableSnowballRework) return;

        long until = PowderSnowTags.getUntil(self);
        if (until <= 0) return;

        long now = self.level().getGameTime();
        if (now > until) return;

        try {
            int cap = self.getTicksRequiredToFreeze();
            int current = self.getTicksFrozen();
            if (current < cap) {
                self.setTicksFrozen(cap);
            }
        } catch (Throwable ignored) {
        }

        try {
            if (!self.level().isClientSide() && self.isAlive()) {
                int frozen = self.getTicksFrozen();
                int cap = self.getTicksRequiredToFreeze();
                if (frozen >= cap) {
                    long next = PowderSnowTags.getNextDamage(self);
                    if (now >= next) {
                        self.hurt(self.damageSources().freeze(), 0.5F);
                        PowderSnowTags.setNextDamage(self, now + 60L);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
