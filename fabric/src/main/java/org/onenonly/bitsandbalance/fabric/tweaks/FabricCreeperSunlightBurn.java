package org.onenonly.bitsandbalance.fabric.tweaks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fabric port: Creepers Burn in Sunlight
 * Creepers burn when in direct sunlight during the day, just like Zombies and Skeletons.
 */
public final class FabricCreeperSunlightBurn {
    private FabricCreeperSunlightBurn() {
    }

    private static final Map<UUID, Long> lastFireApplication = new HashMap<>();
    private static final int FIRE_APPLICATION_COOLDOWN = 80; // 4 seconds

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!FabricTweaksConfig.enableCreeperSunlightBurn) return;
            
            for (var level : server.getAllLevels()) {
                for (var entity : level.getAllEntities()) {
                    if (entity instanceof Creeper creeper) {
                        tickCreeper(creeper, level);
                    }
                }
            }
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof Creeper) {
                lastFireApplication.remove(entity.getUUID());
            }
        });
    }

    private static void tickCreeper(Creeper creeper, Level level) {
        if (shouldBurnInSunlight(creeper, level)) {
            UUID creeperId = creeper.getUUID();
            long currentTick = level.getGameTime();

            Long lastApplication = lastFireApplication.get(creeperId);
            if (lastApplication == null || currentTick - lastApplication >= FIRE_APPLICATION_COOLDOWN) {
                creeper.setRemainingFireTicks(160); // 8 seconds
                lastFireApplication.put(creeperId, currentTick);
            }
        } else {
            lastFireApplication.remove(creeper.getUUID());
        }
    }

    private static boolean shouldBurnInSunlight(Creeper creeper, Level level) {
        int timeOfDay = (int) (level.getDayTime() % 24000L);
        if (timeOfDay >= 12000) return false;

        if (!level.canSeeSky(creeper.blockPosition())) return false;

        if (level.isRaining()) return false;

        int lightLevel = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, creeper.blockPosition());
        return lightLevel >= 15;
    }
}
