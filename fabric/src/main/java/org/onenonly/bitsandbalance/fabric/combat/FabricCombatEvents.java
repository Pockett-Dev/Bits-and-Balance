package org.onenonly.bitsandbalance.fabric.combat;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.world.level.gamerules.GameRules;
import org.onenonly.bitsandbalance.fabric.config.FabricCombatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FabricCombatEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-combat-events");

    private FabricCombatEvents() {
    }

    public static void init() {
        // Use AFTER_RESPAWN so our changes apply after vanilla copies state (including tags).
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) {
                return; // only care about death respawns
            }

            // Maintain Experience: ensure no XP is retained on respawn (we drop the remainder on death).
            if (FabricCombatConfig.enableMaintainExperience) {
                boolean keepInventory = false;
                try {
                    keepInventory = newPlayer.level().getGameRules().get(GameRules.KEEP_INVENTORY);
                } catch (Throwable ignored) {
                    // ignore
                }

                if (!keepInventory) {
                    newPlayer.experienceLevel = 0;
                    newPlayer.experienceProgress = 0.0F;
                    newPlayer.totalExperience = 0;
                }
            }

            // Second Chance cooldown handling. Important: if resetOnRespawn is true,
            // always clear the tag so stale cooldowns can't survive toggles.
            if (FabricCombatConfig.secondChanceResetOnRespawn) {
                SecondChanceTags.clear(newPlayer);
                LOGGER.info("Second Chance cooldown cleared on respawn (resetOnRespawn=true)");
            } else if (FabricCombatConfig.enableSecondChance) {
                SecondChanceTags.copy(oldPlayer, newPlayer);
                LOGGER.info("Second Chance cooldown copied on respawn (resetOnRespawn=false)");
            }
        });
    }
}
