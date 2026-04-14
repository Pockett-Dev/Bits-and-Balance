package org.onenonly.bitsandbalance.fabric.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;

/**
 * Minimal Better Combat compatibility shim.
 *
 * This intentionally avoids compile-time dependencies on Better Combat.
 */
public final class FabricBetterCombatCompat {
    private static final String MOD_ID = "bettercombat";

    private FabricBetterCombatCompat() {
    }

    public static boolean isBetterCombatLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    /**
     * Best-effort handler for a "double damage" mounted phantom hit.
     *
     * If Better Combat is present, we still fall back to vanilla damage application to avoid
     * linking against Better Combat internals.
     */
    public static void handleMountedPhantomAttack(Player player, Phantom phantom) {
        if (player.level().isClientSide()) return;
        if (phantom == null || !phantom.isAlive()) return;

        // Use vanilla attack resolution so fists/weapons/enchantments/cooldown scale behave normally.
        // The "double damage while carried" rule is applied via our hurtServer hook when the attack
        // context indicates the player is attacking their carrying phantom.
        player.attack(phantom);
    }
}
