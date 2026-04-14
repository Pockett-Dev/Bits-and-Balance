package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.onenonly.bitsandbalance.BitsAndBalance;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.onenonly.bitsandbalance.Config;
/**
 * Tweaks: Creepers Burn in Sunlight
 * - Creepers burn when in direct sunlight during the day, just like Zombies and Skeletons.
 * Server-side only.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class CreeperSunlightBurn {
    
    // Track last fire application time for each creeper to prevent over-application
    private static final Map<UUID, Long> lastFireApplication = new HashMap<>();
    private static final int FIRE_APPLICATION_COOLDOWN = 80; // 4 seconds cooldown

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!Config.enableCreeperSunlightBurn) return;

        if (!(event.getEntity() instanceof Creeper creeper)) return;

        Level level = creeper.level();
        if (level.isClientSide()) return;

        // Check if the creeper should burn in sunlight (similar to zombies/skeletons)
        if (shouldBurnInSunlight(creeper, level)) {
            UUID creeperId = creeper.getUUID();
            long currentTick = level.getGameTime();
            
            // Only apply fire if enough time has passed since last application
            // This prevents constant fire refresh and matches vanilla behavior
            Long lastApplication = lastFireApplication.get(creeperId);
            if (lastApplication == null || currentTick - lastApplication >= FIRE_APPLICATION_COOLDOWN) {
                creeper.setRemainingFireTicks(160); // 8 seconds like vanilla zombies/skeletons
                lastFireApplication.put(creeperId, currentTick);
            }
        } else {
            // Clean up tracking when not in sunlight
            lastFireApplication.remove(creeper.getUUID());
        }
    }

    private static boolean shouldBurnInSunlight(Creeper creeper, Level level) {
        int timeOfDay = (int) (level.getOverworldClockTime() % 24000L);
        if (timeOfDay >= 12000) return false;

        // Check if creeper can see the sky
        if (!level.canSeeSky(creeper.blockPosition())) return false;

        // Check if it's raining - creepers shouldn't burn in rain
        if (level.isRaining()) return false;

        // Check light level (must be at least 15 for burning)
        int lightLevel = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, creeper.blockPosition());
        return lightLevel >= 15;
    }
}
