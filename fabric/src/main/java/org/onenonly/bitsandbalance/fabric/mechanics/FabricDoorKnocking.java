package org.onenonly.bitsandbalance.fabric.mechanics;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.network.DoorKnockPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fabric port: Door Knocking
 * Handles keybind-based door knocking with cooldown.
 */
public final class FabricDoorKnocking {
    private FabricDoorKnocking() {
    }

    private static final int KNOCK_COOLDOWN_TICKS = 2;
    private static final Map<UUID, Long> playerKnockData = new HashMap<>();

    public static void initServer() {
        ServerPlayNetworking.registerGlobalReceiver(DoorKnockPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            BlockPos doorPos = payload.doorPos();

            context.server().execute(() -> {
                if (!FabricMechanicsConfig.enableDoorKnocking) return;

                BlockState state = player.level().getBlockState(doorPos);
                if (!(state.getBlock() instanceof DoorBlock)) return;

                UUID playerId = player.getUUID();
                long currentTick = player.level().getGameTime();

                Long lastKnock = playerKnockData.get(playerId);
                if (lastKnock != null) {
                    long ticksSince = currentTick - lastKnock;
                    if (ticksSince < KNOCK_COOLDOWN_TICKS) {
                        return; // Still on cooldown
                    }
                }

                playerKnockData.put(playerId, currentTick);

                // Normalize to lower half
                BlockPos doorBasePos = doorPos;
                if (state.hasProperty(DoorBlock.HALF) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                    doorBasePos = doorPos.below();
                }

                BlockState baseState = player.level().getBlockState(doorBasePos);
                if (!(baseState.getBlock() instanceof DoorBlock)) return;

                var doorBlock = baseState.getBlock();
                boolean isIronDoor = doorBlock == Blocks.IRON_DOOR;
                boolean isCopperDoor = doorBlock == Blocks.COPPER_DOOR || 
                                     doorBlock == Blocks.EXPOSED_COPPER_DOOR || 
                                     doorBlock == Blocks.WEATHERED_COPPER_DOOR || 
                                     doorBlock == Blocks.OXIDIZED_COPPER_DOOR;
                boolean isWoodDoor = baseState.is(BlockTags.WOODEN_DOORS);

                SoundType soundType = baseState.getSoundType();
                var sound = isIronDoor
                    ? SoundEvents.ZOMBIE_ATTACK_IRON_DOOR
                    : isCopperDoor
                    ? SoundEvents.ANVIL_LAND
                    : isWoodDoor
                    ? SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR
                    : soundType.getHitSound();

                float volume = 0.67F;
                float rand = player.level().getRandom().nextFloat();
                float pitch = isIronDoor
                    ? (0.50F + rand * 0.30F)
                    : isCopperDoor
                    ? (1.2F + rand * 0.40F)
                    : isWoodDoor
                    ? (0.60F + rand * 0.60F)
                    : (soundType.getPitch() * (0.90F + rand * 0.20F));

                player.level().playSound(null, doorBasePos, sound, SoundSource.BLOCKS, volume, pitch);
            });
        });
    }
}
