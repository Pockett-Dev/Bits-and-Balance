package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;

import java.util.List;

/**
 * Tweaks: Cascading Ladders
 * - Ladders now behave like vines - when you break one, any ladders below it that are no longer supported will also break.
 * - Simple downward traversal, no complex flood-fill algorithm.
 * Server-side only.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class CascadingLadders {
    // NeoForge's common ladder tag
    private static final TagKey<net.minecraft.world.level.block.Block> LADDERS_TAG = 
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ladders"));

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // Only handle ladder blocks (vanilla ladders or blocks tagged with #c:ladders)
        if (!(event.getState().getBlock() instanceof LadderBlock) && !isLadderBlock(event.getState())) return;
        
        // Server-side only
        if (event.getLevel().isClientSide()) return;
        
        ServerLevel level = (ServerLevel) event.getLevel();
        Player player = event.getPlayer();
        if (player == null) return;
        
        BlockPos brokenPos = event.getPos();
        
        // Schedule the cascade check to run after the block is actually broken
            level.getServer().execute(new net.minecraft.server.TickTask(1, () -> {
            breakUnsupportedLaddersBelow(level, brokenPos);
        }));
    }

    /**
     * Breaks any ladders below the given position that are no longer supported.
     * This mimics vanilla vine behavior - when you break a vine, unsupported vines below also break.
     */
    private static void breakUnsupportedLaddersBelow(ServerLevel level, BlockPos startPos) {
        BlockPos currentPos = startPos.below();
        
            while (currentPos.getY() >= level.getMinY()) {
            BlockState currentState = level.getBlockState(currentPos);
            
            // If it's not a ladder, stop checking
            if (!(currentState.getBlock() instanceof LadderBlock) && !isLadderBlock(currentState)) {
                break;
            }
            
            // Check if this ladder is still supported
            if (isLadderSupported(level, currentPos)) {
                // This ladder is still supported, so we can stop here
                break;
            }

            // This ladder is no longer supported, break it
            
            // Drop items as if broken by player
            List<net.minecraft.world.item.ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                currentState, level, currentPos, null, null, net.minecraft.world.item.ItemStack.EMPTY);
            
            // Spawn the dropped items
            for (net.minecraft.world.item.ItemStack drop : drops) {
                if (!drop.isEmpty()) {
                    net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                        level, currentPos.getX() + 0.5, currentPos.getY() + 0.5, currentPos.getZ() + 0.5, drop);
                    itemEntity.setDefaultPickUpDelay();
                    level.addFreshEntity(itemEntity);
                }
            }
            
            // Remove the block
            level.setBlock(currentPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            
            // Play break sound
            try {
                var soundType = currentState.getSoundType();
                level.playSound(null, currentPos, soundType.getBreakSound(), net.minecraft.sounds.SoundSource.BLOCKS,
                        (soundType.getVolume() + 1.0F) / 2.0F,
                        soundType.getPitch() * 0.8F);
            } catch (Throwable ignored) {}
            
            // Move down to check the next ladder
            currentPos = currentPos.below();
        }
    }

    /**
     * Checks if a ladder at the given position is supported.
     * A ladder is supported if it has a solid block behind it or another ladder above it.
     */
    private static boolean isLadderSupported(Level level, BlockPos ladderPos) {
        BlockState ladderState = level.getBlockState(ladderPos);
        if (!(ladderState.getBlock() instanceof LadderBlock) && !isLadderBlock(ladderState)) return false;
        
        Direction facing = ladderState.getValue(LadderBlock.FACING);
        BlockPos supportPos = ladderPos.relative(facing.getOpposite());
        BlockState supportState = level.getBlockState(supportPos);
        
        // Check if there's a solid block behind the ladder
        if (supportState.isFaceSturdy(level, supportPos, facing) && !supportState.isAir()) {
            return true;
        }
        
        // Check if there's a ladder above this one
        BlockPos abovePos = ladderPos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        if (aboveState.getBlock() instanceof LadderBlock || isLadderBlock(aboveState)) {
            return true;
        }
        
        return false;
    }

    /**
     * Checks if a block state is a ladder block using NeoForge's #c:ladders tag.
     * This allows support for modded ladders that use the common ladder tag.
     */
    private static boolean isLadderBlock(BlockState state) {
        return state.is(LADDERS_TAG);
    }
}
