package org.onenonly.bitsandbalance.common.mechanics;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;

/**
 * Tweaks: Enhanced Slab Behavior
 *
 * Shared helper logic for the Enhanced Slab feature set:
 *   - Hanging items below top/double slabs
 *   - KneeSlab-like independent mining of double slabs
 */
public final class EnhancedSlabHelper {

    private EnhancedSlabHelper() {
    }

    private static final ThreadLocal<Long2DoubleOpenHashMap> bitsandbalance$yOffCache =
            ThreadLocal.withInitial(() -> {
                Long2DoubleOpenHashMap map = new Long2DoubleOpenHashMap();
                map.defaultReturnValue(Double.NaN);
                return map;
            });

    private static final AtomicLong bitsandbalance$yOffCacheEpoch = new AtomicLong(0L);
    private static final ThreadLocal<Long> bitsandbalance$yOffCacheEpochSeen = ThreadLocal.withInitial(() -> -1L);

        /**
         * Data-driven allowlist for which blocks may participate in the Enhanced Slab
         * visual-offset system.
         *
         * <p>This is intentionally an allowlist (not a denylist) so we can keep
         * decoration-style blocks (torches/lanterns/chains/doors/etc.) sitting on
         * bottom slabs without also shifting every full block.</p>
         */
        private static final TagKey<net.minecraft.world.level.block.Block> BITSANDBALANCE$ENHANCED_SLAB_VISUAL_OFFSET =
            TagKey.create(Registries.BLOCK, Identifier.parse("bitsandbalance:enhanced_slab_visual_offset"));

    private static final Field BITSANDBALANCE$HOLDER_TAGS_FIELD;

    static {
        Field found = null;
        for (Field field : Holder.Reference.class.getDeclaredFields()) {
            if (!Set.class.isAssignableFrom(field.getType())) continue;
            try {
                field.setAccessible(true);
                found = field;
            } catch (Throwable ignored) {
            }
            break;
        }
        BITSANDBALANCE$HOLDER_TAGS_FIELD = found;
    }

    public static void bumpVisualYOffsetCacheEpoch() {
        bitsandbalance$yOffCacheEpoch.incrementAndGet();
    }

    // ───────────────────────────────────────────────────────────────
    //  Support-surface helpers (place on top / hang below)
    // ───────────────────────────────────────────────────────────────

    /**
     * Checks if a slab block provides a full top surface for placing items on top.
     *
     * <p>Note: the "place-on-top" portion of Enhanced Slabs is currently disabled,
     * so this helper returns {@code false}.</p>
     */
    public static boolean hasFullTopSurface(BlockState state) {
        return false;
    }

    /**
     * Checks if a slab block provides a full bottom surface for hanging items.
     * Top slabs and double slabs have a solid bottom surface.
     */
    public static boolean hasFullBottomSurface(BlockState state) {
        if (!(state.getBlock() instanceof SlabBlock)) return false;
        SlabType type = state.getValue(SlabBlock.TYPE);
        return type == SlabType.TOP || type == SlabType.DOUBLE;
    }

