package org.onenonly.bitsandbalance.mobs;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.spider.Spider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

/**
 * Handles the replacement of regular Spiders with Cave Spiders in underground areas.
 * This maintains mob caps, group sizes, and biome rules while providing a configurable
 * chance for replacement when spiders spawn naturally underground.
 */
public final class SpawnSwapHandler {

    /**
     * Event handler for the FinalizeSpawnEvent.
     * Intercepts natural Spider spawns and replaces them with Cave Spiders
     * when the spawn occurs underground (no sky access) based on configuration.
     */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        // Only process if cave spider replacement is enabled
        if (!Config.enableCaveSpidersInCaves) {
            return;
        }

        // Only when a Spider is about to spawn
        if (!(event.getEntity() instanceof Spider)) {
            return;
        }

        // Only natural spawns (don't touch spawners, eggs, commands)
        if (event.getSpawnType() != EntitySpawnReason.NATURAL) {
            return;
        }

        // We need a ServerLevel to spawn the replacement
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // Keep this to Overworld caves, so we don't affect Nether/End
        ResourceKey<Level> dimension = level.dimension();
        if (!dimension.equals(Level.OVERWORLD)) {
            return;
        }

        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());

        // "Cave" heuristic: not open to the sky
        if (level.canSeeSkyFromBelowWater(pos)) {
            return;
        }

        // Check replacement chance
        if (level.random.nextDouble() >= Config.caveSpiderReplacementChance) {
            return;
        }


        // Cancel the spider spawn, then spawn a cave spider in its place
        event.setSpawnCancelled(true);

        // Using EntityType#spawn is recommended for living entities; it fires the proper spawn events
        EntityType.CAVE_SPIDER.spawn(
                level,
                pos,
                event.getSpawnType()   // keep the same spawn reason (NATURAL)
        );
    }
}
