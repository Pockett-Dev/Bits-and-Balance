package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPlacementPredictionCache;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabStorageOptimizer;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tweaks: Enhanced Slab Behavior — Quad Step Container
 *
 * A container block that holds 2–4 different step quadrants in a single block position.
 * Each quadrant occupies one of:
 * <ul>
 *   <li>Index {@value #IDX_BOTTOM_WEST} — bottom-negative side (Y 0-8)</li>
 *   <li>Index {@value #IDX_BOTTOM_EAST} — bottom-positive side (Y 0-8)</li>
 *   <li>Index {@value #IDX_TOP_WEST}    — top-negative side    (Y 8-16)</li>
 *   <li>Index {@value #IDX_TOP_EAST}    — top-positive side    (Y 8-16)</li>
 * </ul>
 * The meaning of “negative/positive” depends on {@link #AXIS}:
 * <ul>
 *   <li>AXIS=X: negative=WEST, positive=EAST</li>
 *   <li>AXIS=Z: negative=NORTH, positive=SOUTH</li>
 * </ul>
 *
 * <h3>KneeSlab-style mining (crouch-only)</h3>
 * In survival while crouching, only the targeted quadrant is removed; remaining
 * quadrants stay in place. Without crouching the whole block is mined and all
 * occupied quadrants drop as items.
 */
public class QuadStepBlock extends Block implements SimpleWaterloggedBlock {

    // ── Quadrant indices ──────────────────────────────────────────────────────
    public static final int IDX_BOTTOM_WEST = 0;
    public static final int IDX_BOTTOM_EAST = 1;
    public static final int IDX_TOP_WEST    = 2;
    public static final int IDX_TOP_EAST    = 3;

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Axis-lock for quad steps (X vs Z). */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private static final ThreadLocal<CachedBreak> CACHED_BREAK = new ThreadLocal<>();
    private static final int bitsandbalance$UNION_CACHE_LIMIT = 4096;
    private static final Object bitsandbalance$UNION_CACHE_LOCK = new Object();
    private static final Map<QuadShapeKey, VoxelShape> bitsandbalance$UNION_CACHE = bitsandbalance$createBoundedCache(bitsandbalance$UNION_CACHE_LIMIT);

    public QuadStepBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(AXIS, Direction.Axis.X));
    }

    // ── Static cache accessors (shared with destroy mixin) ────────────────────

    /**
     * Returns the {@link BlockState} of the targeted slab quadrant if a break is in progress at
     * the given position, otherwise {@code null}.
     */
    public static @Nullable BlockState bitsandbalance$peekCachedMinedSlab(BlockPos pos) {
        CachedBreak c = CACHED_BREAK.get();
        return (c != null && c.posLong == pos.asLong()) ? c.minedSlab : null;
    }

    /**
     * Returns the remaining slab array (4-element, some {@code null}) after removing the targeted
     * quadrant, if a break is in progress at the given position, otherwise {@code null}.
     */
    public static BlockState @Nullable [] bitsandbalance$peekCachedRemainingSlabs(BlockPos pos) {
        CachedBreak c = CACHED_BREAK.get();
        return (c != null && c.posLong == pos.asLong()) ? c.remainingSlabs : null;
    }

    public static int bitsandbalance$peekCachedRemovedCount(BlockPos pos) {
        CachedBreak c = CACHED_BREAK.get();
        return (c != null && c.posLong == pos.asLong()) ? c.removedCount : 0;
    }

    /** Returns the targeted quadrant index if a break is in progress, otherwise {@code -1}. */
    public static int bitsandbalance$peekCachedTargetedIndex(BlockPos pos) {
        CachedBreak c = CACHED_BREAK.get();
        return (c != null && c.posLong == pos.asLong()) ? c.targetedIndex : -1;
    }

    public static void bitsandbalance$clearCachedBreak() {
        CACHED_BREAK.remove();
    }

    // ── Quadrant helpers ──────────────────────────────────────────────────────

    /**
     * Determines the quadrant index the player's view vector intersects when aimed at a full-cube
     * outline at {@code pos}.
     */
    public static int bitsandbalance$getTargetedIndex(Player player, BlockPos pos, Direction.Axis axis) {
        // Prefer clipping to the actually-occupied outline geometry so we can correctly resolve
        // "internal" faces (e.g., a vertical-slab made from two stacked steps has a large face at
        // X/Z==0.5 that a full-cube clip can't see).
        QuadStepBlockEntity quad = null;
        BlockEntity be = player.level().getBlockEntity(pos);
        if (be instanceof QuadStepBlockEntity q) quad = q;
        BlockState[] slots = quad != null ? quad.getAllSlabs()
                : EnhancedSlabMaterialSavedData.get(player.level(), pos.asLong());

        BlockHitResult directHit = EnhancedSlabHelper.getCurrentClientBlockHit(player, pos);
        if (directHit != null) {
            int clipped = bitsandbalance$clipTargetedIndex(player, pos, axis, slots, directHit.getLocation());
            if (clipped >= 0) {
                return clipped;
            }
            int preferred = bitsandbalance$getTargetedIndex(pos, directHit.getLocation(), directHit.getDirection(), axis);
            return quad != null ? bitsandbalance$resolveOccupiedIndex(quad, preferred)
                    : slots != null ? bitsandbalance$resolveOccupiedIndex(slots, preferred) : preferred;
        }

        int clipped = bitsandbalance$clipTargetedIndex(player, pos, axis, slots, null);
        if (clipped >= 0) {
            return clipped;
        }

        var ray = EnhancedSlabHelper.getInteractionRay(player, pos);
        var hit = (quad != null ? occupiedUnion(axis, quad) : Shapes.empty()).clip(ray.start(), ray.end(), pos);
        if (hit == null) {
            // Fallback: clip to full cube for a stable answer even if the quad is missing.
            hit = Shapes.block().clip(ray.start(), ray.end(), pos);
        }

        double hitX, hitY, hitZ;
        Direction face;
        if (hit != null) {
            hitX = hit.getLocation().x - pos.getX();
            hitY = hit.getLocation().y - pos.getY();
            hitZ = hit.getLocation().z - pos.getZ();
            face = hit.getDirection();
        } else {
            // Fallback: project eye onto block volume
            hitX = player.getX() - pos.getX();
            hitY = player.getEyeY() - pos.getY();
            hitZ = player.getZ() - pos.getZ();
            face = null;
        }

        final double EPS = 1.0E-6;
        boolean east = axis == Direction.Axis.X ? (hitX > 0.5) : (hitZ > 0.5);
        boolean top  = hitY > 0.5;

        // Boundary tie-breakers: if we land exactly on the 0.5 split plane, use the hit face.
        if (axis == Direction.Axis.X && Math.abs(hitX - 0.5) < EPS && face != null) {
            if (face == Direction.WEST) east = true;
            else if (face == Direction.EAST) east = false;
        } else if (axis == Direction.Axis.Z && Math.abs(hitZ - 0.5) < EPS && face != null) {
            if (face == Direction.NORTH) east = true;
            else if (face == Direction.SOUTH) east = false;
        }
        if (Math.abs(hitY - 0.5) < EPS && face != null) {
            if (face == Direction.DOWN) top = true;
            else if (face == Direction.UP) top = false;
        }

        int preferred = toIndex(east, top);
        return quad != null ? bitsandbalance$resolveOccupiedIndex(quad, preferred)
                : slots != null ? bitsandbalance$resolveOccupiedIndex(slots, preferred) : preferred;
    }

    private static int bitsandbalance$getTargetedIndexFromRay(Player player, BlockPos pos, Direction.Axis axis, @Nullable BlockState[] slots) {
        int clipped = bitsandbalance$clipTargetedIndex(player, pos, axis, slots, null);
        if (clipped >= 0) {
            return clipped;
        }

        var ray = EnhancedSlabHelper.getInteractionRay(player, pos);

        VoxelShape shape = slots != null ? occupiedUnion(axis, slots) : Shapes.empty();
        BlockHitResult hit = shape.clip(ray.start(), ray.end(), pos);
        if (hit == null) {
            hit = Shapes.block().clip(ray.start(), ray.end(), pos);
        }
        if (hit != null) {
            return bitsandbalance$getTargetedIndex(pos, hit.getLocation(), hit.getDirection(), axis);
        }

        double hitX = player.getX() - pos.getX();
        double hitY = player.getEyeY() - pos.getY();
        double hitZ = player.getZ() - pos.getZ();
        boolean east = axis == Direction.Axis.X ? hitX > 0.5 : hitZ > 0.5;
        boolean top = hitY > 0.5;
        return toIndex(east, top);
    }

    private static int bitsandbalance$clipTargetedIndex(Player player,
                                                        BlockPos pos,
                                                        Direction.Axis axis,
                                                        @Nullable BlockState[] slots,
                                                        @Nullable Vec3 knownHitLocation) {
        if (player == null || slots == null) {
            return -1;
        }

        var ray = EnhancedSlabHelper.getInteractionRay(player, pos);
        Vec3 start = ray.start();
        Vec3 end = knownHitLocation != null
                ? bitsandbalance$extendPastHit(start, knownHitLocation)
                : ray.end();
        double bestDistance = Double.MAX_VALUE;
        int bestIndex = -1;

        for (int i = 0; i < 4; i++) {
            if (bitsandbalance$slot(slots, i) == null) {
                continue;
            }

            BlockHitResult clipped = shapeForIndex(axis, i).clip(start, end, pos);
            if (clipped == null) {
                continue;
            }

            double distance = start.distanceToSqr(clipped.getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private static Vec3 bitsandbalance$extendPastHit(Vec3 start, Vec3 hitLocation) {
        Vec3 delta = hitLocation.subtract(start);
        double lengthSqr = delta.lengthSqr();
        if (lengthSqr < 1.0E-8) {
            return hitLocation;
        }
        return hitLocation.add(delta.scale(0.01 / Math.sqrt(lengthSqr)));
    }

    /** Back-compat overload (defaults to X-axis). Prefer calling with an explicit axis. */
    public static int bitsandbalance$getTargetedIndex(Player player, BlockPos pos) {
        return bitsandbalance$getTargetedIndex(player, pos, Direction.Axis.X);
    }

    public static int bitsandbalance$getTargetedIndex(BlockPos pos, Vec3 hitVec, @Nullable Direction face, Direction.Axis axis) {
        double hitX = hitVec.x - pos.getX();
        double hitY = hitVec.y - pos.getY();
        double hitZ = hitVec.z - pos.getZ();

        final double EPS = 1.0E-6;
        boolean east = axis == Direction.Axis.X ? (hitX > 0.5) : (hitZ > 0.5);
        boolean top = hitY > 0.5;

        if (axis == Direction.Axis.X && Math.abs(hitX - 0.5) < EPS && face != null) {
            if (face == Direction.WEST) east = true;
            else if (face == Direction.EAST) east = false;
        } else if (axis == Direction.Axis.Z && Math.abs(hitZ - 0.5) < EPS && face != null) {
            if (face == Direction.NORTH) east = true;
            else if (face == Direction.SOUTH) east = false;
        }
        if (Math.abs(hitY - 0.5) < EPS && face != null) {
            if (face == Direction.DOWN) top = true;
            else if (face == Direction.UP) top = false;
        }

        return toIndex(east, top);
    }

    /** Encodes (east, top) booleans to a quadrant index. */
    public static int toIndex(boolean east, boolean top) {
        if (!top && !east) return IDX_BOTTOM_WEST;
        if (!top &&  east) return IDX_BOTTOM_EAST;
        if ( top && !east) return IDX_TOP_WEST;
        return IDX_TOP_EAST;
    }

    /** Returns {@code true} if the index is on the east side. */
    public static boolean isPositive(int index) {
        return index == IDX_BOTTOM_EAST || index == IDX_TOP_EAST;
    }

    /** Back-compat alias for X-axis quads. */
    public static boolean isEast(int index) {
        return isPositive(index);
    }

    /** Returns {@code true} if the index is on the top half. */
    public static boolean isTop(int index) {
        return index == IDX_TOP_WEST || index == IDX_TOP_EAST;
    }

    /** Returns the half-direction for the given index based on the quad's axis. */
    public static Direction sideForIndex(Direction.Axis axis, int index) {
        boolean positive = isPositive(index);
        if (axis == Direction.Axis.Z) return positive ? Direction.SOUTH : Direction.NORTH;
        return positive ? Direction.EAST : Direction.WEST;
    }

    /** Returns the {@link VoxelShape} for the given quadrant index based on the quad's axis. */
    public static VoxelShape shapeForIndex(Direction.Axis axis, int index) {
        return StepBlock.quadrantShape(sideForIndex(axis, index), isTop(index));
    }

    private static int bitsandbalance$pairedVerticalIndex(int index) {
        return switch (index) {
            case IDX_BOTTOM_WEST -> IDX_TOP_WEST;
            case IDX_BOTTOM_EAST -> IDX_TOP_EAST;
            case IDX_TOP_WEST -> IDX_BOTTOM_WEST;
            case IDX_TOP_EAST -> IDX_BOTTOM_EAST;
            default -> -1;
        };
    }

    private static int bitsandbalance$pairedHorizontalIndex(int index) {
        return switch (index) {
            case IDX_BOTTOM_WEST -> IDX_BOTTOM_EAST;
            case IDX_BOTTOM_EAST -> IDX_BOTTOM_WEST;
            case IDX_TOP_WEST -> IDX_TOP_EAST;
            case IDX_TOP_EAST -> IDX_TOP_WEST;
            default -> -1;
        };
    }

    /**
     * Resolves a preferred index to an actually-occupied slot.
     *
     * <p>This is important for boundary hits (notably when aiming at the top face of a bottom
     * quadrant, where the Y coordinate can fall on the 0.5 boundary and produce the wrong half).
     */
    public static int bitsandbalance$resolveOccupiedIndex(QuadStepBlockEntity quad, int preferredIndex) {
        if (quad == null) return -1;
        if (preferredIndex >= 0 && preferredIndex < 4 && quad.getSlabAt(preferredIndex) != null) return preferredIndex;

        int paired = bitsandbalance$pairedVerticalIndex(preferredIndex);
        if (paired >= 0 && paired < 4 && quad.getSlabAt(paired) != null) return paired;

        paired = bitsandbalance$pairedHorizontalIndex(preferredIndex);
        if (paired >= 0 && paired < 4 && quad.getSlabAt(paired) != null) return paired;

        paired = bitsandbalance$pairedVerticalIndex(bitsandbalance$pairedHorizontalIndex(preferredIndex));
        if (paired >= 0 && paired < 4 && quad.getSlabAt(paired) != null) return paired;

        for (int i = 0; i < 4; i++) {
            if (quad.getSlabAt(i) != null) return i;
        }
        return -1;
    }

    private static int bitsandbalance$resolveOccupiedIndex(@Nullable BlockState[] slots, int preferredIndex) {
        if (slots == null) return -1;
        if (preferredIndex >= 0 && preferredIndex < 4 && bitsandbalance$slot(slots, preferredIndex) != null) return preferredIndex;

        int paired = bitsandbalance$pairedVerticalIndex(preferredIndex);
        if (paired >= 0 && paired < 4 && bitsandbalance$slot(slots, paired) != null) return paired;

        paired = bitsandbalance$pairedHorizontalIndex(preferredIndex);
        if (paired >= 0 && paired < 4 && bitsandbalance$slot(slots, paired) != null) return paired;

        paired = bitsandbalance$pairedVerticalIndex(bitsandbalance$pairedHorizontalIndex(preferredIndex));
        if (paired >= 0 && paired < 4 && bitsandbalance$slot(slots, paired) != null) return paired;

        for (int i = 0; i < 4; i++) {
            if (bitsandbalance$slot(slots, i) != null) return i;
        }
        return -1;
    }

    private static @Nullable BlockState[] bitsandbalance$slots(BlockGetter level, BlockPos pos, @Nullable QuadStepBlockEntity quad) {
        if (quad != null) {
            return quad.getAllSlabs();
        }
        EnhancedSlabPlacementPredictionCache.QuadStepPrediction prediction = EnhancedSlabPlacementPredictionCache.peekQuadStep(level, pos);
        if (prediction != null) {
            return prediction.slabs();
        }
        return level instanceof Level actualLevel ? EnhancedSlabMaterialSavedData.get(actualLevel, pos.asLong()) : null;
    }

    private static @Nullable BlockState bitsandbalance$slot(@Nullable BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static boolean bitsandbalance$requiresTargetedBreaking(@Nullable BlockState[] slots) {
        BlockState first = null;
        for (int i = 0; i < 4; i++) {
            BlockState slot = bitsandbalance$slot(slots, i);
            if (slot == null) continue;
            if (first == null) {
                first = slot;
            } else if (!first.equals(slot)) {
                return true;
            }
        }
        return false;
    }

    // ── Block-state definition ─────────────────────────────────────────────---

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, AXIS);
    }

    // ── Shapes ────────────────────────────────────────────────────────────────

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        BlockEntity be = level.getBlockEntity(pos);
        QuadStepBlockEntity quad = be instanceof QuadStepBlockEntity q ? q : null;
        BlockState[] slots = bitsandbalance$slots(level, pos, quad);
        if (slots != null) {
            Direction.Axis axis = state.getValue(AXIS);
            if (ctx instanceof EntityCollisionContext ectx
                    && ectx.getEntity() instanceof Player player
                    && (player.isShiftKeyDown() || bitsandbalance$requiresTargetedBreaking(slots))) {
                int idx = bitsandbalance$resolveOccupiedIndex(slots, bitsandbalance$getTargetedIndexFromRay(player, pos, axis, slots));
                if (idx >= 0) {
                    BlockState selected = bitsandbalance$slot(slots, idx);
                    if (selected != null) {
                        return player.isShiftKeyDown()
                                ? shapeForIndex(axis, idx)
                                : bitsandbalance$matchingMaterialUnion(axis, slots, selected);
                    }
                }
            }
            // Union all occupied quadrants for general outline
            return occupiedUnion(axis, slots);
        }
        // Never fall back to a full-cube hitbox if the BE isn't available yet.
        BlockState[] contraptionSlots = EnhancedSlabCreateCompat.getContraptionSlots(level, pos, state);
        return contraptionSlots != null ? occupiedUnion(state.getValue(AXIS), contraptionSlots) : Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        BlockEntity be = level.getBlockEntity(pos);
        QuadStepBlockEntity quad = be instanceof QuadStepBlockEntity q ? q : null;
        BlockState[] slots = bitsandbalance$slots(level, pos, quad);
        if (slots != null) {
            return occupiedUnion(state.getValue(AXIS), slots);
        }
        // Never fall back to a full-cube collision box if the BE isn't available yet.
        BlockState[] contraptionSlots = EnhancedSlabCreateCompat.getContraptionSlots(level, pos, state);
        return contraptionSlots != null ? occupiedUnion(state.getValue(AXIS), contraptionSlots) : Shapes.empty();
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape shape = getCollisionShape(state, level, pos, CollisionContext.empty());
        return shape.isEmpty() ? Shapes.block() : shape;
    }

    private static VoxelShape occupiedUnion(Direction.Axis axis, QuadStepBlockEntity quad) {
        return occupiedUnion(axis, quad.getAllSlabs());
    }

    public static VoxelShape bitsandbalance$occupiedShape(Direction.Axis axis, @Nullable BlockState[] slots) {
        return slots == null ? Shapes.empty() : occupiedUnion(axis, slots);
    }

    private static VoxelShape occupiedUnion(Direction.Axis axis, BlockState[] slots) {
        QuadShapeKey key = new QuadShapeKey(axis,
                bitsandbalance$slot(slots, 0), bitsandbalance$slot(slots, 1), bitsandbalance$slot(slots, 2), bitsandbalance$slot(slots, 3));
        VoxelShape cached;
        synchronized (bitsandbalance$UNION_CACHE_LOCK) {
            cached = bitsandbalance$UNION_CACHE.get(key);
        }
        if (cached != null) {
            return cached;
        }

        VoxelShape result = Shapes.empty();
        for (int i = 0; i < 4; i++) {
            if (bitsandbalance$slot(slots, i) != null) {
                result = Shapes.or(result, shapeForIndex(axis, i));
            }
        }
        synchronized (bitsandbalance$UNION_CACHE_LOCK) {
            bitsandbalance$UNION_CACHE.put(key, result);
        }
        return result;
    }

    private static VoxelShape bitsandbalance$matchingMaterialUnion(Direction.Axis axis, BlockState[] slots, BlockState selected) {
        VoxelShape result = Shapes.empty();
        for (int i = 0; i < 4; i++) {
            BlockState slot = bitsandbalance$slot(slots, i);
            if (selected.equals(slot)) {
                result = Shapes.or(result, shapeForIndex(axis, i));
            }
        }
        return result;
    }

    private static <K, V> Map<K, V> bitsandbalance$createBoundedCache(int limit) {
        return new LinkedHashMap<K, V>(limit, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > limit;
            }
        };
    }

    private record QuadShapeKey(Direction.Axis axis,
                                @Nullable BlockState slot0,
                                @Nullable BlockState slot1,
                                @Nullable BlockState slot2,
                                @Nullable BlockState slot3) {
    }

    private static BlockState[] bitsandbalance$buildRemainingSlabs(BlockState[] slots, int targetedIndex, boolean singlePieceOnly) {
        BlockState[] remaining = slots.clone();
        BlockState targeted = bitsandbalance$slot(slots, targetedIndex);
        if (targeted == null) {
            return remaining;
        }
        if (singlePieceOnly) {
            remaining[targetedIndex] = null;
            return remaining;
        }
        for (int i = 0; i < remaining.length; i++) {
            if (targeted.equals(remaining[i])) {
                remaining[i] = null;
            }
        }
        return remaining;
    }

    private static int bitsandbalance$countRemoved(BlockState[] before, BlockState[] after) {
        int removed = 0;
        for (int i = 0; i < 4; i++) {
            if (bitsandbalance$slot(before, i) != null && bitsandbalance$slot(after, i) == null) {
                removed++;
            }
        }
        return removed;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack,
                                          BlockState state,
                                          Level level,
                                          BlockPos pos,
                                          Player player,
                                          InteractionHand hand,
                                          BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        QuadStepBlockEntity quad = blockEntity instanceof QuadStepBlockEntity q ? q : null;
        BlockState[] slots = bitsandbalance$slots(level, pos, quad);
        if (slots == null) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        int targetedIndex = bitsandbalance$resolveOccupiedIndex(
                slots,
                bitsandbalance$getTargetedIndex(pos, hitResult.getLocation(), hitResult.getDirection(), state.getValue(AXIS))
        );
        if (targetedIndex < 0) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        DynamicVariantMaterialHelper.CopperUseResult result = DynamicVariantMaterialHelper.getCopperUseResult(stack, bitsandbalance$slot(slots, targetedIndex));
        if (result == null) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        slots[targetedIndex] = result.slabState();
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), slots);
        if (quad != null) {
            quad.setSlabAt(targetedIndex, result.slabState());
        }
        bitsandbalance$finishCopperUse(level, pos, player, hand, stack, result);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        QuadStepBlockEntity quad = blockEntity instanceof QuadStepBlockEntity q ? q : null;
        BlockState[] slots = bitsandbalance$slots(level, pos, quad);
        if (slots == null) {
            return;
        }

        int[] eligibleIndices = new int[4];
        BlockState[] nextStates = new BlockState[4];
        int eligibleCount = 0;
        for (int i = 0; i < 4; i++) {
            BlockState next = DynamicVariantMaterialHelper.getCopperRandomTickNextState(bitsandbalance$slot(slots, i), level, pos, random);
            if (next == null) {
                continue;
            }
            eligibleIndices[eligibleCount++] = i;
            nextStates[i] = next;
        }
        if (eligibleCount <= 0) {
            return;
        }

        int chosenIndex = eligibleIndices[random.nextInt(eligibleCount)];
        slots[chosenIndex] = nextStates[chosenIndex];
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), slots);
        if (quad != null) {
            quad.setSlabAt(chosenIndex, nextStates[chosenIndex]);
        }
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        try {
            BlockEntity be = level.getBlockEntity(pos);
            QuadStepBlockEntity quad = be instanceof QuadStepBlockEntity q ? q : null;
            BlockState[] slots = bitsandbalance$slots(level, pos, quad);
            if (slots != null) {
                int idx = bitsandbalance$resolveOccupiedIndex(slots, bitsandbalance$getTargetedIndex(player, pos, state.getValue(AXIS)));
                if (idx < 0) return;
                BlockState slab = bitsandbalance$slot(slots, idx);
                if (slab != null) {
                    level.levelEvent(player, 2001, pos, Block.getId(slab));
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        super.spawnDestroyParticles(level, player, pos, state);
    }

    // ── Block entity ──────────────────────────────────────────────────────────

    // ── Waterlogging ──────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level,
                                     ScheduledTickAccess sched, BlockPos pos,
                                     Direction dir, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource rand) {
        if (state.getValue(WATERLOGGED)) {
            sched.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, sched, pos, dir, neighborPos, neighborState, rand);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    // ── Mining speed ──────────────────────────────────────────────────────────

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        QuadStepBlockEntity quad = be instanceof QuadStepBlockEntity q ? q : null;
        BlockState[] slots = bitsandbalance$slots(level, pos, quad);
        if (slots != null) {
            if (!player.isShiftKeyDown() && !bitsandbalance$requiresTargetedBreaking(slots)) {
                float slowest = Float.MAX_VALUE;
                boolean found = false;
                for (int i = 0; i < 4; i++) {
                    BlockState slab = bitsandbalance$slot(slots, i);
                    if (slab == null) continue;
                    float progress = bitsandbalance$computeDestroyProgress(slab, player, level, pos);
                    if (progress <= 0.0F) return progress;
                    slowest = Math.min(slowest, progress);
                    found = true;
                }
                return found ? slowest : super.getDestroyProgress(state, player, level, pos);
            }
            int idx = bitsandbalance$resolveOccupiedIndex(slots, bitsandbalance$getTargetedIndex(player, pos, state.getValue(AXIS)));
            if (idx < 0) return super.getDestroyProgress(state, player, level, pos);
            BlockState slab = bitsandbalance$slot(slots, idx);
            if (slab != null) {
                return bitsandbalance$computeDestroyProgress(slab, player, level, pos);
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    private static float bitsandbalance$computeDestroyProgress(BlockState ref, Player player,
                                                               BlockGetter level, BlockPos pos) {
        float hardness = ref.getDestroySpeed(level, pos);
        if (hardness == -1.0F) return 0.0F;
        boolean correctTool = !ref.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(ref);
        float speed = player.getDestroySpeed(ref);
        return correctTool ? speed / hardness / 30.0F : speed / hardness / 100.0F;
    }

    // ── Breaking / drops ──────────────────────────────────────────────────────

    /** Drops all occupied quadrants — used by explosions, pistons, etc. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof QuadStepBlockEntity quad) {
            List<ItemStack> drops = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                BlockState slab = quad.getSlabAt(i);
                if (slab != null) {
                    ItemStack drop = StepBlock.bitsandbalance$makeStepDropItem(slab);
                    if (!drop.isEmpty()) drops.add(drop);
                }
            }
            return drops;
        }
        return Collections.emptyList();
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            QuadStepBlockEntity quad = be instanceof QuadStepBlockEntity q ? q : null;
            BlockState[] slots = bitsandbalance$slots(level, pos, quad);
            if (slots != null) {
                if (player.isShiftKeyDown() || bitsandbalance$requiresTargetedBreaking(slots)) {
                    int idx = bitsandbalance$resolveOccupiedIndex(slots, bitsandbalance$getTargetedIndex(player, pos, state.getValue(AXIS)));
                    if (idx < 0) {
                        CACHED_BREAK.remove();
                        return super.playerWillDestroy(level, pos, state, player);
                    }
                    BlockState minedSlab = bitsandbalance$slot(slots, idx);
                    if (minedSlab != null) {
                        CachedBreak cached = new CachedBreak();
                        cached.posLong     = pos.asLong();
                        cached.targetedIndex  = idx;
                        cached.minedSlab   = minedSlab;
                        cached.waterlogged = state.getValue(WATERLOGGED);
                        cached.axis = state.getValue(AXIS);
                        BlockState[] remaining = bitsandbalance$buildRemainingSlabs(slots, idx, player.isShiftKeyDown());
                        cached.removedCount = Math.max(1, bitsandbalance$countRemoved(slots, remaining));
                        cached.remainingSlabs = remaining;
                        CACHED_BREAK.set(cached);
                    } else {
                        CACHED_BREAK.remove();
                    }
                } else {
                    // Uniform quads keep the old behavior: whole-block break unless crouching.
                    CACHED_BREAK.remove();
                }
            } else {
                CACHED_BREAK.remove();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    //? if mc == 26.3
    public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos,
    //? else
    public void playerDestroy(Level level, Player player, BlockPos pos,
    //? endif
                              BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (level.isClientSide()) return;

        CachedBreak cached = CACHED_BREAK.get();
        if (cached == null || cached.posLong != pos.asLong()) {
            BlockState[] slots = blockEntity instanceof QuadStepBlockEntity quad
                    ? quad.getAllSlabs()
                    : EnhancedSlabMaterialSavedData.get(level, pos.asLong());
            if (!player.isCreative() && slots != null) {
                for (int i = 0; i < 4; i++) {
                    BlockState slab = bitsandbalance$slot(slots, i);
                    if (slab == null) continue;
                    boolean correctTool = !slab.requiresCorrectToolForDrops()
                            || player.hasCorrectToolForDrops(slab);
                    if (!correctTool) continue;

                    ItemStack drop = StepBlock.bitsandbalance$makeStepDropItem(slab);
                    if (!drop.isEmpty()) Block.popResource(level, pos, drop);
                }
                player.awardStat(Stats.BLOCK_MINED.get(this));
                player.causeFoodExhaustion(0.005F);
            }
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
            return;
        }

        // Crouching case: drop only the targeted quadrant
        if (!player.isCreative() && cached.minedSlab != null) {
            boolean correctTool = !cached.minedSlab.requiresCorrectToolForDrops()
                    || player.hasCorrectToolForDrops(cached.minedSlab);
            if (correctTool) {
                for (int i = 0; i < Math.max(1, cached.removedCount); i++) {
                    ItemStack drop = StepBlock.bitsandbalance$makeStepDropItem(cached.minedSlab);
                    if (!drop.isEmpty()) Block.popResource(level, pos, drop);
                }
            }
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
        }

        // Last-chance: restore remaining steps if the mixin failed to do so
        if (level.getBlockState(pos).isAir()) {
            bitsandbalance$restoreRemaining(level, pos, cached);
        }

        CACHED_BREAK.remove();
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), cached.remainingSlabs);
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        // Runs for both survival and creative (before creative's early return skips playerDestroy).
        CachedBreak cached = CACHED_BREAK.get();
        if (cached != null && cached.posLong == pos.asLong()) {
            BlockState current = level.getBlockState(pos);
            if (current.isAir() || current.getFluidState().isSource()) {
                bitsandbalance$restoreRemaining(level, pos, cached);
            }
        }
        super.destroy(level, pos, state);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Restores remaining quadrant steps after the targeted quadrant has been mined.
     * Places a {@link QuadStepBlock} if ≥ 2 quadrants remain, or a single {@link FixedStepBlock}
     * (via {@link StepDynamicRegistry}) if exactly 1 remains.
     */
    public static void bitsandbalance$restoreRemaining(LevelAccessor level, BlockPos pos, CachedBreak cached) {
        if (cached.remainingSlabs == null) return;

        int count = 0;
        int lastIdx = -1;
        for (int i = 0; i < 4; i++) {
            if (cached.remainingSlabs[i] != null) {
                count++;
                lastIdx = i;
            }
        }

        if (count == 0) {
            // Nothing to restore — position stays as air/water
            return;
        }

        boolean waterlogged = cached.waterlogged;
        Direction.Axis axis = cached.axis != null ? cached.axis : Direction.Axis.X;

        if (count == 1 && lastIdx >= 0) {
            // Place a single FixedStepBlock for the remaining slab
            BlockState remainingSlab = bitsandbalance$toRenderSlabState(cached.remainingSlabs[lastIdx]);
            if (remainingSlab == null) return;

            Block sourceSlabBlock = remainingSlab.getBlock();
            if (sourceSlabBlock instanceof StepBlock) {
                Block mapped = StepDynamicRegistry.getSlabForStep(sourceSlabBlock);
                if (mapped != null) sourceSlabBlock = mapped;
            }
            Block mappedFromVertical = VerticalSlabDynamicRegistry.getSlabForVertical(sourceSlabBlock);
            if (mappedFromVertical != null) sourceSlabBlock = mappedFromVertical;
            Block sourceVertical = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(sourceSlabBlock);
            if (sourceVertical != null) {
                Block mapped = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVertical);
                if (mapped != null) sourceSlabBlock = mapped;
            }

            Block stepBlock = StepDynamicRegistry.getStepForSlab(sourceSlabBlock);
            if (stepBlock instanceof FixedStepBlock || stepBlock instanceof StepBlock) {
                BlockState newState = stepBlock.defaultBlockState()
                    .setValue(StepBlock.FACING, sideForIndex(axis, lastIdx))
                        .setValue(StepBlock.TOP,    isTop(lastIdx))
                        .setValue(StepBlock.WATERLOGGED, waterlogged);
                level.setBlock(pos, newState, Block.UPDATE_ALL);
                if (level instanceof Level actualLevel) {
                    EnhancedSlabMaterialSavedData.remember(actualLevel, pos.asLong(), remainingSlab);
                }
                if (stepBlock instanceof FixedStepBlock) {
                    return;
                }
                BlockEntity newBe = level.getBlockEntity(pos);
                if (!(newBe instanceof org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity)
                        && level instanceof Level actualLevel
                        && stepBlock instanceof EntityBlock entityBlock) {
                    BlockEntity created = entityBlock.newBlockEntity(pos, newState);
                    if (created != null) {
                        created.setLevel(actualLevel);
                        actualLevel.setBlockEntity(created);
                        newBe = created;
                    }
                }
                if (newBe instanceof org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity sbe) {
                    sbe.setSlabState(remainingSlab);
                }
            }
        } else if (count >= 2) {
            // Place a QuadStepBlock with remaining slots
            BlockState quadState = CommonBlocks.QUAD_STEP.defaultBlockState()
                    .setValue(WATERLOGGED, waterlogged && count < 4)
                    .setValue(AXIS, axis);
            level.setBlock(pos, quadState, Block.UPDATE_ALL);
            if (level instanceof Level actualLevel) {
                EnhancedSlabMaterialSavedData.remember(actualLevel, pos.asLong(), cached.remainingSlabs);
            }
            BlockEntity newBe = level.getBlockEntity(pos);
            if (newBe instanceof QuadStepBlockEntity quad) {
                quad.setAllSlabs(cached.remainingSlabs);
                if (level instanceof Level actualLevel) {
                    EnhancedSlabStorageOptimizer.optimizeAt(actualLevel, pos);
                }
            }
        }
    }

    private static void bitsandbalance$finishCopperUse(Level level,
                                                       BlockPos pos,
                                                       @Nullable Player player,
                                                       InteractionHand hand,
                                                       ItemStack stack,
                                                       DynamicVariantMaterialHelper.CopperUseResult result) {
        level.playSound(null, pos, result.sound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.levelEvent(null, result.levelEvent(), pos, 0);

        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            if (!player.getAbilities().instabuild) {
                if (result.consumeItem()) {
                    stack.shrink(1);
                }
                if (result.damageTool()) {
                    EquipmentSlot slot = hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                    stack.hurtAndBreak(1, player, slot);
                }
            }
        } else if (result.consumeItem()) {
            stack.shrink(1);
        }
    }

    private static @Nullable BlockState bitsandbalance$toRenderSlabState(@Nullable BlockState state) {
        if (state == null) return null;

        BlockState renderState = state;
        if (!(renderState.getBlock() instanceof SlabBlock)) {
            Block slabBlock = null;
            Block block = renderState.getBlock();

            if (block instanceof FixedStepBlock fixedStep) {
                slabBlock = fixedStep.getSourceSlab();
            } else {
                slabBlock = StepDynamicRegistry.getSlabForStep(block);
                if (slabBlock == null) {
                    Block sourceVertical = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(block);
                    if (sourceVertical != null) {
                        slabBlock = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVertical);
                    }
                }
                if (slabBlock == null) {
                    slabBlock = VerticalSlabDynamicRegistry.getSlabForVertical(block);
                }
            }

            if (slabBlock == null) return null;
            renderState = bitsandbalance$copySharedProperties(renderState, slabBlock.defaultBlockState());
        }

        if (renderState.hasProperty(SlabBlock.TYPE)) {
            renderState = renderState.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (renderState.hasProperty(SlabBlock.WATERLOGGED)) {
            renderState = renderState.setValue(SlabBlock.WATERLOGGED, false);
        }
        return renderState;
    }

    private static BlockState bitsandbalance$copySharedProperties(BlockState from, BlockState to) {
        if (from == null || to == null) return to;
        for (Property<?> property : from.getProperties()) {
            if (property != null && to.hasProperty(property)) {
                to = bitsandbalance$copySharedProperty(from, to, property);
            }
        }
        return to;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState bitsandbalance$copySharedProperty(BlockState from, BlockState to, Property property) {
        try {
            return to.setValue(property, from.getValue(property));
        } catch (IllegalArgumentException ignored) {
            return to;
        }
    }

    // ── Cached break state ────────────────────────────────────────────────────

    public static final class CachedBreak {
        public long posLong;
        public int targetedIndex;
        public @Nullable BlockState minedSlab;
        public BlockState @Nullable [] remainingSlabs;
        public boolean waterlogged;
        public @Nullable Direction.Axis axis;
        public int removedCount;
    }

    // ── Placement helpers (called by placement mixin) ─────────────────────────

    /**
     * Determines whether a step placement on the given existing block state should create or
     * extend a {@link QuadStepBlock}.
     *
     * @param existing     the block state currently at the target position
     * @param placingBlock the step block the player is placing
     * @param targetIndex  the quadrant index the player is targeting
     * @return {@code true} if the placement should merge into a QuadStepBlock
     */
    public static boolean canMergeIntoQuad(BlockState existing, Block placingBlock, int targetIndex) {
        if (!(placingBlock instanceof StepBlock)) return false;

        if (existing.getBlock() instanceof StepBlock) {
            // Single step → check the targeted quadrant is empty
            Direction existingFacing = existing.getValue(StepBlock.FACING);
            boolean existingTop = existing.getValue(StepBlock.TOP);
            int existingIndex = toIndex(StepBlock.bitsandbalance$isPositiveFacing(existingFacing), existingTop);
            return existingIndex != targetIndex;
        }

        if (existing.getBlock() instanceof QuadStepBlock) {
            // Already a quad → check the targeted quadrant is empty
            return true; // mixin will check BE slot
        }

        return false;
    }
}
