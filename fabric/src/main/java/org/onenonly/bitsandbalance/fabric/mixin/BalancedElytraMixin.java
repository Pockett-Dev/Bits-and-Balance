package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Balance: Balanced Elytra
 *
 * Elytras lose extra durability based on distance flown.
 */
@Mixin(LivingEntity.class)
public abstract class BalancedElytraMixin {
    private static final Map<UUID, Vec3> LAST_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> DISTANCE_BUFFER = new ConcurrentHashMap<>();

    @Redirect(
        method = "updateFallFlying",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"
        ),
        require = 0
    )
    private void bitsandbalance$balancedElytra$redirectVanillaDamage(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot) {
        // Only suppress vanilla's elytra durability drain; leave other (unlikely) travel-time damage alone.
        if (FabricBalanceConfig.enableBalancedElytra
            && slot == EquipmentSlot.CHEST
            && entity instanceof Player
            && entity.isFallFlying()
            && stack != null
            && stack.is(Items.ELYTRA)) {
            return;
        }

        stack.hurtAndBreak(amount, entity, slot);
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void bitsandbalance$balancedElytraDurability(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!FabricBalanceConfig.enableBalancedElytra) return;
        if (!(self instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (player.getAbilities().instabuild) return;

        UUID id = player.getUUID();
        if (!self.isFallFlying()) {
            LAST_POS.remove(id);
            DISTANCE_BUFFER.remove(id);
            return;
        }

        ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestItem.isEmpty() || !chestItem.is(Items.ELYTRA)) {
            LAST_POS.remove(id);
            DISTANCE_BUFFER.remove(id);
            return;
        }

        Vec3 nowPos = player.position();
        Vec3 prev = LAST_POS.put(id, nowPos);
        if (prev != null) {
            double dx = nowPos.x - prev.x;
            double dz = nowPos.z - prev.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0 && Double.isFinite(dist)) {
                DISTANCE_BUFFER.merge(id, dist, Double::sum);
            }
        }

        int blocksPerDurability = FabricBalanceConfig.balancedElytraBlocksPerDurability;
        if (blocksPerDurability <= 0) blocksPerDurability = 10;

        double buffer = DISTANCE_BUFFER.getOrDefault(id, 0.0D);
        if (!Double.isFinite(buffer) || buffer <= 0) {
            DISTANCE_BUFFER.put(id, 0.0D);
            return;
        }

        int damage = (int) Math.floor(buffer / (double) blocksPerDurability);
        if (damage <= 0) return;

        DISTANCE_BUFFER.put(id, buffer - (damage * (double) blocksPerDurability));
        chestItem.hurtAndBreak(damage, player, EquipmentSlot.CHEST);
    }
}
