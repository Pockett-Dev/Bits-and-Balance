package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.combat.PowderSnowMarks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PowderSnowMarkMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void rebalance$maintainPowderSnowMark(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!Config.enableSnowballRework) return;

        long until = PowderSnowMarks.getUntil(self);
        if (until <= 0) return;
        long now = self.level().getGameTime();
        if (now <= until) {
            try {
                //? if mc != 26.1 && mc != 26.2 && mc != 26.3
                self.setIsInPowderSnow(true);
                //? endif
                int cap = self.getTicksRequiredToFreeze();
                int current = self.getTicksFrozen();
                if (current < cap) {
                    self.setTicksFrozen(cap);
                }
            } catch (Throwable ignored) {
                // Mapping safety
            }

            try {
                if (!self.level().isClientSide() && self.isAlive()) {
                    int frozen = self.getTicksFrozen();
                    int cap = self.getTicksRequiredToFreeze();
                    if (frozen >= cap) {
                        long next = PowderSnowMarks.getNextDamage(self);
                        if (now >= next) {
                            self.hurt(self.damageSources().freeze(), 0.5F);
                            PowderSnowMarks.setNextDamage(self, now + 60L);
                        }
                    }
                }
            } catch (Throwable ignored) {
                // Safety against mapping differences
            }
        } else {
            PowderSnowMarks.clear(self);
        }
    }
}
