package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.fabric.config.FabricMobsConfig;
import org.onenonly.bitsandbalance.fabric.mobs.ImprovedPhantomCarryHelper;
import org.onenonly.bitsandbalance.fabric.mobs.MountedPhantomAttackContext;
import org.onenonly.bitsandbalance.fabric.mobs.PhantomDismountTracker;
import org.onenonly.bitsandbalance.fabric.mobs.PhantomPickupTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerAttackPhantomContextMixin {

	@Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
	private void bitsandbalance$markMountedPhantomAttackHead(Entity target, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (self.level().isClientSide()) return;
		if (!(target instanceof Phantom phantom)) return;

		boolean isCarriedByThisPhantom = phantom.hasPassenger(self);

		if (!isCarriedByThisPhantom) {
			try {
				PhantomDismountTracker tracker = (PhantomDismountTracker) (Object) phantom;
				var lastRider = tracker.bitsandbalance$getLastRider();
				long lastDismountTime = tracker.bitsandbalance$getLastDismountTime();
				if (lastRider != null && lastDismountTime >= 0L && lastRider.equals(self.getUUID())) {
					long now = phantom.level().getGameTime();
					long ticksSinceDismount = now - lastDismountTime;
					if (ticksSinceDismount >= 0L && ticksSinceDismount <= 10L) {
						isCarriedByThisPhantom = true;
					}
				}
			} catch (Throwable ignored) {
			}
		}

		if (isCarriedByThisPhantom) {
			if (FabricMobsConfig.enableImprovedPhantoms && self instanceof PhantomPickupTracker tracker) {
				tracker.bitsandbalance$setPhantomHitDuringCarry(true);
				if (FabricMobsConfig.enablePhantomDropOnHit) {
					ImprovedPhantomCarryHelper.forceDrop(self, phantom, tracker, true);
				}
			}
			MountedPhantomAttackContext.push(phantom.getUUID());
		}
	}

	@Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
	private void bitsandbalance$markMountedPhantomAttackTail(Entity target, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (self.level().isClientSide()) return;
		if (target instanceof Phantom phantom) {
			MountedPhantomAttackContext.popIfMatches(phantom.getUUID());
		}
	}
}
