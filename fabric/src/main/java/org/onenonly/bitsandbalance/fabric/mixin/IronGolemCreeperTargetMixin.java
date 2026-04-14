package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import org.onenonly.bitsandbalance.fabric.config.FabricMobsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronGolem.class)
public abstract class IronGolemCreeperTargetMixin {
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void bitsandbalance$addCreeperTarget(CallbackInfo ci) {
        if (!FabricMobsConfig.enableIronGolemsKillCreepers) return;

		IronGolem self = (IronGolem) (Object) this;
		((MobTargetSelectorAccessor) self).bitsandbalance$getTargetSelector().addGoal(3, new NearestAttackableTargetGoal<>(self, Creeper.class, true));
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$allowCreeperTargets(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricMobsConfig.enableIronGolemsKillCreepers) return;
        if (target instanceof Creeper) {
            cir.setReturnValue(true);
        }
    }
}
