package org.onenonly.bitsandbalance.tweaks;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.onenonly.bitsandbalance.Config;
/**
 * Tweaks: Improved Recovery Compass
 * - Keep Recovery Compass in inventory when you die (configurable).
 * - Custom recipe with only 4 Echo Shards instead of 8 (handled via datapack).
 * Server-side only.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class ImprovedRecoveryCompass {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_KEY = "bitsandbalance_recovery_compasses";
    private static final int TRAIL_COOLDOWN_TICKS = 60;
    private static final int TRAIL_TRAVEL_TICKS = 100;
    private static final int TRAIL_HOVER_TICKS = 60;
    private static final int TRAIL_ARRIVAL_POOF_DELAY_TICKS = 10;
    private static final double TRAIL_SPEED_BLOCKS_PER_TICK = 0.34D;
    private static final double TRAIL_HOVER_HEIGHT = 1.35D;
    private static final double TRAIL_HOVER_BOB_AMPLITUDE = 0.12D;
    private static final double TRAIL_HOVER_BOB_SPEED = 0.28D;
    private static final Map<UUID, ActiveTrail> ACTIVE_TRAILS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> TRAIL_COOLDOWNS = new ConcurrentHashMap<>();

    // Capture recovery compasses before death processing clears inventory
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        
        if (!Config.enableImprovedRecoveryCompass) return;
        if (!Config.recoveryCompassKeepOnDeath) return;
        
        // Don't interfere with keepInventory gamerule
        if (shouldKeepInventory(player)) return;
        
        LOGGER.info("Player {} is dying - capturing recovery compasses before inventory clears", player.getName().getString());
        
        // Find and store recovery compasses in persistent data
        List<ItemStack> recoveryCompasses = new ArrayList<>();
        
        // Check main inventory (includes hotbar)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.RECOVERY_COMPASS) {
                recoveryCompasses.add(stack.copy());
                LOGGER.info("Captured recovery compass from slot {}: {}", i, stack.getDisplayName().getString());
            }
        }
        
        // Check offhand
        ItemStack offhandStack = player.getOffhandItem();
        if (offhandStack.getItem() == Items.RECOVERY_COMPASS) {
            recoveryCompasses.add(offhandStack.copy());
            LOGGER.info("Captured recovery compass from offhand: {}", offhandStack.getDisplayName().getString());
        }
        
        // Store in persistent data for restoration later
        if (!recoveryCompasses.isEmpty()) {
            storeRecoveryCompasses(player, recoveryCompasses);
            LOGGER.info("Stored {} recovery compass(es) in persistent data", recoveryCompasses.size());
        }
    }

    // Prevent recovery compasses from dropping naturally
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        
        if (!Config.enableImprovedRecoveryCompass) return;
        if (!Config.recoveryCompassKeepOnDeath) return;
        
        // Don't interfere with keepInventory gamerule
        if (shouldKeepInventory(player)) return;
        
        // Remove recovery compasses from drops since we'll restore them manually
        var drops = event.getDrops();
        boolean removedAny = drops.removeIf(itemEntity -> {
            ItemStack stack = itemEntity.getItem();
            boolean isRecoveryCompass = stack.getItem() == Items.RECOVERY_COMPASS;
            if (isRecoveryCompass) {
                LOGGER.info("Prevented recovery compass from dropping naturally: {}", stack.getDisplayName().getString());
            }
            return isRecoveryCompass;
        });
        
        if (removedAny) {
            LOGGER.info("Removed recovery compass(es) from natural drops to prevent duplication");
        }
    }

    // Restore recovery compasses from persistent data after respawn
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!Config.enableImprovedRecoveryCompass) return;
        if (!Config.recoveryCompassKeepOnDeath) return;
        if (!event.isWasDeath()) return;
        
        Player newPlayer = event.getEntity();
        Player originalPlayer = event.getOriginal();
        if (newPlayer.level().isClientSide()) return;
        
        LOGGER.info("Player {} respawned - checking for recovery compasses in original player data", newPlayer.getName().getString());
        
        // Copy recovery compass data from original player to new player
        CompoundTag originalData = originalPlayer.getPersistentData();
        if (originalData.contains(NBT_KEY)) {
            LOGGER.info("Found recovery compass data in original player - copying to new player");
            CompoundTag newData = newPlayer.getPersistentData();
            var compassData = originalData.get(NBT_KEY);
            if (compassData != null) {
                newData.put(NBT_KEY, compassData);
            }
        } else {
            LOGGER.info("No recovery compass data found in original player persistent data");
        }
        
        // Now restore from the new player's data
        restoreRecoveryCompasses(newPlayer);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!Config.enableImprovedRecoveryCompass) return;
        if (!Config.recoveryCompassSculkParticles) return;

        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!serverPlayer.isShiftKeyDown()) return;

        ItemStack item = event.getItemStack();
        if (item.getItem() != Items.RECOVERY_COMPASS) return;

        if (bitsandbalance$isOnTrailCooldown(serverPlayer)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (bitsandbalance$startSoulTrail(serverPlayer)) {
            bitsandbalance$setTrailCooldown(serverPlayer);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!Config.enableImprovedRecoveryCompass || !Config.recoveryCompassSculkParticles) {
            ACTIVE_TRAILS.clear();
            TRAIL_COOLDOWNS.clear();
            return;
        }

        if (!TRAIL_COOLDOWNS.isEmpty()) {
            TRAIL_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= 1);
            TRAIL_COOLDOWNS.replaceAll((uuid, ticks) -> ticks - 1);
        }

        if (ACTIVE_TRAILS.isEmpty()) {
            return;
        }

        ACTIVE_TRAILS.entrySet().removeIf(entry -> !entry.getValue().tick());
    }
    
    private static boolean shouldKeepInventory(Player player) {
        try {
            if (!(player.level() instanceof ServerLevel serverLevel)) return false;
            return serverLevel.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY);
        } catch (Throwable ignored) {
            return false;
        }
    }
    
    private static void storeRecoveryCompasses(Player player, List<ItemStack> compasses) {
        try {
            CompoundTag playerData = player.getPersistentData();
            ListTag compassList = new ListTag();
            
            for (ItemStack compass : compasses) {
                CompoundTag compassTag = new CompoundTag();
                // Use the legacy save method which includes the item ID
                compassTag.putString("id", "minecraft:recovery_compass");
                compassTag.putInt("count", compass.getCount());
                // Store basic item info only - components are complex in 1.21
                LOGGER.debug("Storing basic recovery compass (count: {})", compass.getCount());
                compassList.add(compassTag);
                LOGGER.debug("Stored compass NBT: {}", compassTag);
            }
            
            playerData.put(NBT_KEY, compassList);
        } catch (Throwable e) {
            LOGGER.warn("Failed to store recovery compasses for player {}: {}", 
                player.getName().getString(), e.getMessage());
        }
    }
    
    private static void restoreRecoveryCompasses(Player player) {
        try {
            CompoundTag playerData = player.getPersistentData();
            if (!playerData.contains(NBT_KEY)) {
                LOGGER.info("No recovery compasses in persistent data to restore");
                return;
            }
            
            ListTag compassList = playerData.getListOrEmpty(NBT_KEY);
            
            for (int i = 0; i < compassList.size(); i++) {
                CompoundTag compassTag = compassList.getCompoundOrEmpty(i);
                LOGGER.debug("Restoring compass from NBT: {}", compassTag);
                
                // Create a new recovery compass with the stored count
                int count = compassTag.getIntOr("count", 1);
                ItemStack compass = new ItemStack(Items.RECOVERY_COMPASS, count);
                
                // In 1.21, we just create a basic recovery compass
                // Components/NBT restoration is complex and not needed for basic functionality
                
                if (!compass.isEmpty()) {
                    if (!player.getInventory().add(compass)) {
                        // If inventory is full, drop at player location
                        player.drop(compass, false);
                        LOGGER.info("Inventory full, dropped recovery compass at player location");
                    } else {
                        LOGGER.info("Successfully restored recovery compass to inventory (count: {})", count);
                    }
                }
            }
            
            // Clean up persistent data
            playerData.remove(NBT_KEY);
            LOGGER.info("Restored {} recovery compass(es) and cleaned up persistent data", compassList.size());
            
        } catch (Throwable e) {
            LOGGER.warn("Failed to restore recovery compasses for player {}: {}", 
                player.getName().getString(), e.getMessage());
        }
    }

    private static boolean bitsandbalance$startSoulTrail(ServerPlayer player) {
        Optional<GlobalPos> lastDeathLocation = player.getLastDeathLocation();
        if (lastDeathLocation.isEmpty()) {
            player.sendSystemMessage(Component.literal("No death location found"), true);
            return false;
        }

        GlobalPos deathLocation = lastDeathLocation.get();
        ServerLevel level = (ServerLevel) player.level();
        if (!deathLocation.dimension().equals(level.dimension())) {
            player.sendSystemMessage(Component.literal("Your last death location is in another dimension"), true);
            return false;
        }

        BlockPos deathPos = deathLocation.pos();
        ACTIVE_TRAILS.put(player.getUUID(), new ActiveTrail(player, level, deathPos));
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.6F, 1.1F);
        return true;
    }

    private static boolean bitsandbalance$isOnTrailCooldown(ServerPlayer player) {
        Integer ticks = TRAIL_COOLDOWNS.get(player.getUUID());
        return ticks != null && ticks > 0;
    }

    private static void bitsandbalance$setTrailCooldown(ServerPlayer player) {
        TRAIL_COOLDOWNS.put(player.getUUID(), TRAIL_COOLDOWN_TICKS);
    }

    private record ActiveTrail(UUID playerId, ServerLevel level, Vec3State state, Vec3State hoverTarget, int travelTicksRemaining, int hoverTicksRemaining) {
        private ActiveTrail(ServerPlayer player, ServerLevel level, BlockPos deathPos) {
            this(
                player.getUUID(),
                level,
                Vec3State.from(player.getX(), player.getY() + player.getBbHeight() * 0.55D, player.getZ()),
                Vec3State.hoverTarget(deathPos),
                TRAIL_TRAVEL_TICKS,
                0
            );
        }

        private boolean tick() {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player == null || player.level() != level) {
                return false;
            }

            if (hoverTicksRemaining > 0) {
                int hoverAge = TRAIL_HOVER_TICKS - hoverTicksRemaining;
                Vec3State hoverState = hoverTarget.withYOffset(Math.sin(hoverAge * TRAIL_HOVER_BOB_SPEED) * TRAIL_HOVER_BOB_AMPLITUDE);
                bitsandbalance$sendHoverParticles(level, player, hoverState);

                if (hoverTicksRemaining <= 1) {
                    bitsandbalance$sendPoof(level, player, hoverState);
                    return false;
                }

                ACTIVE_TRAILS.put(playerId, new ActiveTrail(playerId, level, hoverTarget, hoverTarget, travelTicksRemaining, hoverTicksRemaining - 1));
                return true;
            }

            if (travelTicksRemaining <= 0) {
                bitsandbalance$sendPoof(level, player, state);
                return false;
            }

            Vec3State delta = hoverTarget.subtract(state);
            double distanceToTarget = delta.length();
            if (distanceToTarget <= TRAIL_SPEED_BLOCKS_PER_TICK) {
                bitsandbalance$sendHoverParticles(level, player, hoverTarget);
                ACTIVE_TRAILS.put(playerId, new ActiveTrail(playerId, level, hoverTarget, hoverTarget, travelTicksRemaining, TRAIL_HOVER_TICKS + TRAIL_ARRIVAL_POOF_DELAY_TICKS));
                return true;
            }

            Vec3State velocity = delta.normalize().scale(TRAIL_SPEED_BLOCKS_PER_TICK);
            Vec3State nextState = state.add(velocity);
            bitsandbalance$sendTrailParticles(level, player, nextState, velocity);
            ACTIVE_TRAILS.put(playerId, new ActiveTrail(playerId, level, nextState, hoverTarget, travelTicksRemaining - 1, 0));
            return true;
        }
    }

    private record Vec3State(double x, double y, double z) {
        private static Vec3State from(double x, double y, double z) {
            return new Vec3State(x, y, z);
        }

        private static Vec3State hoverTarget(BlockPos deathPos) {
            return new Vec3State(deathPos.getX() + 0.5D, deathPos.getY() + TRAIL_HOVER_HEIGHT, deathPos.getZ() + 0.5D);
        }

        private Vec3State add(Vec3State other) {
            return new Vec3State(x + other.x, y + other.y, z + other.z);
        }

        private Vec3State subtract(Vec3State other) {
            return new Vec3State(x - other.x, y - other.y, z - other.z);
        }

        private double length() {
            return Math.sqrt(x * x + y * y + z * z);
        }

        private Vec3State normalize() {
            double length = length();
            if (length < 1.0E-4D) {
                return new Vec3State(0.0D, 0.0D, 0.0D);
            }
            return new Vec3State(x / length, y / length, z / length);
        }

        private Vec3State scale(double scale) {
            return new Vec3State(x * scale, y * scale, z * scale);
        }

        private Vec3State withYOffset(double yOffset) {
            return new Vec3State(x, y + yOffset, z);
        }
    }

    private static void bitsandbalance$sendTrailParticles(ServerLevel level, ServerPlayer player, Vec3State state, Vec3State velocity) {
        level.sendParticles(player, ParticleTypes.SOUL, true, true, state.x, state.y, state.z, 2, 0.12D, 0.12D, 0.12D, 0.008D);
        level.sendParticles(player, ParticleTypes.SOUL_FIRE_FLAME, true, true, state.x, state.y, state.z, 1, 0.07D, 0.07D, 0.07D, 0.004D);
    }

    private static void bitsandbalance$sendHoverParticles(ServerLevel level, ServerPlayer player, Vec3State state) {
        level.sendParticles(player, ParticleTypes.SOUL, true, true, state.x, state.y, state.z, 1, 0.08D, 0.12D, 0.08D, 0.006D);
        level.sendParticles(player, ParticleTypes.SOUL_FIRE_FLAME, true, true, state.x, state.y, state.z, 1, 0.04D, 0.08D, 0.04D, 0.002D);
    }

    private static void bitsandbalance$sendPoof(ServerLevel level, ServerPlayer player, Vec3State state) {
        level.sendParticles(player, ParticleTypes.SOUL, true, true, state.x, state.y, state.z, 18, 0.30D, 0.24D, 0.30D, 0.075D);
        level.sendParticles(player, ParticleTypes.SOUL_FIRE_FLAME, true, true, state.x, state.y, state.z, 9, 0.18D, 0.18D, 0.18D, 0.045D);
        level.sendParticles(player, ParticleTypes.SOUL, true, true, state.x, state.y, state.z, 6, 0.10D, 0.10D, 0.10D, 0.0D);
    }
}
