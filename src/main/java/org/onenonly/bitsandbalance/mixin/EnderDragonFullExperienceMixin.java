package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.onenonly.bitsandbalance.common.config.GameplayFeatureFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EnderDragon.class)
public abstract class EnderDragonFullExperienceMixin {
	@ModifyArg(
			method = "tickDeath",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"),
			index = 2,
			require = 0
	)
	private int bitsandbalance$useFullEnderDragonXp(int xp) {
		if (!GameplayFeatureFlags.fullEnderDragonXpEnabled()) {
			return xp;
		}

		return xp < 12000 ? 12000 : xp;
	}
}
