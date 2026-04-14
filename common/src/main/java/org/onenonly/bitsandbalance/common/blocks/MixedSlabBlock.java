package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior — Mixed Double Slabs
 *
 * A compound block that stores two different slab types (bottom + top) in a single
 * block position via a {@link MixedSlabBlockEntity}. This allows, for example, an
 * oak slab on the bottom half and a stone slab on the top half.
 *
 * <p>Vanilla double slabs require both halves to be the same block; this block
 * lifts that restriction.</p>
 *
 * <h3>KneeSlab-style mining</h3>
 * In survival mode, only the half the player is looking at is mined. The remaining
 * half converts back to a vanilla single slab. In creative mode the whole block is
 * removed instantly with no drops.
 *
 * <h3>Drops</h3>
 * <ul>
 *   <li>Player survival break (half-break) — drops the targeted half's slab</li>
 *   <li>Explosion / non-player removal — drops <em>both</em> slabs via
 *       {@link #getDrops}</li>
 * </ul>
 */
public class MixedSlabBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final ThreadLocal<CachedBreak> CACHED_BREAK = new ThreadLocal<>();

    private static final VoxelShape BOTTOM_HALF_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private static final VoxelShape TOP_HALF_SHAPE = Block.box(0.0, 8.0, 0.0, 16.0, 16.0, 16.0);

    public MixedSlabBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false));
    }

    public static @Nullable BlockState bitsandbalance$peekCachedMinedSlab(BlockPos pos) {
        CachedBreak cached = CACHED_BREAK.get();
        if (cached == null || cached.posLong != pos.asLong()) return null;
        return cached.minedSlab;
    }

    public static @Nullable BlockState bitsandbalance$peekCachedRemainingSlab(BlockPos pos) {
        CachedBreak cached = CACHED_BREAK.get();
        if (cached == null || cached.posLong != pos.asLong()) return null;
        return cached.remainingSlab;
    }

    public static void bitsandbalance$clearCachedBreak() {
        CACHED_BREAK.remove();
    }

    public static SlabType bitsandbalance$getTargetedHalf(Player player, BlockPos pos) {
        return getTargetedHalfFromRayToFullCube(player, pos);
    }

    // ── Block-state definition ──────────────────────────────────────────────

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    // ── Shapes ──────────────────────────────────────────────────────────────

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Selection outline shape: show either the top or bottom half based on where the
        // player is aiming, so each slab half outlines separately.
        if (context instanceof EntityCollisionContext entityContext) {
            if (entityContext.getEntity() instanceof Player player) {
                SlabType half = getTargetedHalfFromRayToFullCube(player, pos);
                return half == SlabType.TOP ? TOP_HALF_SHAPE : BOTTOM_HALF_SHAPE;
            }
        }
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    // ── Rendering ───────────────────────────────────────────────────────────

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        // Vanilla calls this from Block#playerWillDestroy() and uses the block's own
        // ID for particle/sound effects. Since this block is render-invisible, we
        // emit particles/sounds for the slab half the player is actually breaking.
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MixedSlabBlockEntity mixedBe) {
                SlabType targetedHalf = getTargetedHalfFromRayToFullCube(player, pos);
                BlockState halfState = (targetedHalf == SlabType.TOP) ? mixedBe.getTopSlab() : mixedBe.getBottomSlab();
                if (halfState != null) {
                    level.levelEvent(player, 2001, pos, Block.getId(halfState));
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        super.spawnDestroyParticles(level, player, pos, state);
    }

    // ── Block entity ────────────────────────────────────────────────────────

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MixedSlabBlockEntity(pos, state);
    }

    // ── Waterlogging ────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level,
                                     ScheduledTickAccess scheduledTickAccess, BlockPos pos,
                                     Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    // ── Pick block (middle-click) ───────────────────────────────────────────

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MixedSlabBlockEntity mixedBe) {
            // Return whichever slab the player is "looking at".
            // Since no player context is available here, return the bottom slab.
            BlockState bottom = mixedBe.getBottomSlab();
            if (bottom != null) {
                return bitsandbalance$makeHalfDropItem(bottom);
            }
        }
        return ItemStack.EMPTY;
    }

    // ── Mining speed (per-half) ─────────────────────────────────────────────

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MixedSlabBlockEntity mixedBe)) {
            return super.getDestroyProgress(state, player, level, pos);
        }

        // Determine which half the player is looking at.
        // Avoid relying on player.pick() here because it can be sensitive to timing/air state;
        // instead, ray-clip directly against this block's shape.
        SlabType targetedHalf = getTargetedHalfFromRay(player, state, level, pos);
        BlockState halfState = (targetedHalf == SlabType.TOP)
                ? mixedBe.getTopSlab() : mixedBe.getBottomSlab();
        if (halfState == null) {
            return super.getDestroyProgress(state, player, level, pos);
        }

        float hardness = halfState.getDestroySpeed(level, pos);
        if (hardness == -1.0F) return 0.0F;

        boolean correctTool = !halfState.requiresCorrectToolForDrops()
                || player.hasCorrectToolForDrops(halfState);
        float speedFactor = player.getDestroySpeed(halfState);

        return correctTool
                ? speedFactor / hardness / 30.0F
                : speedFactor / hardness / 100.0F;
    }

    // ── Breaking / drops ────────────────────────────────────────────────────

    /**
     * Returns both slab halves as drops. Used by explosions, pistons, and any
     * non-player block destruction. For player survival breaks, {@link #playerDestroy}
     * handles per-half drops directly.
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof MixedSlabBlockEntity mixedBe) {
            List<ItemStack> drops = new ArrayList<>(2);
            BlockState bottom = mixedBe.getBottomSlab();
            BlockState top = mixedBe.getTopSlab();
            if (bottom != null) drops.add(bitsandbalance$makeHalfDropItem(bottom));
            if (top != null) drops.add(bitsandbalance$makeHalfDropItem(top));
            return drops;
        }
        return Collections.emptyList();
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos,
                              BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (level.isClientSide()) return;

        CachedBreak cached = CACHED_BREAK.get();
        if (cached == null || cached.posLong != pos.asLong()) {
            return; // No cached half info; do not call super.
        }

        // In 1.21.11, creative mode exits early in destroyBlock() before calling playerDestroy.
        // So playerDestroy should only handle drops/stats in survival; placement of the remaining
        // half is handled in destroy(...), which runs for both survival and creative.

        if (!player.isCreative() && cached.minedSlab != null) {
            boolean correctTool = !cached.minedSlab.requiresCorrectToolForDrops()
                    || player.hasCorrectToolForDrops(cached.minedSlab);
            if (correctTool) {
                Block.dropResources(cached.minedSlab, level, pos, null, player, tool);
            }

            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
        }

        // Last-chance: if something left the position empty, restore remaining half.
        if (cached.remainingSlab != null && level.getBlockState(pos).isAir()) {
            level.setBlock(pos, cached.remainingSlab, Block.UPDATE_ALL);
        }

        CACHED_BREAK.remove();
        return; // Do NOT call super — we handle all drops ourselves.
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MixedSlabBlockEntity mixedBe) {
                CachedBreak cached = new CachedBreak();
                cached.posLong = pos.asLong();
                cached.bottomSlab = mixedBe.getBottomSlab();
                cached.topSlab = mixedBe.getTopSlab();

                cached.targetedHalf = getTargetedHalfFromRayToFullCube(player, pos);

                cached.minedSlab = (cached.targetedHalf == SlabType.TOP) ? cached.topSlab : cached.bottomSlab;
                BlockState remaining = (cached.targetedHalf == SlabType.TOP) ? cached.bottomSlab : cached.topSlab;
                if (remaining != null && state.getValue(WATERLOGGED) && remaining.hasProperty(SlabBlock.WATERLOGGED)) {
                    remaining = remaining.setValue(SlabBlock.WATERLOGGED, true);
                }
                cached.remainingSlab = remaining;

                CACHED_BREAK.set(cached);
            } else {
                CACHED_BREAK.remove();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        // This is invoked immediately after the block is removed from the world.
        // Crucially, it runs even in creative mode before the early return that
        // skips playerDestroy(), so it is the correct place to re-place the
        // remaining half.
        CachedBreak cached = CACHED_BREAK.get();
        if (cached != null && cached.posLong == pos.asLong() && cached.remainingSlab != null) {
            // Only restore if the position is still empty (don't stomp other updates).
            if (level.getBlockState(pos).isAir() || level.getBlockState(pos).is(Blocks.WATER)) {
                level.setBlock(pos, cached.remainingSlab, Block.UPDATE_ALL);
            }
        }
        super.destroy(level, pos, state);
    }

    private static SlabType getTargetedHalfFromRay(Player player, BlockState state, BlockGetter level, BlockPos pos) {
        // Raycast from the player's eye along their view vector, then clip to this block's shape.
        // This is more reliable than Entity#pick() during destruction because it doesn't depend on
        // the world still containing the block at that position.
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));

        VoxelShape shape = state.getShape(level, pos, CollisionContext.of(player));
        BlockHitResult clipped = shape.clip(start, end, pos);
        if (clipped != null) {
            return EnhancedSlabHelper.getTargetedHalf(pos, clipped.getLocation());
        }

        // Fallback: use eye height relative to the block.
        double relativeY = player.getEyeY() - pos.getY();
        return relativeY > 0.5 ? SlabType.TOP : SlabType.BOTTOM;
    }

    private static SlabType getTargetedHalfFromRayToFullCube(Player player, BlockPos pos) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));

        BlockHitResult clipped = Shapes.block().clip(start, end, pos);
        if (clipped != null) {
            return EnhancedSlabHelper.getTargetedHalf(pos, clipped.getLocation());
        }

        double relativeY = player.getEyeY() - pos.getY();
        return relativeY > 0.5 ? SlabType.TOP : SlabType.BOTTOM;
    }

    private static final class CachedBreak {
        long posLong;
        @Nullable BlockState bottomSlab;
        @Nullable BlockState topSlab;
        @Nullable SlabType targetedHalf;
        @Nullable BlockState minedSlab;
        @Nullable BlockState remainingSlab;
    }

    // ── Placement helpers (called by placement mixin) ───────────────────────

    /**
     * Checks whether a slab placement on the given existing slab state qualifies for
     * creating a mixed double slab.
     *
     * @param existingState  the slab state already at the target position
     * @param placingBlock   the slab block the player is placing
     * @param clickedFace    the face the player clicked
     * @param hitY           unused; retained for call-site compatibility
     * @return {@code true} if the placement should create a mixed slab
     */
    public static boolean canCreateMixedSlab(BlockState existingState, Block placingBlock,
                                             Direction clickedFace, double hitY) {
        if (!(existingState.getBlock() instanceof SlabBlock)) return false;
        if (!(placingBlock instanceof SlabBlock)) return false;
        if (existingState.getBlock() == placingBlock) return false; // Same type → vanilla handles it

        SlabType existingType = existingState.getValue(SlabBlock.TYPE);
        if (existingType == SlabType.DOUBLE) return false; // Already double

        if (existingType == SlabType.BOTTOM) {
            // Only merge when the top face is directly clicked — not from a side face.
            return clickedFace == Direction.UP;
        } else { // TOP
            // Only merge when the bottom face is directly clicked — not from a side face.
            return clickedFace == Direction.DOWN;
        }
    }

    /**
     * Builds the {@link BlockState}s for the two slab halves when creating a mixed slab.
     *
     * @param existingState the existing single slab at the target position
     * @param placingBlock  the slab block being placed
     * @return an array {@code [bottomSlab, topSlab]} of single-slab block states
     */
    /**
     * Convenience overload – uses the block's default state for the placed half.
     * Prefer {@link #buildMixedSlabStates(BlockState, BlockState)} when you already have a
     * full placing state (e.g. with the correct DyeColor already applied).
     */
    public static BlockState[] buildMixedSlabStates(BlockState existingState, Block placingBlock) {
        return buildMixedSlabStates(existingState, placingBlock.defaultBlockState());
    }

    /**
     * Builds the {@link BlockState}s for the two slab halves when creating a mixed slab.
     *
     * @param existingState the existing single slab at the target position
     * @param placingSlabState the slab state being placed
     * @return an array {@code [bottomSlab, topSlab]} of single-slab block states
     */
    public static BlockState[] buildMixedSlabStates(BlockState existingState, BlockState placingSlabState) {
        SlabType existingType = existingState.getValue(SlabBlock.TYPE);

        BlockState bottomSlab;
        BlockState topSlab;

        if (existingType == SlabType.BOTTOM) {
            bottomSlab = existingState; // keep existing bottom
            topSlab = placingSlabState.hasProperty(SlabBlock.TYPE)
                    ? placingSlabState.setValue(SlabBlock.TYPE, SlabType.TOP)
                    : placingSlabState;
        } else {
            // existing is TOP
            topSlab = existingState; // keep existing top
            bottomSlab = placingSlabState.hasProperty(SlabBlock.TYPE)
                    ? placingSlabState.setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                    : placingSlabState;
        }

        // Strip waterlogged from individual slab states (waterlogging is on the MixedSlabBlock)
        if (bottomSlab.hasProperty(SlabBlock.WATERLOGGED)) {
            bottomSlab = bottomSlab.setValue(SlabBlock.WATERLOGGED, false);
        }
        if (topSlab.hasProperty(SlabBlock.WATERLOGGED)) {
            topSlab = topSlab.setValue(SlabBlock.WATERLOGGED, false);
        }

        return new BlockState[]{ bottomSlab, topSlab };
    }

    /** Creates a drop {@link ItemStack} for one half of a mixed slab. */
    private static ItemStack bitsandbalance$makeHalfDropItem(BlockState halfState) {
        return new ItemStack(halfState.getBlock().asItem());
    }
}
