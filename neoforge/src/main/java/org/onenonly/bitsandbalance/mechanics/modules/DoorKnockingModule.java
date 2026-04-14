package org.onenonly.bitsandbalance.mechanics.modules;

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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Door knocking handler.
 * Keybind-based door knocking with a 2-tick cooldown.
 * Left-click knocking has been removed - only keybind knocking is supported.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class DoorKnockingModule {
    
    private static final int KNOCK_COOLDOWN_TICKS = 2;
    private static final Map<UUID, KnockData> playerKnockData = new HashMap<>();
    
    private static class KnockData {
        long lastKnockTick;
        
        KnockData(long tick) {
            this.lastKnockTick = tick;
        }
    }

    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!Config.enableDoorKnocking) return;
        
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof DoorBlock)) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        
        // Door was broken - clear tracking
        playerKnockData.remove(player.getUUID());
    }
    
    /**
     * Handles door knocking triggered by keybind.
     * Simple implementation: allows knocking once every 2 ticks.
     */
    public static void handleKeybindKnock(ServerPlayer player, BlockPos doorPos, BlockState state) {
        if (!Config.enableDoorKnocking) return;
        
        UUID playerId = player.getUUID();
        long currentTick = player.level().getGameTime();
        
        // Normalize to lower half for consistency
        BlockPos doorBasePos = doorPos;
        if (state.hasProperty(DoorBlock.HALF) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            doorBasePos = doorPos.below();
        }

        BlockState baseState = player.level().getBlockState(doorBasePos);
        if (!(baseState.getBlock() instanceof DoorBlock)) return;
        
        KnockData data = playerKnockData.get(playerId);
        
        // Simple cooldown check - just check if 5 ticks have passed since last knock
        boolean shouldKnock = true;
        if (data != null) {
            long ticksSinceLastKnock = currentTick - data.lastKnockTick;
            if (ticksSinceLastKnock < KNOCK_COOLDOWN_TICKS) {
                shouldKnock = false;
            }
        }
        
        // Update tracking - always update the last knock time
        if (data == null) {
            data = new KnockData(currentTick);
            playerKnockData.put(playerId, data);
        } else {
            data.lastKnockTick = currentTick; // Always update, even if we don't knock
        }
        
        // Play knock sound if cooldown allows
        if (shouldKnock) {
            boolean isIronDoor = baseState.is(Blocks.IRON_DOOR);
            boolean isCopperDoor = baseState.is(Blocks.COPPER_DOOR) || 
                                 baseState.is(Blocks.EXPOSED_COPPER_DOOR) || 
                                 baseState.is(Blocks.WEATHERED_COPPER_DOOR) || 
                                 baseState.is(Blocks.OXIDIZED_COPPER_DOOR);
            boolean isWoodDoor = baseState.is(BlockTags.WOODEN_DOORS);

            SoundType soundType = baseState.getSoundType(player.level(), doorBasePos, player);
            var sound = isIronDoor
                ? SoundEvents.ZOMBIE_ATTACK_IRON_DOOR
                : isCopperDoor
                ? SoundEvents.ANVIL_LAND  // Copper doors: metallic but softer than iron
                : isWoodDoor
                ? SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR
                : soundType.getHitSound(); // Modded doors (e.g., glass) use their own hit sound

            float volume = 0.67F; // Reduced by 33% from 1.0F
            float rand = player.level().getRandom().nextFloat();
            float pitch = isIronDoor
                ? (0.50F + rand * 0.30F)
                : isCopperDoor
                ? (1.2F + rand * 0.40F)
                : isWoodDoor
                ? (0.60F + rand * 0.60F)
                : (soundType.getPitch() * (0.90F + rand * 0.20F));

            player.level().playSound(null, doorBasePos, sound, SoundSource.BLOCKS, volume, pitch);
        }
    }
}