package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.onenonly.bitsandbalance.common.potions.BlindnessMobVision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class BlindnessMobTargetMixin {
    @Shadow private LivingEntity target;

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$rejectDistantTarget(LivingEntity target, CallbackInfo ci) {
        if (BlindnessMobVision.isOutOfRange((Mob) (Object) this, target)) {
            ci.cancel();
        }
    }

    @Inject(method = {"getTarget", "getTargetFromBrain"}, at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$hideDistantTarget(CallbackInfoReturnable<LivingEntity> cir) {
        if (BlindnessMobVision.isOutOfRange((Mob) (Object) this, cir.getReturnValue())) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "serverAiStep", at = {@At("HEAD"), @At("TAIL")})
    private void bitsandbalance$forgetDistantTarget(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!BlindnessMobVision.isActive(mob)) {
            return;
        }

        boolean forgotTarget = false;
        if (BlindnessMobVision.isOutOfRange(mob, this.target)) {
            mob.setTarget(null);
            forgotTarget = true;
        }
        var brain = mob.getBrain();
        var attackTarget = brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                ? brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) : null;
        if (BlindnessMobVision.isOutOfRange(mob, attackTarget)) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            forgotTarget = true;
        }
        if (forgotTarget) {
            mob.getNavigation().stop();
        }
    }
}
