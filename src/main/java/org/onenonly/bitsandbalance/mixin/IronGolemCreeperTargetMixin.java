package org.onenonly.bitsandbalance.mixin;

//? if mc != 26.1 && mc != 26.2 && mc != 26.3
import net.minecraft.world.entity.EntityType;
//? else
import net.minecraft.world.entity.LivingEntity;
//? endif
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
//? if mc == 1.21.1
import net.minecraft.world.entity.animal.IronGolem;
//? else
import net.minecraft.world.entity.animal.golem.IronGolem;
//? endif
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

        golem.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(golem, Creeper.class, true));
    }

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$allowAttackingCreepers(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableIronGolemsKillCreepers) return;

        if (target instanceof Creeper) {
            cir.setReturnValue(true);
        }
    }
    //? else
    @Inject(method = "canAttackType", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$allowAttackingCreepers(EntityType<?> type, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableIronGolemsKillCreepers) return;

        if (type == EntityType.CREEPER) {
            cir.setReturnValue(true);
        }
    }
    //? endif
}
