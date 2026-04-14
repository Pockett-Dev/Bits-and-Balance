package org.onenonly.bitsandbalance.fabric.tweaks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

/**
 * Fabric port of Tweaks: Improved Climbing
 * - Chain placement by right-clicking a climbable block with a climbable item to extend downward.
 *   Supports ladders, vines, glow berries, twisting vines, and weeping vines.
 * - Faster upward and downward movement on any climbable based on look angle.
 */
public class FabricImprovedLadders {

    public static void init() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (!FabricTweaksConfig.enableFastClimbableSlide && !FabricTweaksConfig.enableFastClimbableAscend) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                applyImprovedClimbingPre(player);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!FabricTweaksConfig.enableFastClimbableSlide && !FabricTweaksConfig.enableFastClimbableAscend) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                applyImprovedClimbingPost(player);
            }
        });

        // Register chain placement on right-click block
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!FabricTweaksConfig.enableClimbableChainPlacement) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) return InteractionResult.PASS;
            
            // Support multiple climbable items
            boolean isSupportedItem = stack.getItem() == Items.LADDER || 
                                     stack.getItem() == Items.VINE ||
                                     stack.getItem() == Items.GLOW_BERRIES ||
                                     stack.getItem() == Items.TWISTING_VINES ||
                                     stack.getItem() == Items.WEEPING_VINES;
            if (!isSupportedItem) return InteractionResult.PASS;

            BlockPos clickedPos = hitResult.getBlockPos();
            BlockState clickedState = level.getBlockState(clickedPos);
            if (!clickedState.is(BlockTags.CLIMBABLE)) return InteractionResult.PASS;

            // Determine what block to place based on the held item
            BlockState blockToPlace = getBlockStateForItem(stack.getItem());
            if (blockToPlace == null) return InteractionResult.PASS;

            // For ladders, determine the facing; for other climbables, use default facing
            Direction facing = Direction.NORTH; // Default facing
            if (clickedState.getBlock() instanceof LadderBlock && blockToPlace.getBlock() instanceof LadderBlock) {
                facing = clickedState.getValue(LadderBlock.FACING);
            }

            // Determine placement position based on block type
            BlockPos placePos;
            if (blockToPlace.getBlock() == Blocks.TWISTING_VINES) {
                // Twisting vines grow upward - find the top-most climbable block
                BlockPos top = clickedPos;
                while (true) {
                    BlockPos above = top.above();
                    BlockState aboveState = level.getBlockState(above);
                    if (aboveState.is(BlockTags.CLIMBABLE)) {
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
                    if (belowState.is(BlockTags.CLIMBABLE)) {
                        bottom = below;
                    } else {
                        break;
                    }
                }
                placePos = bottom.below();
            } else {
                // For ladders, cave vines, and weeping vines - find bottom and place below
                BlockPos bottom = clickedPos;
                while (true) {
                    BlockPos below = bottom.below();
                    BlockState belowState = level.getBlockState(below);
                    if (belowState.is(BlockTags.CLIMBABLE)) {
                        bottom = below;
                    } else {
                        break;
                    }
                }
                placePos = bottom.below();
            }

            BlockState placeAtState = level.getBlockState(placePos);
            if (!placeAtState.isAir()) return InteractionResult.PASS;

            // Set the appropriate properties for the block being placed
            BlockState finalBlockState = blockToPlace;
            if (blockToPlace.getBlock() instanceof LadderBlock) {
                finalBlockState = blockToPlace.setValue(LadderBlock.FACING, facing);
            } else if (blockToPlace.getBlock() == Blocks.VINE) {
                // Regular vines need to be attached to a solid block behind them
                finalBlockState = getVineStateForPosition(level, placePos);
                if (finalBlockState == null) return InteractionResult.PASS; // No valid vine state found
            } else {
                // For other blocks (cave vines, weeping vines, twisting vines), use normal canSurvive check
                if (!finalBlockState.canSurvive(level, placePos)) return InteractionResult.PASS;
            }

            // Place the block
            boolean placed = level.setBlock(placePos, finalBlockState, 3);
            if (!placed) return InteractionResult.PASS;

            // Play placement sound
            try {
                var soundType = finalBlockState.getSoundType();
                level.playSound(null, placePos, soundType.getPlaceSound(), SoundSource.BLOCKS, 
                    (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
            } catch (Throwable ignored) {}

            // Consume one item if not in creative
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            player.swing(hand, true);

            return InteractionResult.SUCCESS;
        });
    }

    private static void applyImprovedClimbingPre(ServerPlayer player) {
        Level level = player.level();
        if (level.isClientSide()) return;
        if (player.getAbilities().flying || player.isFallFlying()) return;

        BlockPos pos = player.blockPosition();
        if (!(player.onClimbable() || isNearClimbable(level, pos))) return;

        Vec3 velocity = player.getDeltaMovement();
        Double targetY = getImprovedClimbingTargetY(player, velocity.y);
        if (targetY == null) return;

        player.setDeltaMovement(velocity.x, targetY, velocity.z);
        if (targetY < 0.0D) {
            player.resetFallDistance();
        }
    }

    private static void applyImprovedClimbingPost(ServerPlayer player) {
        Level level = player.level();
        if (level.isClientSide()) return;
        if (player.getAbilities().flying || player.isFallFlying()) return;

        BlockPos pos = player.blockPosition();
        if (!(player.onClimbable() || isNearClimbable(level, pos))) return;

        Vec3 velocity = player.getDeltaMovement();
        Double targetY = getImprovedClimbingTargetY(player, velocity.y);
        if (targetY == null) return;

        double extra = targetY - velocity.y;
        if ((targetY < 0.0D && extra < 0.0D) || (targetY > 0.0D && extra > 0.0D)) {
            player.move(MoverType.SELF, new Vec3(0.0D, extra, 0.0D));
            player.setDeltaMovement(velocity.x, targetY, velocity.z);
            if (targetY < 0.0D) {
                player.resetFallDistance();
            }
        }
    }

    private static Double getImprovedClimbingTargetY(ServerPlayer player, double currentY) {
        float pitch = player.getXRot();

        if (currentY <= 0.0D && pitch >= (float) FabricTweaksConfig.fastClimbableLookDownMinDeg) {
            return getDownwardTargetY(pitch);
        }

        if (FabricTweaksConfig.enableFastClimbableAscend && currentY > 0.0D && pitch <= -(float) FabricTweaksConfig.fastClimbableLookUpMinDeg) {
            return getUpwardTargetY(-pitch);
        }

        return null;
    }

    private static double getDownwardTargetY(float downwardAngleDeg) {
        float minDeg = (float) FabricTweaksConfig.fastClimbableLookDownMinDeg;
        float maxDeg = (float) FabricTweaksConfig.fastClimbableLookDownMaxDeg;
        double vanillaDown = -0.15D;
        double maxConfigured = -Math.max(0.05D, FabricTweaksConfig.fastClimbableSlideSpeed);
        if (maxDeg <= minDeg) {
            return maxConfigured;
        }
        return Mth.clampedMap(downwardAngleDeg, minDeg, maxDeg, (float) vanillaDown, (float) maxConfigured);
    }

    private static double getUpwardTargetY(float upwardAngleDeg) {
        float minDeg = (float) FabricTweaksConfig.fastClimbableLookUpMinDeg;
        float maxDeg = (float) FabricTweaksConfig.fastClimbableLookUpMaxDeg;
        double vanillaUp = 0.2D;
        double maxConfigured = Math.max(vanillaUp, FabricTweaksConfig.fastClimbableAscendSpeed);
        if (maxDeg <= minDeg) {
            return maxConfigured;
        }
        return Mth.clampedMap(upwardAngleDeg, minDeg, maxDeg, (float) vanillaUp, (float) maxConfigured);
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
        }
        return null;
    }

    private static BlockState getVineStateForPosition(Level level, BlockPos vinePos) {
        // First, try to find the attachment direction from vines above in the chain
        Direction attachmentDirection = findAttachmentDirectionFromChain(level, vinePos);
        
        if (attachmentDirection != null) {
            // Use the same attachment direction as the vine chain above
            return Blocks.VINE.defaultBlockState().setValue(
                net.minecraft.world.level.block.VineBlock.getPropertyForFace(attachmentDirection), true);
        }
        
        // If no chain direction found, check for immediate solid blocks
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        
        for (Direction dir : directions) {
            BlockPos attachPos = vinePos.relative(dir);
            BlockState attachState = level.getBlockState(attachPos);
            
            // If there's a solid block in this direction, attach the vine to it
            if (attachState.isFaceSturdy(level, attachPos, dir.getOpposite()) && !attachState.isAir()) {
                return Blocks.VINE.defaultBlockState().setValue(
                    net.minecraft.world.level.block.VineBlock.getPropertyForFace(dir), true);
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
            if (!checkState.is(BlockTags.CLIMBABLE)) {
                break;
            }
            
            checkPos = checkPos.above();
        }
        
        return null; // No attachment direction found
    }

            private static boolean isNearClimbable(Level level, BlockPos pos) {
                BlockPos above = pos.above();
                BlockPos[] positions = new BlockPos[]{
                        pos, above,
                        pos.north(), above.north(),
                        pos.south(), above.south(),
                        pos.east(), above.east(),
                        pos.west(), above.west()
                };
                for (BlockPos checkPos : positions) {
                    if (level.getBlockState(checkPos).is(BlockTags.CLIMBABLE)) return true;
                }
                return false;
            }
}
