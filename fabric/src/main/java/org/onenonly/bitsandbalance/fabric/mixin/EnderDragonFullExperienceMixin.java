package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragon.class)
public abstract class EnderDragonFullExperienceMixin {
	@Shadow
	private int dragonDeathTime;

	@Shadow
	private EndDragonFight dragonFight;

	@Unique
	private Vec3 bitsandbalance$dragonDeathPos;

	@Unique
	private static final int bitsandbalance$DRAGON_DEATH_TICKS = 200;

	@Inject(method = "tickDeath", at = @At("TAIL"), require = 0)
	private void bitsandbalance$grantFullRespawnedDragonXp(CallbackInfo ci) {
		if (!FabricBalanceConfig.enableFullEnderDragonXp) {
			return;
		}

		// Only run once, after the death animation completes.
		if (this.dragonDeathTime < bitsandbalance$DRAGON_DEATH_TICKS) {
			return;
		}

		EnderDragon self = (EnderDragon) (Object) this;
		if (!(self.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		// Track the last known position during the death sequence; we'll award XP at the end.
		bitsandbalance$dragonDeathPos = self.position();

		if (this.dragonFight == null) {
			return;
		}

		// Vanilla: first kill = 12000 XP, respawned kills = 500 XP.
		// We top up respawned kills to match the first-kill total.
		if (this.dragonFight.hasPreviouslyKilledDragon()) {
			int extra = 12000 - 500;
			if (extra > 0) {
				Vec3 xpPos = bitsandbalance$dragonDeathPos != null ? bitsandbalance$dragonDeathPos : self.position();
				ExperienceOrb.award(serverLevel, xpPos, extra);
			}
		}
	}
}
