package org.onenonly.bitsandbalance.fabric.tweaks;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric port of Tweaks: Improved Recovery Compass
 * - Keep Recovery Compass in inventory when you die (configurable).
 * - Custom recipe with only 4 Echo Shards instead of 8 (handled via datapack).
 * Server-side only.
 */
public class FabricImprovedRecoveryCompass {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TRAIL_COOLDOWN_TICKS = 60;
    private static final int TRAIL_TRAVEL_TICKS = 100;
    private static final int TRAIL_HOVER_TICKS = 60;
    private static final int TRAIL_ARRIVAL_POOF_DELAY_TICKS = 10;
    private static final double TRAIL_SPEED_BLOCKS_PER_TICK = 0.34D;
    private static final double TRAIL_HOVER_HEIGHT = 1.35D;
    private static final double TRAIL_HOVER_BOB_AMPLITUDE = 0.12D;
    private static final double TRAIL_HOVER_BOB_SPEED = 0.28D;

    private static final Map<UUID, List<ItemStack>> PENDING_RESTORE = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveTrail> ACTIVE_TRAILS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> TRAIL_COOLDOWNS = new ConcurrentHashMap<>();

    public static void init() {
        // Capture recovery compasses before death processing clears inventory
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity.level().isClientSide()) return true;
            if (!(entity instanceof ServerPlayer player)) return true;
            
            if (!FabricTweaksConfig.enableImprovedRecoveryCompass) return true;
            if (!FabricTweaksConfig.recoveryCompassKeepOnDeath) return true;
            
            // Don't interfere with keepInventory gamerule
            if (shouldKeepInventory(player)) return true;
            
            List<ItemStack> recoveryCompasses = removeRecoveryCompasses(player);
            if (!recoveryCompasses.isEmpty()) {
                PENDING_RESTORE.put(player.getUUID(), recoveryCompasses);
            }
            
            return true; // Allow death to proceed
        });

        // Restore recovery compasses after respawn
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!FabricTweaksConfig.enableImprovedRecoveryCompass) return;
            if (!FabricTweaksConfig.recoveryCompassKeepOnDeath) return;
            
            // Don't interfere with keepInventory gamerule
            if (shouldKeepInventory(newPlayer)) return;
            
            List<ItemStack> storedCompasses = PENDING_RESTORE.remove(newPlayer.getUUID());
            if (storedCompasses == null || storedCompasses.isEmpty()) return;

            for (ItemStack compass : storedCompasses) {
                boolean added = newPlayer.getInventory().add(compass);
                if (!added) {
                    newPlayer.drop(compass, false);
                }
            }
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!FabricTweaksConfig.enableImprovedRecoveryCompass) return InteractionResult.PASS;
            if (!FabricTweaksConfig.recoveryCompassSculkParticles) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!serverPlayer.isShiftKeyDown()) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!bitsandbalance$isRecoveryCompass(stack)) return InteractionResult.PASS;

            if (bitsandbalance$isOnTrailCooldown(serverPlayer)) {
                return InteractionResult.CONSUME;
            }

            return bitsandbalance$startSoulTrail(serverPlayer) ? InteractionResult.CONSUME : InteractionResult.CONSUME;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!FabricTweaksConfig.enableImprovedRecoveryCompass || !FabricTweaksConfig.recoveryCompassSculkParticles) {
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
        });
    }

    private static List<ItemStack> removeRecoveryCompasses(ServerPlayer player) {
        List<ItemStack> recovered = new ArrayList<>();

        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.RECOVERY_COMPASS)) {
                recovered.add(stack.copy());
                inv.setItem(i, ItemStack.EMPTY);
            }
        }

        ItemStack offhandStack = player.getOffhandItem();
        if (offhandStack.is(Items.RECOVERY_COMPASS)) {
            recovered.add(offhandStack.copy());
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }

        return recovered;
    }

    private static boolean shouldKeepInventory(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getGameRules().get(GameRules.KEEP_INVENTORY);
        }
        return false;
    }

    private static boolean bitsandbalance$isRecoveryCompass(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == Items.RECOVERY_COMPASS;
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

        ACTIVE_TRAILS.put(player.getUUID(), new ActiveTrail(player, level, deathLocation.pos()));
        TRAIL_COOLDOWNS.put(player.getUUID(), TRAIL_COOLDOWN_TICKS);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.6F, 1.1F);
        return true;
    }

    private static boolean bitsandbalance$isOnTrailCooldown(ServerPlayer player) {
        Integer ticks = TRAIL_COOLDOWNS.get(player.getUUID());
        return ticks != null && ticks > 0;
    }

    private record ActiveTrail(UUID playerId, ServerLevel level, Vec3State state, Vec3State hoverTarget, int travelTicksRemaining, int hoverTicksRemaining, int arrivalDelayTicksRemaining) {
        private ActiveTrail(ServerPlayer player, ServerLevel level, BlockPos deathPos) {
            this(
                    player.getUUID(),
                    level,
                    Vec3State.from(player.getX(), player.getY() + player.getBbHeight() * 0.55D, player.getZ()),
                    Vec3State.hoverTarget(deathPos),
                    TRAIL_TRAVEL_TICKS,
                    0,
                    0
            );
        }

        private boolean tick() {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player == null || player.level() != level) {
                return false;
            }

            if (arrivalDelayTicksRemaining > 0) {
                bitsandbalance$sendHoverParticles(level, player, hoverTarget);
                ACTIVE_TRAILS.put(playerId, new ActiveTrail(playerId, level, hoverTarget, hoverTarget, travelTicksRemaining, hoverTicksRemaining, arrivalDelayTicksRemaining - 1));
                return true;
            }

            if (hoverTicksRemaining > 0) {
                int hoverAge = TRAIL_HOVER_TICKS - hoverTicksRemaining;
                Vec3State hoverState = hoverTarget.withYOffset(Math.sin(hoverAge * TRAIL_HOVER_BOB_SPEED) * TRAIL_HOVER_BOB_AMPLITUDE);
                bitsandbalance$sendHoverParticles(level, player, hoverState);

                if (hoverTicksRemaining <= 1) {
                    bitsandbalance$sendPoof(level, player, hoverState);
                    return false;
                }

                ACTIVE_TRAILS.put(playerId, new ActiveTrail(playerId, level, hoverTarget, hoverTarget, travelTicksRemaining, hoverTicksRemaining - 1, 0));
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
                ACTIVE_TRAILS.put(playerId, new ActiveTrail(playerId, level, hoverTarget, hoverTarget, travelTicksRemaining, TRAIL_HOVER_TICKS, TRAIL_ARRIVAL_POOF_DELAY_TICKS));
                return true;
            }

            Vec3State velocity = delta.normalize().scale(TRAIL_SPEED_BLOCKS_PER_TICK);
            Vec3State nextState = state.add(velocity);
            bitsandbalance$sendTrailParticles(level, player, nextState);
            ACTIVE_TRAILS.put(playerId, new ActiveTrail(playerId, level, nextState, hoverTarget, travelTicksRemaining - 1, 0, 0));
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

    private static void bitsandbalance$sendTrailParticles(ServerLevel level, ServerPlayer player, Vec3State state) {
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
