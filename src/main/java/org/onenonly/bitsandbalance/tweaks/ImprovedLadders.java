package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.MoverType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.onenonly.bitsandbalance.BitsAndBalance;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import org.onenonly.bitsandbalance.Config;
/**
 * Tweaks: Improved Climbing
 * - Chain placement by right-clicking a climbable block with a climbable item to extend downward.
 *   Supports ladders, vines, glow berries, twisting vines, and weeping vines.
 * - Faster upward and downward movement on any climbable based on look angle.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class ImprovedLadders {
    // NeoForge's common ladder tag
    private static final TagKey<net.minecraft.world.level.block.Block> LADDERS_TAG = 
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ladders"));
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!Config.enableClimbableChainPlacement) return;
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        
        // Support multiple climbable items
        boolean isSupportedItem = stack.getItem() == Items.LADDER || 
                                 stack.getItem() == Items.VINE ||
                                 stack.getItem() == Items.GLOW_BERRIES ||
                                 stack.getItem() == Items.TWISTING_VINES ||
                                 stack.getItem() == Items.WEEPING_VINES ||
                                 isLadderItem(stack.getItem());
        if (!isSupportedItem) return;

        BlockPos clickedPos = event.getPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!clickedState.is(BlockTags.CLIMBABLE) && !isLadderBlock(clickedState)) return;

        // Determine what block to place based on the held item
        BlockState blockToPlace = getBlockStateForItem(stack.getItem());
        if (blockToPlace == null) return;

        // For ladders, determine the facing; for other climbables, use default facing
        Direction facing = Direction.NORTH; // Default facing
        if ((clickedState.getBlock() instanceof LadderBlock && blockToPlace.getBlock() instanceof LadderBlock) ||
            (isLadderBlock(clickedState) && isLadderBlock(blockToPlace))) {
            // Handle both vanilla and modded ladders
            if (clickedState.getBlock() instanceof LadderBlock) {
                facing = clickedState.getValue(LadderBlock.FACING);
            } else {
                // For modded ladders, try to get facing from properties or use default
                facing = getLadderFacing(clickedState);
            }
        }

        // Determine placement position based on block type
        BlockPos placePos;
        if (blockToPlace.getBlock() == Blocks.TWISTING_VINES) {
            // Twisting vines grow upward - find the top-most climbable block
            BlockPos top = clickedPos;
            while (true) {
                BlockPos above = top.above();
                BlockState aboveState = level.getBlockState(above);
                if (aboveState.is(BlockTags.CLIMBABLE) || isLadderBlock(aboveState)) {
                    top = above;
                } else {
                    break;
                }
            }
            placePos = top.above();
        } else if (blockToPlace.getBlock() == Blocks.VINE) {
            // Regular vines grow downward - find the bottom-most climbable block
            BlockPos bottom = clickedPos;
            while (true) {
                BlockPos below = bottom.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.is(BlockTags.CLIMBABLE) || isLadderBlock(belowState)) {
                    bottom = below;
                } else {
                    break;
                }
            }
            placePos = bottom.below();
            
            // For vines, we'll check placement validity in getVineStateForPosition
            // This allows vines to continue through air gaps
        } else {
            // For ladders, cave vines, and weeping vines - find bottom and place below
            BlockPos bottom = clickedPos;
            while (true) {
                BlockPos below = bottom.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.is(BlockTags.CLIMBABLE) || isLadderBlock(belowState)) {
                    bottom = below;
                } else {
                    break;
                }
            }
            placePos = bottom.below();
        }

        BlockState placeAtState = level.getBlockState(placePos);
        if (!placeAtState.isAir()) return;

        // Set the appropriate properties for the block being placed
        BlockState finalBlockState = blockToPlace;
        if (blockToPlace.getBlock() instanceof LadderBlock) {
            finalBlockState = blockToPlace.setValue(LadderBlock.FACING, facing);
        } else if (isLadderBlock(blockToPlace)) {
            // For modded ladders, try to set facing if possible
            finalBlockState = setLadderFacing(blockToPlace, facing);
        } else if (blockToPlace.getBlock() == Blocks.VINE) {
            // Regular vines need to be attached to a solid block behind them
            finalBlockState = getVineStateForPosition(level, placePos);
            if (finalBlockState == null) return; // No valid vine state found
            // Skip canSurvive check for vines since we've already validated placement
        } else {
            // For other blocks (cave vines, weeping vines, twisting vines), use normal canSurvive check
            if (!finalBlockState.canSurvive(level, placePos)) return;
        }

        // Place the block
        boolean placed = level.setBlock(placePos, finalBlockState, 3);
        if (!placed) return;

        // Play placement sound
        try {
            var soundType = finalBlockState.getSoundType(level, placePos, player);
            level.playSound(null, placePos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
        } catch (Throwable ignored) {}

        // Consume one item if not in creative
        if (!player.getAbilities().instabuild) {
            // Prefer consuming from the hand used in the event
            InteractionHand hand = event.getHand();
            ItemStack handStack = player.getItemInHand(hand);
            if (!handStack.isEmpty() && handStack.getItem() == stack.getItem()) {
                handStack.shrink(1);
            } else {
                // Fallback: shrink the original stack reference if still valid
                stack.shrink(1);
            }
        }

        player.swing(event.getHand(), true);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!Config.enableFastClimbableSlide && !Config.enableFastClimbableAscend) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        Level level = sp.level();
        if (level.isClientSide()) return;
        if (sp.getAbilities().flying || sp.isFallFlying()) return;

        // Require being on a climbable or specifically near a climbable block
        BlockPos pos = sp.blockPosition();
        boolean onOrNearClimbable = sp.onClimbable() || isNearClimbable(level, pos);
        if (!onOrNearClimbable) return;

        Vec3 vel = sp.getDeltaMovement();
        Double targetY = getImprovedClimbingTargetY(sp, vel.y);
        if (targetY == null) return;

        sp.setDeltaMovement(vel.x, targetY, vel.z);
        if (targetY < 0.0D) {
            sp.resetFallDistance();
        }

    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!Config.enableFastClimbableSlide && !Config.enableFastClimbableAscend) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        Level level = sp.level();
        if (level.isClientSide()) return;
        if (sp.getAbilities().flying || sp.isFallFlying()) return;

        // Require being on a climbable or specifically near a climbable block
        BlockPos pos = sp.blockPosition();
        boolean onOrNearClimbable = sp.onClimbable() || isNearClimbable(level, pos);
        if (!onOrNearClimbable) return;

        Vec3 vel = sp.getDeltaMovement();

        Double targetY = getImprovedClimbingTargetY(sp, vel.y);
        if (targetY == null) return;

        double extra = targetY - vel.y;
        if ((targetY < 0.0D && extra < 0.0D) || (targetY > 0.0D && extra > 0.0D)) {
            sp.move(MoverType.SELF, new Vec3(0.0D, extra, 0.0D));
            sp.setDeltaMovement(vel.x, targetY, vel.z);
            if (targetY < 0.0D) {
                sp.resetFallDistance();
            }
        }

    }

    private static Double getImprovedClimbingTargetY(ServerPlayer player, double currentY) {
        float pitch = player.getXRot();

        if (currentY <= 0.0D && pitch >= (float) Config.fastClimbableLookDownMinDeg) {
            return getDownwardTargetY(pitch);
        }

        if (Config.enableFastClimbableAscend && currentY > 0.0D && pitch <= -(float) Config.fastClimbableLookUpMinDeg) {
            return getUpwardTargetY(-pitch);
        }

        return null;
    }

    private static double getDownwardTargetY(float downwardAngleDeg) {
        float minDeg = (float) Config.fastClimbableLookDownMinDeg;
        float maxDeg = (float) Config.fastClimbableLookDownMaxDeg;
        double vanillaDown = -0.15D;
        double maxConfigured = -Math.max(0.05D, Config.fastClimbableSlideSpeed);
        if (maxDeg <= minDeg) {
            return maxConfigured;
        }
        return Mth.clampedMap(downwardAngleDeg, minDeg, maxDeg, (float) vanillaDown, (float) maxConfigured);
    }

    private static double getUpwardTargetY(float upwardAngleDeg) {
        float minDeg = (float) Config.fastClimbableLookUpMinDeg;
        float maxDeg = (float) Config.fastClimbableLookUpMaxDeg;
        double vanillaUp = 0.2D;
        double maxConfigured = Math.max(vanillaUp, Config.fastClimbableAscendSpeed);
        if (maxDeg <= minDeg) {
            return maxConfigured;
        }
        return Mth.clampedMap(upwardAngleDeg, minDeg, maxDeg, (float) vanillaUp, (float) maxConfigured);
    }

    private static boolean isClimbable(BlockState state) {
        return state.is(BlockTags.CLIMBABLE) || isLadderBlock(state);
    }

    private static boolean isNearClimbable(Level level, BlockPos pos) {
        // Check four horizontal neighbors at feet and head height, and the current block just in case
        BlockPos above = pos.above();
        BlockPos[] positions = new BlockPos[]{
                pos, above,
                pos.north(), above.north(),
                pos.south(), above.south(),
                pos.east(), above.east(),
                pos.west(), above.west()
        };
        for (BlockPos p : positions) {
            if (isClimbable(level.getBlockState(p))) return true;
        }
        return false;
    }

    private static BlockState getBlockStateForItem(net.minecraft.world.item.Item item) {
        if (item == Items.LADDER) {
            return Blocks.LADDER.defaultBlockState();
        } else if (item == Items.VINE) {
            return Blocks.VINE.defaultBlockState();
        } else if (item == Items.GLOW_BERRIES) {
            return Blocks.CAVE_VINES.defaultBlockState();
        } else if (item == Items.TWISTING_VINES) {
            return Blocks.TWISTING_VINES.defaultBlockState();
        } else if (item == Items.WEEPING_VINES) {
            return Blocks.WEEPING_VINES.defaultBlockState();
        } else if (isLadderItem(item)) {
            // For modded ladder items, get the block state from the item
            return getLadderBlockStateFromItem(item);
        }
        return null;
    }


    private static BlockState getVineStateForPosition(Level level, BlockPos vinePos) {
        // First, try to find the attachment direction from vines above in the chain
        Direction attachmentDirection = findAttachmentDirectionFromChain(level, vinePos);
        
        if (attachmentDirection != null) {
            // Use the same attachment direction as the vine chain above
            return Blocks.VINE.defaultBlockState().setValue(net.minecraft.world.level.block.VineBlock.getPropertyForFace(attachmentDirection), true);
        }
        
        // If no chain direction found, check for immediate solid blocks
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        
        for (Direction dir : directions) {
            BlockPos attachPos = vinePos.relative(dir);
            BlockState attachState = level.getBlockState(attachPos);
            
            // If there's a solid block in this direction, attach the vine to it
            if (attachState.isFaceSturdy(level, attachPos, dir.getOpposite()) && !attachState.isAir()) {
                return Blocks.VINE.defaultBlockState().setValue(net.minecraft.world.level.block.VineBlock.getPropertyForFace(dir), true);
            }
        }
        
        // Fallback: return default vine state
        return Blocks.VINE.defaultBlockState();
    }

    private static Direction findAttachmentDirectionFromChain(Level level, BlockPos vinePos) {
        // Look upward through the climbable chain to find vine attachment directions
        BlockPos checkPos = vinePos.above();
        
        while (checkPos.getY() < level.getMaxY()) {
            BlockState checkState = level.getBlockState(checkPos);
            
            // If we find a vine block, check its attachment directions
            if (checkState.getBlock() == Blocks.VINE) {
                // Check which directions this vine is attached to
                Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                
                for (Direction dir : directions) {
                    if (checkState.getValue(net.minecraft.world.level.block.VineBlock.getPropertyForFace(dir))) {
                        // Found an attachment direction - use the same one
                        return dir;
                    }
                }
            }
            
            // If we hit a solid block, stop looking
            if (checkState.isFaceSturdy(level, checkPos, Direction.DOWN) && !checkState.isAir()) {
                break;
            }
            
            // If we hit a non-climbable block, stop looking
            if (!checkState.is(BlockTags.CLIMBABLE) && !isLadderBlock(checkState)) {
                break;
            }
            
            checkPos = checkPos.above();
        }
        
        return null; // No attachment direction found
    }

    /**
     * Checks if a block state is a ladder block using NeoForge's #c:ladders tag.
     * This allows support for modded ladders that use the common ladder tag.
     */
    private static boolean isLadderBlock(BlockState state) {
        return state.is(LADDERS_TAG);
    }

    /**
     * Checks if an item corresponds to a ladder block using NeoForge's #c:ladders tag.
     * This allows support for modded ladder items.
     */
    private static boolean isLadderItem(net.minecraft.world.item.Item item) {
        if (item instanceof net.minecraft.world.item.BlockItem blockItem) {
            BlockState blockState = blockItem.getBlock().defaultBlockState();
            return isLadderBlock(blockState);
        }
        return false;
    }

    /**
     * Gets the block state for a modded ladder item.
     */
    private static BlockState getLadderBlockStateFromItem(net.minecraft.world.item.Item item) {
        if (item instanceof net.minecraft.world.item.BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState();
        }
        return null;
    }

    /**
     * Gets the facing direction from a modded ladder block state.
     * Returns NORTH as default if facing property is not found.
     */
    private static Direction getLadderFacing(BlockState state) {
        // Try to get facing from common property names
        if (state.hasProperty(LadderBlock.FACING)) {
            return state.getValue(LadderBlock.FACING);
        }
        // For modded ladders, try to find a facing-like property
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (property.getName().toLowerCase().contains("facing") && property.getValueClass() == Direction.class) {
                @SuppressWarnings("unchecked")
                net.minecraft.world.level.block.state.properties.Property<Direction> facingProp = (net.minecraft.world.level.block.state.properties.Property<Direction>) property;
                return state.getValue(facingProp);
            }
        }
        return Direction.NORTH; // Default facing
    }

    /**
     * Sets the facing direction for a modded ladder block state.
     * Returns the original state if facing cannot be set.
     */
    private static BlockState setLadderFacing(BlockState state, Direction facing) {
        // Try to set facing using common property names
        if (state.hasProperty(LadderBlock.FACING)) {
            return state.setValue(LadderBlock.FACING, facing);
        }
        // For modded ladders, try to find and set a facing-like property
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (property.getName().toLowerCase().contains("facing") && property.getValueClass() == Direction.class) {
                @SuppressWarnings("unchecked")
                net.minecraft.world.level.block.state.properties.Property<Direction> facingProp = (net.minecraft.world.level.block.state.properties.Property<Direction>) property;
                return state.setValue(facingProp, facing);
            }
        }
        return state; // Return original state if facing cannot be set
    }

}
