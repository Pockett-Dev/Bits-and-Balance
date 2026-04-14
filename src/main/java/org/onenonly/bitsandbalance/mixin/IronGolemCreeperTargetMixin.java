package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import org.onenonly.bitsandbalance.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronGolem.class)
public abstract class IronGolemCreeperTargetMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void bitsandbalance$targetCreepers(CallbackInfo ci) {
        if (!Config.enableIronGolemsKillCreepers) return;

        IronGolem golem = (IronGolem) (Object) this;

        // Priority 3 matches the golem's other "seek enemies" target goals.
        // Vanilla explicitly excludes Creepers; we add them back as a dedicated target.
        golem.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(golem, Creeper.class, true));
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$allowAttackingCreepers(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableIronGolemsKillCreepers) return;

        if (target instanceof Creeper) {
            cir.setReturnValue(true);
        }
    }
}
