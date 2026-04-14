package org.onenonly.bitsandbalance.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.onenonly.bitsandbalance.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityReturnToKillerMixin {
    private static final String KEY_KILLER_UUID = "bitsandbalance:return_to_killer_uuid";
    private static final String KEY_EXPIRES_AT = "bitsandbalance:return_to_killer_expires";

    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void bitsandbalance$returnToKiller(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        if (!Config.enableReturnToKiller) return;

        LivingEntity self = (LivingEntity) (Object) this;

        Entity killerEntity = damageSource.getEntity();
        if (!(killerEntity instanceof Player killer)) return;

        // Airborne kills only (keeps the feature focused on hard-to-reach drops).
        // Do not require a height delta: many flying mobs swoop near the player.
        if (self.onGround()) return;

        UUID killerUuid = killer.getUUID();
        long expiresAt = level.getGameTime() + 20L * 15L; // 15 seconds

        // Allow a slightly larger radius and spawn window to catch drops that appear a few ticks late
        // or get initial motion away from the corpse.
        AABB searchBox = self.getBoundingBox().inflate(4.0D);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchBox, e -> e.tickCount <= 10);
        for (ItemEntity itemEntity : items) {
            var data = itemEntity.getPersistentData();
            String existing = data.getString(KEY_KILLER_UUID).orElse("");
            if (!existing.isEmpty()) continue;
            data.putString(KEY_KILLER_UUID, killerUuid.toString());
            data.putLong(KEY_EXPIRES_AT, expiresAt);
        }

        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, searchBox, e -> e.tickCount <= 10);
        for (ExperienceOrb orb : orbs) {
            var data = orb.getPersistentData();
            String existing = data.getString(KEY_KILLER_UUID).orElse("");
            if (!existing.isEmpty()) continue;
            data.putString(KEY_KILLER_UUID, killerUuid.toString());
            data.putLong(KEY_EXPIRES_AT, expiresAt);
        }
    }
}
