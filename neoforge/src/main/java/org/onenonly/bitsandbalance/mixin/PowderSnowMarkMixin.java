package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.onenonly.bitsandbalance.Config;
@Mixin(LivingEntity.class)
public abstract class PowderSnowMarkMixin {
    private static final String NBT_KEY_UNTIL = "bitsandbalance_powdersnow_mark_until";
    private static final String NBT_KEY_NEXT_DMG = "bitsandbalance_powdersnow_next_dmg";

    @Inject(method = "tick", at = @At("HEAD"))
    private void rebalance$maintainPowderSnowMark(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!Config.enableSnowballRework) return;
        long until = self.getPersistentData().getLong(NBT_KEY_UNTIL).orElse(0L);
        if (until <= 0) return;
        long now = self.level().getGameTime();
        if (now <= until) {
            try {
                // Keep entity flagged as in powder snow so vanilla visuals/logic apply
                self.setIsInPowderSnow(true);
                // Ensure the entity remains fully frozen for the duration
                int cap = self.getTicksRequiredToFreeze();
                int current = self.getTicksFrozen();
                if (current < cap) {
                    self.setTicksFrozen(cap);
                }
            } catch (Throwable ignored) {
                // Mapping safety
            }

            // Apply periodic FREEZE damage server-side while fully frozen
            try {
                if (!self.level().isClientSide() && self.isAlive()) {
                    int frozen = self.getTicksFrozen();
                    int cap = self.getTicksRequiredToFreeze();
                    if (frozen >= cap) {
                        long next = self.getPersistentData().getLong(NBT_KEY_NEXT_DMG).orElse(0L);
                        if (now >= next) {
                            // Deal 0.5 damage every 3 seconds while "submerged"
                            self.hurt(self.damageSources().freeze(), 0.5F);
                            self.getPersistentData().putLong(NBT_KEY_NEXT_DMG, now + 60L);
                        }
                    }
                }
            } catch (Throwable ignored) {
                // Safety against mapping differences
            }
        }
    }
}
