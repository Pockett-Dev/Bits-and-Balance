package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.tweaks.ReturnToKillerTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityReturnToKillerMixin {
    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void bitsandbalance$returnToKiller(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        if (!FabricTweaksConfig.enableReturnToKiller) return;

        LivingEntity self = (LivingEntity) (Object) this;

        Entity killerEntity = damageSource.getEntity();
        if (!(killerEntity instanceof Player killer)) return;

        // Airborne kills only (keeps the feature focused on hard-to-reach drops).
        // Do not require a height delta: many flying mobs swoop near the player.
        if (self.onGround()) return;

        UUID killerUuid = killer.getUUID();
        long expiresAt = level.getGameTime() + 20L * 15L;

        // Allow a slightly larger radius and spawn window to catch drops that appear a few ticks late
        // or get initial motion away from the corpse.
        AABB searchBox = self.getBoundingBox().inflate(4.0D);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchBox, e -> e.tickCount <= 10);
        for (ItemEntity itemEntity : items) {
            ReturnToKillerTags.set(itemEntity, killerUuid.toString(), expiresAt);
        }

        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, searchBox, e -> e.tickCount <= 10);
        for (ExperienceOrb orb : orbs) {
            ReturnToKillerTags.set(orb, killerUuid.toString(), expiresAt);
        }
    }
}
