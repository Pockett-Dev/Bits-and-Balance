package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import java.util.*;

import org.onenonly.bitsandbalance.Config;
/**
 * Tweaks: Sophisticated Scaffolding
 * - When breaking scaffolding blocks, all drops (including cascading blocks) appear at the player's location with no velocity.
 * - Prevents scaffolding items from scattering and makes collection much easier.
 * Server-side only.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class SophisticatedScaffolding {

    // Cache to track scaffolding structures and prevent infinite recursion
    private static final Map<ServerLevel, Set<BlockPos>> processingCache = new HashMap<>();

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!Config.enableSophisticatedScaffolding) return;
        
        // Only handle scaffolding blocks
        if (!(event.getState().getBlock() instanceof ScaffoldingBlock)) return;
        
        // Server-side only
        if (event.getLevel().isClientSide()) return;
        
        ServerLevel level = (ServerLevel) event.getLevel();
        Player player = event.getPlayer();
        if (player == null) return;
        
        BlockPos brokenPos = event.getPos();
        
        // Prevent processing the same position multiple times in cascading breaks
        Set<BlockPos> currentlyProcessing = processingCache.computeIfAbsent(level, k -> new HashSet<>());
        if (currentlyProcessing.contains(brokenPos)) return;
        
        try {
            // Cancel the original event to prevent double-dropping
            event.setCanceled(true);
            handleSophisticatedScaffoldingBreak(level, player, brokenPos, event);
        } finally {
            // Clean up processing cache for this position
            currentlyProcessing.remove(brokenPos);
            if (currentlyProcessing.isEmpty()) {
                processingCache.remove(level);
            }
        }
    }

    private static void handleSophisticatedScaffoldingBreak(ServerLevel level, Player player, BlockPos brokenPos, BlockEvent.BreakEvent event) {
        // Get the original block state before it's broken
        BlockState originalState = event.getState();
        
        // Find all scaffolding blocks that will cascade due to this break
        Set<BlockPos> cascadingBlocks = findCascadingScaffolding(level, brokenPos);
        
        // Collect all drops from the original block and cascading blocks
        List<ItemStack> allDrops = new ArrayList<>();
        
        // Add drops from the originally broken block
        List<ItemStack> originalDrops = Block.getDrops(originalState, level, brokenPos, null, player, player.getMainHandItem());
        allDrops.addAll(originalDrops);
        
        // Add drops from cascading blocks
        for (BlockPos cascadePos : cascadingBlocks) {
            BlockState cascadeState = level.getBlockState(cascadePos);
            if (cascadeState.getBlock() instanceof ScaffoldingBlock) {
                List<ItemStack> cascadeDrops = Block.getDrops(cascadeState, level, cascadePos, null, player, player.getMainHandItem());
                allDrops.addAll(cascadeDrops);
                
                // Remove the cascading block
                level.setBlock(cascadePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                
                // Play break sound for each cascading block
                try {
                    var soundType = cascadeState.getSoundType(level, cascadePos, player);
                    level.playSound(null, cascadePos, soundType.getBreakSound(), net.minecraft.sounds.SoundSource.BLOCKS,
                            (soundType.getVolume() + 1.0F) / 2.0F,
                            soundType.getPitch() * 0.8F);
                } catch (Throwable ignored) {}
            }
        }
        
        // Remove the original block since we canceled the event
        level.setBlock(brokenPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        
        // Play break sound for the original block
        try {
            var soundType = originalState.getSoundType(level, brokenPos, player);
            level.playSound(null, brokenPos, soundType.getBreakSound(), net.minecraft.sounds.SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F,
                    soundType.getPitch() * 0.8F);
        } catch (Throwable ignored) {}
        
        // Spawn all items at player location with zero velocity
        spawnItemsAtPlayerLocation(level, player, allDrops);
    }

    /**
     * Finds all scaffolding blocks that will cascade/break when the given block is removed.
     * Uses a simplified version of scaffolding physics to determine which blocks lose support.
     */
    private static Set<BlockPos> findCascadingScaffolding(ServerLevel level, BlockPos brokenPos) {
        Set<BlockPos> cascadingBlocks = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        
        // Start by checking all adjacent scaffolding blocks
        for (BlockPos adjacent : getAdjacentPositions(brokenPos)) {
            if (level.getBlockState(adjacent).getBlock() instanceof ScaffoldingBlock) {
                toCheck.add(adjacent);
            }
        }
        
        while (!toCheck.isEmpty()) {
            BlockPos current = toCheck.poll();
            if (visited.contains(current)) continue;
            visited.add(current);
            
            BlockState currentState = level.getBlockState(current);
            if (!(currentState.getBlock() instanceof ScaffoldingBlock)) continue;
            
            // Check if this scaffolding block will lose support when brokenPos is removed
            if (willLoseSupport(level, current, brokenPos, cascadingBlocks)) {
                cascadingBlocks.add(current);
                
                // Add adjacent scaffolding blocks to check queue
                for (BlockPos adjacent : getAdjacentPositions(current)) {
                    if (!visited.contains(adjacent) && level.getBlockState(adjacent).getBlock() instanceof ScaffoldingBlock) {
                        toCheck.add(adjacent);
                    }
                }
            }
        }
        
        return cascadingBlocks;
    }

    /**
     * Determines if a scaffolding block will lose support when the broken block and other cascading blocks are removed.
     * This is a simplified version of Minecraft's scaffolding physics.
     */
    private static boolean willLoseSupport(ServerLevel level, BlockPos scaffoldPos, BlockPos brokenPos, Set<BlockPos> alreadyCascading) {
        // Scaffolding can be supported by:
        // 1. A solid block directly below
        // 2. Another scaffolding block within distance (usually 7 blocks from a solid support)
        
        // Check for solid block directly below
        BlockPos below = scaffoldPos.below();
        if (!below.equals(brokenPos) && !alreadyCascading.contains(below)) {
            BlockState belowState = level.getBlockState(below);
            if (belowState.isSolidRender()) {
                return false; // Has solid support
            }
        }
        
        // Check for scaffolding chain support (simplified - check within reasonable distance)
        return !hasScaffoldingChainSupport(level, scaffoldPos, brokenPos, alreadyCascading, 0, 7);
    }

    /**
     * Recursively checks if there's a chain of scaffolding blocks leading to solid support.
     */
    private static boolean hasScaffoldingChainSupport(ServerLevel level, BlockPos pos, BlockPos brokenPos, Set<BlockPos> alreadyCascading, int depth, int maxDepth) {
        if (depth >= maxDepth) return false;
        
        // Check all adjacent positions for scaffolding or solid blocks
        for (BlockPos adjacent : getAdjacentPositions(pos)) {
            if (adjacent.equals(brokenPos) || alreadyCascading.contains(adjacent)) continue;
            
            BlockState adjacentState = level.getBlockState(adjacent);
            
            // Found solid support
            if (adjacentState.isSolidRender()) {
                return true;
            }
            
            // Found scaffolding - continue the chain
            if (adjacentState.getBlock() instanceof ScaffoldingBlock) {
                if (hasScaffoldingChainSupport(level, adjacent, brokenPos, alreadyCascading, depth + 1, maxDepth)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Gets all 6 adjacent positions (up, down, north, south, east, west).
     */
    private static List<BlockPos> getAdjacentPositions(BlockPos pos) {
        return Arrays.asList(
            pos.above(),
            pos.below(),
            pos.north(),
            pos.south(),
            pos.east(),
            pos.west()
        );
    }

    /**
     * Spawns all items at the player's location with zero velocity.
     */
    private static void spawnItemsAtPlayerLocation(ServerLevel level, Player player, List<ItemStack> drops) {
        if (drops.isEmpty()) return;
        
        // Merge identical items to reduce entity count
        Map<ItemStack, Integer> mergedDrops = new HashMap<>();
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            
            // Find existing similar stack or create new entry
            boolean merged = false;
            for (Map.Entry<ItemStack, Integer> entry : mergedDrops.entrySet()) {
                if (ItemStack.isSameItemSameComponents(entry.getKey(), drop)) {
                    entry.setValue(entry.getValue() + drop.getCount());
                    merged = true;
                    break;
                }
            }
            
            if (!merged) {
                mergedDrops.put(drop.copy(), drop.getCount());
            }
        }
        
        // Spawn merged items at player location
        Vec3 playerPos = player.position();
        for (Map.Entry<ItemStack, Integer> entry : mergedDrops.entrySet()) {
            ItemStack baseStack = entry.getKey();
            int totalCount = entry.getValue();
            
            // Split into multiple stacks if needed (respecting max stack size)
            while (totalCount > 0) {
                int stackSize = Math.min(totalCount, baseStack.getMaxStackSize());
                ItemStack stackToSpawn = baseStack.copy();
                stackToSpawn.setCount(stackSize);
                
                // Create item entity at player location with zero velocity
                ItemEntity itemEntity = new ItemEntity(level, playerPos.x, playerPos.y + 0.5, playerPos.z, stackToSpawn);
                itemEntity.setDeltaMovement(Vec3.ZERO); // No velocity
                itemEntity.setPickUpDelay(10); // Short pickup delay
                
                level.addFreshEntity(itemEntity);
                
                totalCount -= stackSize;
            }
        }
    }
}
