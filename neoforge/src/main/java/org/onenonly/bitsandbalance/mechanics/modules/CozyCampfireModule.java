package org.onenonly.bitsandbalance.mechanics.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.mechanics.FeatureModule;

/**
 * Cozy Campfire Module
 * 
 * Provides regeneration effects to players and optionally bees when they are near lit campfires.
 * 
 * Features:
 * - Configurable range around campfires
 * - Configurable regeneration duration and amplifier
 * - Configurable tick interval for performance
 * - Optional bee support (can be toggled separately)
 * - Only affects lit campfires
 * - Server-side only for performance
 * 
 * This creates a cozy atmosphere where campfires provide healing benefits,
 * encouraging players to set up camps and rest areas.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class CozyCampfireModule implements FeatureModule {
    
    @Override
    public String getFeatureName() {
        return "Cozy Campfire";
    }
    
    @Override
    public boolean isEnabled() {
        return Config.enableCozyCampfire;
    }
    
    @Override
    public int getInitializationPriority() {
        return 400; // Medium-low priority - environmental effect
    }
    
    /**
     * Provides regeneration effect to players near lit campfires
     */
    @SubscribeEvent
    public static void onCozyCampfire(PlayerTickEvent.Post event) {
        if (!Config.enableCozyCampfire) return;
        
        var entity = event.getEntity();
        if (!(entity instanceof ServerPlayer sp)) return;
        if (sp.tickCount % Config.cozyCampfireIntervalTicks != 0) return;
        
        if (isNearLitCampfire(sp.blockPosition(), sp.level()::getBlockState, sp::distanceToSqr)) {
            applyRegenerationEffect(sp);
        }
    }
    
    /**
     * Provides regeneration effect to bees near lit campfires (if enabled)
     */
    @SubscribeEvent
    public static void onCozyCampfireBees(EntityTickEvent.Post event) {
        if (!Config.enableCozyCampfire || !Config.cozyCampfireAffectBees) return;
        
        var entity = event.getEntity();
        if (!(entity instanceof Bee bee)) return;
        if (bee.level().isClientSide()) return;
        if (bee.tickCount % Config.cozyCampfireIntervalTicks != 0) return;
        
        if (isNearLitCampfire(bee.blockPosition(), bee.level()::getBlockState, bee::distanceToSqr)) {
            applyRegenerationEffect(bee);
        }
    }
    
    /**
     * Checks if an entity is near a lit campfire within the configured range
     */
    private static boolean isNearLitCampfire(BlockPos center, BlockStateProvider blockStateProvider, DistanceCalculator distanceCalculator) {
        try {
            int range = (int) Math.ceil(Config.cozyCampfireRange);
            double rangeSq = Config.cozyCampfireRange * Config.cozyCampfireRange;
            
            for (int x = -range; x <= range; x++) {
                for (int y = -range; y <= range; y++) {
                    for (int z = -range; z <= range; z++) {
                        BlockPos pos = center.offset(x, y, z);
                        
                        // Check distance first for performance
                        if (distanceCalculator.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > rangeSq) {
                            continue;
                        }
                        
                        try {
                            BlockState state = blockStateProvider.getBlockState(pos);
                            if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
                                return true;
                            }
                        } catch (Throwable ignored) {
                            // Continue checking other blocks if one fails
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
        
        return false;
    }
    
    /**
     * Applies the regeneration effect to an entity
     */
    private static void applyRegenerationEffect(net.minecraft.world.entity.LivingEntity entity) {
        try {
            int duration = Config.cozyCampfireDurationTicks;
            int amplifier = Config.cozyCampfireAmplifier;
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier));
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }
    
    /**
     * Functional interface for getting block states (allows for easy testing/mocking)
     */
    @FunctionalInterface
    private interface BlockStateProvider {
        BlockState getBlockState(BlockPos pos);
    }
    
    /**
     * Functional interface for distance calculations (allows for easy testing/mocking)
     */
    @FunctionalInterface
    private interface DistanceCalculator {
        double distanceToSqr(double x, double y, double z);
    }
}
