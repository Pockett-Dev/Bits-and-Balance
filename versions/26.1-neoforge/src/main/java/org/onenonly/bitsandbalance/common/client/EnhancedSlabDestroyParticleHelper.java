package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPlacementPredictionCache;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabParticleStateCache;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;

import java.util.List;

public final class EnhancedSlabDestroyParticleHelper {

    private static final int VANILLA_DESTROY_PARTICLE_COUNT = 64;

    private static final VoxelShape NORTH_HALF_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0);
    private static final VoxelShape SOUTH_HALF_SHAPE = Block.box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0);
    private static final VoxelShape WEST_HALF_SHAPE = Block.box(0.0, 0.0, 0.0, 8.0, 16.0, 16.0);
    private static final VoxelShape EAST_HALF_SHAPE = Block.box(8.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape FULL_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    private EnhancedSlabDestroyParticleHelper() {
    }

    public static boolean spawnForState(ClientLevel level, @Nullable Player player, BlockPos pos, BlockState current) {
        if (level == null || current == null) return false;

        if (EnhancedSlabVerticalDestroyParticleCapture.spawnOrSuppressCaptured(level, pos, current)) {
            return true;
        }

        ParticleSpec spec = resolve(level, player, pos, current);
        if (spec == null || spec.particleState == null || spec.shape == null || spec.shape.isEmpty()) return false;

        spawn(level, pos, spec.particleState, spec.shape);
        return true;
    }

    public static boolean spawnHitForState(ClientLevel level,
                                           @Nullable Player player,
                                           BlockPos pos,
                                           Direction direction,
                                           BlockState current) {
        return spawnHitForState(level, player, null, pos, direction, current);
    }

    public static boolean spawnHitForState(ClientLevel level,
                                           @Nullable Player player,
                                           @Nullable BlockHitResult hitResult,
                                           BlockPos pos,
                                           Direction direction,
                                           BlockState current) {
        if (level == null || current == null || direction == null) return false;

        ParticleSpec spec = resolve(level, player, hitResult, pos, current);
        if (spec == null || spec.particleState == null || spec.shape == null || spec.shape.isEmpty()) return false;

        spawnHit(level, pos, direction, spec.particleState, spec.shape);
        return true;
    }

    private static @Nullable ParticleSpec resolve(BlockGetter level, @Nullable Player player, BlockPos pos, BlockState current) {
        return resolve(level, player, null, pos, current);
    }

    private static @Nullable ParticleSpec resolve(BlockGetter level,
                                                  @Nullable Player player,
                                                  @Nullable BlockHitResult hitResult,
                                                  BlockPos pos,
                                                  BlockState current) {
        try {
            if (current.getBlock() instanceof MixedSlabBlock) {
                BlockState[] stored = materialSlots(level, pos);
                if (stored == null) return null;

                SlabType targetedHalf = SlabType.BOTTOM;
                BlockHitResult effectiveHit = hitResult;
                if (effectiveHit == null) {
                    effectiveHit = Minecraft.getInstance().hitResult instanceof BlockHitResult bhr ? bhr : null;
                }
                if (effectiveHit != null && pos.equals(effectiveHit.getBlockPos())) {
                    targetedHalf = org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper.getTargetedHalf(pos, effectiveHit.getLocation());
                } else if (player != null) {
                    targetedHalf = MixedSlabBlock.bitsandbalance$getTargetedHalf(player, pos);
                }

                BlockState particleState = targetedHalf == SlabType.TOP ? slot(stored, 1) : slot(stored, 0);
                if (particleState == null) {
                    particleState = targetedHalf == SlabType.TOP ? slot(stored, 0) : slot(stored, 1);
                }

                VoxelShape shape = targetedHalf == SlabType.TOP ? Block.box(0.0, 8.0, 0.0, 16.0, 16.0, 16.0)
                        : Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
                return particleState != null ? new ParticleSpec(slabHalf(particleState, targetedHalf), shape) : null;
            }

            if (current.getBlock() instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
                Direction facing = current.getValue(VerticalSlabBlock.FACING);
                BlockState particleState = fixedVerticalSlab.getSourceSlab().defaultBlockState();
                if (level.getBlockEntity(pos) instanceof VerticalSlabBlockEntity verticalSlab) {
                    BlockState facingState = verticalSlab.getFacingSlab();
                    BlockState oppositeState = verticalSlab.getOppositeSlab();
                    if (facingState != null) {
                        particleState = facingState;
                    } else if (oppositeState != null) {
                        particleState = oppositeState;
                    }
                }
                return new ParticleSpec(particleState, verticalHalfShape(facing));
            }

            if (current.getBlock() instanceof VerticalSlabBlock) {
                Direction facing = current.getValue(VerticalSlabBlock.FACING);
                if (!(level.getBlockEntity(pos) instanceof VerticalSlabBlockEntity verticalSlab)) return null;

                if (current.getValue(VerticalSlabBlock.DOUBLE)
                        && !VerticalSlabBlock.bitsandbalance$shouldTargetHalf(player, current, verticalSlab)) {
                    BlockState particleState = verticalSlab.getFacingSlab();
                    if (particleState == null) {
                        particleState = verticalSlab.getOppositeSlab();
                    }
                    return particleState != null ? new ParticleSpec(particleState, FULL_SHAPE) : null;
                }

                Direction targeted = facing;
                if (current.getValue(VerticalSlabBlock.DOUBLE) && player != null) {
                    targeted = VerticalSlabBlock.bitsandbalance$getTargetedHalf(player, pos, facing);
                }

                BlockState particleState = targeted == facing ? verticalSlab.getFacingSlab() : verticalSlab.getOppositeSlab();
                if (particleState == null) {
                    particleState = targeted == facing ? verticalSlab.getOppositeSlab() : verticalSlab.getFacingSlab();
                }

                return particleState != null ? new ParticleSpec(particleState, verticalHalfShape(targeted)) : null;
            }

            if (current.getBlock() instanceof FixedStepBlock fixedStep) {
                BlockState particleState = null;
                if (level.getBlockEntity(pos) instanceof StepBlockEntity step && step.getSlabState() != null) {
                    particleState = step.getSlabState();
                }
                if (particleState == null) {
                    particleState = EnhancedSlabParticleStateCache.peek(level, pos);
                }
                if (particleState == null) {
                    particleState = fixedStep.getSourceSlab().defaultBlockState();
                }
                return new ParticleSpec(
                        particleState,
                        StepBlock.quadrantShape(current.getValue(StepBlock.FACING), current.getValue(StepBlock.TOP))
                );
            }

            if (current.getBlock() instanceof StepBlock) {
                BlockState particleState = null;
                if (level.getBlockEntity(pos) instanceof StepBlockEntity step && step.getSlabState() != null) {
                    particleState = step.getSlabState();
                }
                if (particleState == null) {
                    EnhancedSlabPlacementPredictionCache.SingleStepPrediction prediction = EnhancedSlabPlacementPredictionCache.peekSingleStep(level, pos);
                    if (prediction != null && current.equals(prediction.stepState())) {
                        particleState = prediction.slabState();
                    }
                }
                if (particleState == null) {
                    particleState = EnhancedSlabParticleStateCache.peek(level, pos);
                }
                return particleState != null ? new ParticleSpec(
                        particleState,
                        StepBlock.quadrantShape(current.getValue(StepBlock.FACING), current.getValue(StepBlock.TOP))
                ) : null;
            }

            if (current.getBlock() instanceof QuadStepBlock) {
                if (player == null) return null;
                if (!(level.getBlockEntity(pos) instanceof QuadStepBlockEntity quad)) return null;

                Direction.Axis axis = current.getValue(QuadStepBlock.AXIS);
                int idx = QuadStepBlock.bitsandbalance$getTargetedIndex(player, pos, axis);
                idx = QuadStepBlock.bitsandbalance$resolveOccupiedIndex(quad, idx);
                if (idx < 0) return null;

                BlockState particleState = quad.getSlabAt(idx);
                if (particleState == null) return null;

                VoxelShape shape = Shapes.empty();
                for (int i = 0; i < 4; i++) {
                    BlockState slab = quad.getSlabAt(i);
                    if (slab == null) continue;
                    if (player.isShiftKeyDown()) {
                        if (i == idx) {
                            shape = Shapes.or(shape, QuadStepBlock.shapeForIndex(axis, i));
                        }
                    } else if (particleState.equals(slab)) {
                        shape = Shapes.or(shape, QuadStepBlock.shapeForIndex(axis, i));
                    }
                }
                return !shape.isEmpty() ? new ParticleSpec(particleState, shape) : null;
            }

            if (current.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
                BlockState particleState = null;
                if (level.getBlockEntity(pos) instanceof VerticalStepBlockEntity verticalStep && verticalStep.getSlabState() != null) {
                    particleState = verticalStep.getSlabState();
                }
                if (particleState == null) {
                    particleState = EnhancedSlabParticleStateCache.peek(level, pos);
                }
                if (particleState == null) {
                    particleState = sourceRenderState(fixedVerticalStep.getSourceVerticalSlab());
                }
                return new ParticleSpec(
                        particleState,
                        VerticalStepBlock.quadrantShape(current.getValue(VerticalStepBlock.FACING), current.getValue(VerticalStepBlock.SIDE))
                );
            }

            if (current.getBlock() instanceof VerticalStepBlock) {
                BlockState particleState = null;
                if (level.getBlockEntity(pos) instanceof VerticalStepBlockEntity verticalStep && verticalStep.getSlabState() != null) {
                    particleState = verticalStep.getSlabState();
                }
                if (particleState == null) {
                    EnhancedSlabPlacementPredictionCache.SingleVerticalStepPrediction prediction = EnhancedSlabPlacementPredictionCache.peekSingleVerticalStep(level, pos);
                    if (prediction != null && current.equals(prediction.stepState())) {
                        particleState = prediction.slabState();
                    }
                }
                if (particleState == null) {
                    particleState = EnhancedSlabParticleStateCache.peek(level, pos);
                }
                return particleState != null ? new ParticleSpec(
                        particleState,
                        VerticalStepBlock.quadrantShape(current.getValue(VerticalStepBlock.FACING), current.getValue(VerticalStepBlock.SIDE))
                ) : null;
            }

            if (current.getBlock() instanceof QuadVerticalStepBlock) {
                if (player == null) return null;
                if (!(level.getBlockEntity(pos) instanceof QuadVerticalStepBlockEntity quad)) return null;

                int idx = QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, pos);
                idx = QuadVerticalStepBlock.bitsandbalance$resolveOccupiedIndex(quad, idx);
                if (idx < 0) return null;

                BlockState particleState = quad.getSlabAt(idx);
                if (particleState == null) return null;

                VoxelShape shape = Shapes.empty();
                for (int i = 0; i < 4; i++) {
                    BlockState slab = quad.getSlabAt(i);
                    if (slab == null) continue;
                    if (player.isShiftKeyDown()) {
                        if (i == idx) {
                            shape = Shapes.or(shape, QuadVerticalStepBlock.shapeForIndex(i));
                        }
                    } else if (particleState.equals(slab)) {
                        shape = Shapes.or(shape, QuadVerticalStepBlock.shapeForIndex(i));
                    }
                }
                return !shape.isEmpty() ? new ParticleSpec(particleState, shape) : null;
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static void spawn(ClientLevel level, BlockPos pos, BlockState particleState, VoxelShape shape) {
        BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, particleState);
        List<AABB> boxes = shape.toAabbs();
        int spawnedCount = EnhancedSlabDestroyParticleGeometry.emitGrid(pos, shape,
                (box, motion) -> bitsandbalance$spawnDestroyParticle(level, option, motion));

        if (spawnedCount >= VANILLA_DESTROY_PARTICLE_COUNT || boxes.isEmpty()) {
            return;
        }

        double totalVolume = 0.0D;
        for (AABB box : boxes) {
            totalVolume += bitsandbalance$volume(box);
        }
        if (totalVolume <= 0.0D) {
            return;
        }

        while (spawnedCount < VANILLA_DESTROY_PARTICLE_COUNT) {
            AABB box = bitsandbalance$pickWeightedBox(level, boxes, totalVolume);
            double x = pos.getX() + bitsandbalance$randomCoord(level, box.minX, box.maxX);
            double y = pos.getY() + bitsandbalance$randomCoord(level, box.minY, box.maxY);
            double z = pos.getZ() + bitsandbalance$randomCoord(level, box.minZ, box.maxZ);
            EnhancedSlabDestroyParticleGeometry.ParticleMotion motion =
                    EnhancedSlabDestroyParticleGeometry.motionFor(pos, box, x, y, z);
            bitsandbalance$spawnDestroyParticle(level, option, motion);
            spawnedCount++;
        }
    }

    private static void spawnHit(ClientLevel level, BlockPos pos, Direction direction, BlockState particleState, VoxelShape shape) {
        AABB bounds = shape.bounds();
        double x = pos.getX() + bitsandbalance$particleCoord(level, bounds.minX, bounds.maxX);
        double y = pos.getY() + bitsandbalance$particleCoord(level, bounds.minY, bounds.maxY);
        double z = pos.getZ() + bitsandbalance$particleCoord(level, bounds.minZ, bounds.maxZ);
        double inset = 0.1D;

        switch (direction) {
            case DOWN -> y = pos.getY() + bounds.minY - inset;
            case UP -> y = pos.getY() + bounds.maxY + inset;
            case NORTH -> z = pos.getZ() + bounds.minZ - inset;
            case SOUTH -> z = pos.getZ() + bounds.maxZ + inset;
            case WEST -> x = pos.getX() + bounds.minX - inset;
            case EAST -> x = pos.getX() + bounds.maxX + inset;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.particleEngine == null) return;

        minecraft.particleEngine.add(
                new TerrainParticle(level, x, y, z, 0.0D, 0.0D, 0.0D, particleState, pos)
                        .setPower(0.2F)
                        .scale(0.6F)
        );
    }

    private static double bitsandbalance$particleCoord(ClientLevel level, double min, double max) {
        if ((max - min) <= 0.2D) {
            return (min + max) * 0.5D;
        }
        return Mth.nextDouble(level.getRandom(), min + 0.1D, max - 0.1D);
    }

    private static void bitsandbalance$spawnDestroyParticle(ClientLevel level,
                                                            BlockParticleOption option,
                                                            EnhancedSlabDestroyParticleGeometry.ParticleMotion motion) {
        level.addParticle(option,
                motion.x(), motion.y(), motion.z(),
                motion.vx(), motion.vy(), motion.vz());
    }

    private static double bitsandbalance$randomCoord(ClientLevel level, double min, double max) {
        if (max <= min) {
            return min;
        }
        return Mth.nextDouble(level.getRandom(), min, max);
    }

    private static double bitsandbalance$volume(AABB box) {
        return Math.max(0.0D, box.maxX - box.minX)
                * Math.max(0.0D, box.maxY - box.minY)
                * Math.max(0.0D, box.maxZ - box.minZ);
    }

    private static AABB bitsandbalance$pickWeightedBox(ClientLevel level, List<AABB> boxes, double totalVolume) {
        double target = Mth.nextDouble(level.getRandom(), 0.0D, totalVolume);
        double running = 0.0D;
        for (AABB box : boxes) {
            running += bitsandbalance$volume(box);
            if (target <= running) {
                return box;
            }
        }
        return boxes.get(boxes.size() - 1);
    }

    private static VoxelShape verticalHalfShape(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH_HALF_SHAPE;
            case SOUTH -> SOUTH_HALF_SHAPE;
            case WEST -> WEST_HALF_SHAPE;
            case EAST -> EAST_HALF_SHAPE;
            default -> NORTH_HALF_SHAPE;
        };
    }

    private static BlockState[] materialSlots(BlockGetter level, BlockPos pos) {
        if (level instanceof Level actualLevel) {
            return EnhancedSlabMaterialSavedData.getClient(actualLevel, pos.asLong());
        }
        return EnhancedSlabMaterialSavedData.getClient(pos.asLong());
    }

    private static BlockState slot(@Nullable BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static BlockState slabHalf(BlockState state, SlabType half) {
        if (state != null && state.hasProperty(SlabBlock.TYPE)) {
            return state.setValue(SlabBlock.TYPE, half);
        }
        return state;
    }

    private static BlockState sourceRenderState(Block sourceVerticalSlab) {
        Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
        return sourceSlab != null ? sourceSlab.defaultBlockState() : sourceVerticalSlab.defaultBlockState();
    }

    private static final class ParticleSpec {
        private final BlockState particleState;
        private final VoxelShape shape;

        private ParticleSpec(BlockState particleState, VoxelShape shape) {
            this.particleState = particleState;
            this.shape = shape;
        }
    }
}