    /**
     * Checks whether a block presents a solid underside at top-slab height.
     *
     * <p>This is used for hanging attachments below top slabs and top-aligned
     * steps. For quad steps, any occupied top quadrant counts.</p>
     */
    public static boolean hasFullBottomSurface(BlockGetter level, BlockPos pos, BlockState state) {
        if (hasFullBottomSurface(state)) return true;

        if (state.getBlock() instanceof StepBlock) {
            return state.getValue(StepBlock.TOP);
        }

        if (state.getBlock() instanceof QuadStepBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof QuadStepBlockEntity quad)) return false;
            return quad.getSlabAt(QuadStepBlock.IDX_TOP_WEST) != null
                || quad.getSlabAt(QuadStepBlock.IDX_TOP_EAST) != null;
        }

        return false;
    }

    /**
     * Returns whether a block should behave like a half-block hang-below anchor for visual offset.
     *
     * <p>Quad-step bundles with 3 or 4 occupied quadrants are treated as effectively full blocks
     * for foreign block placement/visuals, so hang-below blocks should remain in the next full
     * block space below instead of being pulled up into the quad-step cell.</p>
     */
    private static boolean bitsandbalance$hasHalfBlockHangBelowOffsetSurface(BlockGetter level, BlockPos pos, BlockState state) {
        if (hasFullBottomSurface(state)) return true;

        if (state.getBlock() instanceof StepBlock) {
            return state.getValue(StepBlock.TOP);
        }

        if (state.getBlock() instanceof QuadStepBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof QuadStepBlockEntity quad)) return false;
            if (quad.getCount() >= 3) return false;
            return quad.getSlabAt(QuadStepBlock.IDX_TOP_WEST) != null
                || quad.getSlabAt(QuadStepBlock.IDX_TOP_EAST) != null;
        }

        return false;
    }

    // ───────────────────────────────────────────────────────────────
    //  KneeSlab mining helpers
    // ───────────────────────────────────────────────────────────────

    /**
     * Determines which half of a double slab the player is looking at.
     * Returns SlabType.BOTTOM or SlabType.TOP based on the hit position.
     */
    public static SlabType getTargetedHalf(BlockPos pos, Vec3 hitVec) {
        double relativeY = hitVec.y - pos.getY();
        return relativeY > 0.5 ? SlabType.TOP : SlabType.BOTTOM;
    }

    /**
     * Safe client/server-friendly targeting helper that does not call {@code player.pick()}.
     *
     * <p>We ray-clip against a full cube at {@code pos} so we never depend on block shape
     * lookups that can recurse back into slab shape mixins.</p>
     */
    public static SlabType getTargetedHalfFromRayToFullCube(Player player, BlockPos pos) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));

        BlockHitResult clipped = Shapes.block().clip(start, end, pos);
        if (clipped != null) {
            return getTargetedHalf(pos, clipped.getLocation());
        }

        double relativeY = player.getEyeY() - pos.getY();
        return relativeY > 0.5 ? SlabType.TOP : SlabType.BOTTOM;
    }

    /**
     * Gets the BlockState that the targeted half of a double slab would have
     * if it were a standalone slab. Used for mining speed calculations.
     *
     * For mixed double slabs stored via our block entity, this returns the
     * appropriate slab type's state. For vanilla same-type double slabs,
     * returns the same block as a half-slab.
     */
    public static BlockState getHalfSlabState(BlockState doubleSlabState, SlabType targetedHalf) {
        if (!(doubleSlabState.getBlock() instanceof SlabBlock)) return doubleSlabState;
        SlabType current = doubleSlabState.getValue(SlabBlock.TYPE);
        if (current != SlabType.DOUBLE) return doubleSlabState;

        // For vanilla double slabs (same type), just return the half-slab variant
        return doubleSlabState.setValue(SlabBlock.TYPE, targetedHalf);
    }

    /**
     * After mining one half of a double slab, return the remaining half's state.
     */
    public static BlockState getRemainingHalfState(BlockState doubleSlabState, SlabType minedHalf) {
        if (!(doubleSlabState.getBlock() instanceof SlabBlock)) return doubleSlabState;

        SlabType remaining = (minedHalf == SlabType.TOP) ? SlabType.BOTTOM : SlabType.TOP;
        return doubleSlabState.setValue(SlabBlock.TYPE, remaining);
    }

    /**
     * Checks whether a block state is a double slab.
     */
    public static boolean isDoubleSlab(BlockState state) {
        if (!(state.getBlock() instanceof SlabBlock)) return false;
        return state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE;
    }

    /**
     * Resolves the slab-half state whose step sound should be used when an entity walks
     * on a compound enhanced-slab block.
     */
    public static BlockState resolveStepSoundState(Entity entity, @Nullable BlockPos pos, BlockState state) {
        BlockPos soundPos = pos != null ? pos : entity.getOnPos();
        BlockEntity blockEntity = entity.level().getBlockEntity(soundPos);

        if (state.getBlock() instanceof MixedSlabBlock) {
            if (!(blockEntity instanceof MixedSlabBlockEntity mixedSlab)) return state;
            BlockState topSlab = mixedSlab.getTopSlab();
            return topSlab != null ? topSlab : state;
        }

        if (state.getBlock() instanceof FixedStepBlock fixedStep) {
            return fixedStep.getSourceSlab().defaultBlockState();
        }

        if (state.getBlock() instanceof StepBlock) {
            if (blockEntity instanceof StepBlockEntity step && step.getSlabState() != null) {
                return step.getSlabState();
            }
            return state;
        }

        if (state.getBlock() instanceof QuadStepBlock) {
            if (!(blockEntity instanceof QuadStepBlockEntity quad)) return state;
            int preferred = QuadStepBlock.toIndex(
                    bitsandbalance$isPositive(entity, soundPos, state.getValue(QuadStepBlock.AXIS)),
                    bitsandbalance$isTop(entity, soundPos)
            );
            int resolvedIndex = QuadStepBlock.bitsandbalance$resolveOccupiedIndex(quad, preferred);
            BlockState resolved = quad.getSlabAt(resolvedIndex);
            return resolved != null ? resolved : state;
        }

        if (state.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
            return bitsandbalance$sourceRenderState(fixedVerticalStep.getSourceVerticalSlab());
        }

        if (state.getBlock() instanceof VerticalStepBlock) {
            if (blockEntity instanceof VerticalStepBlockEntity verticalStep && verticalStep.getSlabState() != null) {
                return verticalStep.getSlabState();
            }
            return state;
        }

        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            if (!(blockEntity instanceof QuadVerticalStepBlockEntity quad)) return state;
            int preferred = QuadVerticalStepBlock.toIndex(
                    bitsandbalance$isEast(entity, soundPos),
                    bitsandbalance$isSouth(entity, soundPos)
            );
            int resolvedIndex = QuadVerticalStepBlock.bitsandbalance$resolveOccupiedIndex(quad, preferred);
            BlockState resolved = quad.getSlabAt(resolvedIndex);
            return resolved != null ? resolved : state;
        }

        return state;
    }

    private static BlockState bitsandbalance$sourceRenderState(net.minecraft.world.level.block.Block sourceVerticalSlab) {
        net.minecraft.world.level.block.Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
        if (sourceSlab != null) return sourceSlab.defaultBlockState();
        return sourceVerticalSlab.defaultBlockState();
    }

    private static boolean bitsandbalance$isTop(Entity entity, BlockPos pos) {
        return (entity.getY() - pos.getY()) > 0.5D;
    }

    private static boolean bitsandbalance$isPositive(Entity entity, BlockPos pos, Direction.Axis axis) {
        return axis == Direction.Axis.Z
                ? bitsandbalance$isSouth(entity, pos)
                : bitsandbalance$isEast(entity, pos);
    }

    private static boolean bitsandbalance$isEast(Entity entity, BlockPos pos) {
        return (entity.getX() - pos.getX()) > 0.5D;
    }

    private static boolean bitsandbalance$isSouth(Entity entity, BlockPos pos) {
        return (entity.getZ() - pos.getZ()) > 0.5D;
    }

    /**
     * Resolves the slab-half state whose mining hit sound should be used while the player
     * is breaking an enhanced slab variant.
     */
    public static BlockState resolveMiningHitSoundState(BlockGetter level, @Nullable Player player,
                                                        @Nullable BlockHitResult hitResult, BlockPos pos,
                                                        BlockState state) {
        if (state.getBlock() instanceof MixedSlabBlock) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof MixedSlabBlockEntity mixedSlab)) return state;

            SlabType targetedHalf = null;
            if (hitResult != null && pos.equals(hitResult.getBlockPos())) {
                targetedHalf = getTargetedHalf(pos, hitResult.getLocation());
            } else if (player != null) {
                targetedHalf = getTargetedHalfFromRayToFullCube(player, pos);
            }

            if (targetedHalf == null) return state;
            BlockState halfState = targetedHalf == SlabType.TOP ? mixedSlab.getTopSlab() : mixedSlab.getBottomSlab();
            return halfState != null ? halfState : state;
        }

        if (state.getBlock() instanceof VerticalSlabBlock) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof VerticalSlabBlockEntity verticalSlab)) return state;

            if (!state.getValue(VerticalSlabBlock.DOUBLE)) {
                BlockState halfState = verticalSlab.getFacingSlab();
                return halfState != null ? halfState : state;
            }

            Direction facing = state.getValue(VerticalSlabBlock.FACING);
            Direction targetedDir = null;
            if (hitResult != null && pos.equals(hitResult.getBlockPos())) {
                targetedDir = VerticalSlabBlock.bitsandbalance$getTargetedHalf(pos, hitResult.getLocation(), facing);
            } else if (player != null) {
                targetedDir = VerticalSlabBlock.bitsandbalance$getTargetedHalf(player, pos, facing);
            }

            if (targetedDir == null) return state;
            BlockState halfState = (targetedDir == facing) ? verticalSlab.getFacingSlab() : verticalSlab.getOppositeSlab();
            return halfState != null ? halfState : state;
        }

        if (state.getBlock() instanceof QuadStepBlock) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof QuadStepBlockEntity quadStep)) return state;

            int targetedIndex = -1;
            if (player != null) {
                Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
                targetedIndex = QuadStepBlock.bitsandbalance$getTargetedIndex(player, pos, axis);
            }

            if (targetedIndex < 0) return state;
            BlockState targetedState = quadStep.getSlabAt(targetedIndex);
            return targetedState != null ? targetedState : state;
        }

        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep)) return state;

            int targetedIndex = -1;
            if (player != null) {
                targetedIndex = QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, pos);
            }

            if (targetedIndex < 0) return state;
            BlockState targetedState = quadVerticalStep.getSlabAt(targetedIndex);
            return targetedState != null ? targetedState : state;
        }

        return state;
    }

    // ───────────────────────────────────────────────────────────────
    //  Visual-offset helpers (outline shape + render alignment)
    // ───────────────────────────────────────────────────────────────

    /**
     * Checks if the block directly below is a bottom slab.
     */
    public static boolean hasBottomSlabBelow(BlockGetter level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof SlabBlock
            && below.getValue(SlabBlock.TYPE) == SlabType.BOTTOM;
    }

    /**
     * Checks if the block directly above is a top slab.
     */
    public static boolean hasTopSlabAbove(BlockGetter level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.getBlock() instanceof SlabBlock
            && above.getValue(SlabBlock.TYPE) == SlabType.TOP;
    }

    /**
     * Returns {@code true} for block types that break slab-grid offset chains.
     *
     * <p>{@link SlabBlock} is intentionally <b>not</b> treated as excluded here.
     * Slabs participate in the chain and are handled explicitly in
     * {@link #getVisualYOffset}.</p>
     */
    public static boolean shouldExcludeFromVisualOffset(BlockState state) {
        if (state.isAir()) return true;
        var block = state.getBlock();
        if (block instanceof SlabBlock) return false;

        // Hard exclusions: chain breakers / never offset.
        if (block instanceof StairBlock)     return true;
        if (block instanceof IronBarsBlock)  return true;
        if (block instanceof LiquidBlock)    return true;

        // Default: only a decoration-focused allowlist participates.
        return !bitsandbalance$isVisualOffsetEligibleNonSlab(state);
    }

    private static boolean bitsandbalance$isVisualOffsetEligibleNonSlab(BlockState state) {
        if (state == null || state.isAir()) return false;

        // Data-driven escape hatch for modded blocks.
        if (bitsandbalance$hasBoundTag(state, BITSANDBALANCE$ENHANCED_SLAB_VISUAL_OFFSET)) return true;

        var block = state.getBlock();

        // Keep common decoration/attachment blocks eligible.
        if (block instanceof DoorBlock) return true;
        if (block instanceof ButtonBlock) return true;
        if (block instanceof PressurePlateBlock) return true;
        if (block instanceof TorchBlock) return true;
        if (block instanceof LanternBlock) return true;
        if (block instanceof LeverBlock) return true;
        if (block instanceof BellBlock) return true;
        if (block instanceof CarpetBlock) return true;
        if (block instanceof FlowerPotBlock) return true;
        if (block instanceof CandleBlock) return true;
        if (block instanceof CandleCakeBlock) return true;

        // Tag-based vanilla groups (covers most variants).
        if (bitsandbalance$hasBoundTag(state, BlockTags.BUTTONS)) return true;
        if (bitsandbalance$hasBoundTag(state, BlockTags.PRESSURE_PLATES)) return true;
        if (bitsandbalance$hasBoundTag(state, BlockTags.STANDING_SIGNS)) return true;
        if (bitsandbalance$hasBoundTag(state, BlockTags.WALL_SIGNS)) return true;
        if (bitsandbalance$hasBoundTag(state, BlockTags.CEILING_HANGING_SIGNS)) return true;
        if (bitsandbalance$hasBoundTag(state, BlockTags.WALL_HANGING_SIGNS)) return true;

        return false;
    }

    private static boolean bitsandbalance$hasBoundTag(BlockState state, TagKey<net.minecraft.world.level.block.Block> tag) {
        if (state == null || tag == null || BITSANDBALANCE$HOLDER_TAGS_FIELD == null) return false;

        try {
            @SuppressWarnings("unchecked")
            Set<TagKey<net.minecraft.world.level.block.Block>> tags =
                    (Set<TagKey<net.minecraft.world.level.block.Block>>) BITSANDBALANCE$HOLDER_TAGS_FIELD.get(state.getBlock().builtInRegistryHolder());
            return tags != null && tags.contains(tag);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Visual Y offset for rendering and selection shapes of blocks in a slab grid.
     *
     * <p>Each bottom slab found while walking downward contributes {@code -0.5}
     * to the cumulative offset. Regular solid blocks between slabs pass through
    * transparently. The chain stops when it hits air or a chain-breaking block
    * (stairs/panes/fluids), or when it hits a slab that
     * is not a bottom slab (TOP/DOUBLE).</p>
     *
     * <p>Similarly, each top slab found while walking upward contributes
     * {@code +0.5}.</p>
     */
    public static double getVisualYOffset(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir()) return 0.0;

        boolean isSlab = state.getBlock() instanceof SlabBlock;
        if (!isSlab && shouldExcludeFromVisualOffset(state)) return 0.0;

        // We currently want vanilla behavior for bottom slabs.
        // That means: do NOT shift blocks downward based on bottom-slab stacks.
        // (This used to be the "place-on-top" / bottom-slab-grid portion.)

        Long2DoubleOpenHashMap cache = bitsandbalance$yOffCache.get();
        long epoch = bitsandbalance$yOffCacheEpoch.get();
        long seen = bitsandbalance$yOffCacheEpochSeen.get();
        if (seen != epoch) {
            cache.clear();
            bitsandbalance$yOffCacheEpochSeen.set(epoch);
        }
        if (cache.size() > 16384) cache.clear();

        long key = pos.asLong();
        double cached = cache.get(key);
        if (!Double.isNaN(cached)) return cached;

        double up = bitsandbalance$computeUpOffset(level, pos, state, 0, cache);
        cache.put(key, up);
        return up;
    }

    private static double bitsandbalance$computeDownOffset(BlockGetter level, BlockPos pos, BlockState state,
                                                          int depth, Long2DoubleOpenHashMap cache) {
        if (depth >= 256) return 0.0;
        if (state.isAir()) return 0.0;

        boolean isSlab = state.getBlock() instanceof SlabBlock;
        if (!isSlab && shouldExcludeFromVisualOffset(state)) return 0.0;
        if (isSlab && state.getValue(SlabBlock.TYPE) == SlabType.TOP) return 0.0;
        if (isSlab && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) return 0.0;

        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        if (shouldExcludeFromVisualOffset(below)) return 0.0;

        long belowKey = belowPos.asLong();
        double belowCached = cache.get(belowKey);
        double belowOff;
        if (!Double.isNaN(belowCached)) {
            belowOff = belowCached;
        } else {
            belowOff = bitsandbalance$computeDownOffset(level, belowPos, below, depth + 1, cache);
            // Only store downward results if non-zero; upward results will be stored by the caller.
            if (belowOff != 0.0) cache.put(belowKey, belowOff);
        }

        if (below.getBlock() instanceof SlabBlock) {
            SlabType type = below.getValue(SlabBlock.TYPE);
            if (type == SlabType.BOTTOM) return belowOff - 0.5;
            return 0.0;
        }

        return belowOff;
    }

    private static double bitsandbalance$computeUpOffset(BlockGetter level, BlockPos pos, BlockState state,
                                                        int depth, Long2DoubleOpenHashMap cache) {
        if (depth >= 256) return 0.0;
        if (state.isAir()) return 0.0;

        boolean isSlab = state.getBlock() instanceof SlabBlock;
        if (!isSlab && shouldExcludeFromVisualOffset(state)) return 0.0;
        if (isSlab && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) return 0.0;
        if (isSlab && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) return 0.0;

        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);
        if (shouldExcludeFromVisualOffset(above) && !bitsandbalance$hasHalfBlockHangBelowOffsetSurface(level, abovePos, above)) return 0.0;

        long aboveKey = abovePos.asLong();
        double aboveCached = cache.get(aboveKey);
        double aboveOff;
        if (!Double.isNaN(aboveCached)) {
            aboveOff = aboveCached;
        } else {
            aboveOff = bitsandbalance$computeUpOffset(level, abovePos, above, depth + 1, cache);
            if (aboveOff != 0.0) cache.put(aboveKey, aboveOff);
        }

        if (above.getBlock() instanceof SlabBlock) {
            SlabType type = above.getValue(SlabBlock.TYPE);
            if (type == SlabType.TOP) return aboveOff + 0.5;
            return 0.0;
        }

        if (bitsandbalance$hasHalfBlockHangBelowOffsetSurface(level, abovePos, above)) {
            return aboveOff + 0.5;
        }

        return aboveOff;
    }
}
