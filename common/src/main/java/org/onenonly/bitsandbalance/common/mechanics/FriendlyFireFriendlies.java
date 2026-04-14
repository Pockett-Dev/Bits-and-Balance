package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Shared helper for the "Friendly Fire Friendlies" mechanic.
 *
 * This mechanic prevents players from accidentally melee-attacking their own tamed pets.
 */
public final class FriendlyFireFriendlies {
    private FriendlyFireFriendlies() {
    }

    public static boolean isOwnedTamedPet(Player player, Entity target) {
        if (player == null || target == null) return false;
        if (target == player) return false;
        if (!(target instanceof LivingEntity)) return false;

        try {
            if (target instanceof TamableAnimal tamable) {
                return tamable.isTame() && tamable.isOwnedBy(player);
            }
        } catch (Throwable ignored) {
            // Defensive: if mappings/behavior change, treat as not protected.
        }

        try {
            if (target instanceof AbstractHorse horse) {
                if (!horse.isTamed()) return false;
                var ownerRef = horse.getOwnerReference();
                return ownerRef != null && ownerRef.matches(player);
            }
        } catch (Throwable ignored) {
        }

        return false;
    }
}
