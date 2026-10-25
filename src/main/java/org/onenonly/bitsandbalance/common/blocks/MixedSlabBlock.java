package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if mc == 26.3
import net.minecraft.world.entity.Entity;
//? endif
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.mechanics.MixedSlabMaterialLookup;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabStorageOptimizer;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior — Mixed Double Slabs
 *
 * A compound block that stores two different slab types (bottom + top) in a single
 * block position in the enhanced slab material store. This allows, for example, an
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
public class MixedSlabBlock extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final TagKey<Block> NO_MIXED_SLABS =
            TagKey.create(Registries.BLOCK, Identifier.parse("bitsandbalance:no_mixed_slabs"));

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

    private static @Nullable BlockState[] bitsandbalance$storedSlabs(BlockGetter level, BlockPos pos) {
        return MixedSlabMaterialLookup.get(level, pos);
    }

    private static @Nullable BlockState bitsandbalance$bottom(@Nullable BlockState[] slabs) {
        return slabs != null && slabs.length > 0 ? slabs[0] : null;
    }

    private static @Nullable BlockState bitsandbalance$top(@Nullable BlockState[] slabs) {
        return slabs != null && slabs.length > 1 ? slabs[1] : null;
    }

    private static VoxelShape bitsandbalance$standardHalfShape(SlabType half) {
        return half == SlabType.TOP ? TOP_HALF_SHAPE : BOTTOM_HALF_SHAPE;
    }

    private static BlockState bitsandbalance$slabStateForHalf(BlockState slabState, SlabType half) {
        if (slabState.hasProperty(SlabBlock.TYPE)) {
            return slabState.setValue(SlabBlock.TYPE, half);
        }
        return slabState;
    }

    private static VoxelShape bitsandbalance$halfOutlineShape(@Nullable BlockState slabState,
                                                              SlabType half,
                                                              BlockGetter level,
                                                              BlockPos pos,
                                                              CollisionContext context) {
        if (slabState == null) {
            return Shapes.empty();
        }

        BlockState halfState = bitsandbalance$slabStateForHalf(slabState, half);
        try {
            VoxelShape shape = halfState.getShape(level, pos, context);
            if (!shape.isEmpty()) {
                return shape;
            }
        } catch (Throwable ignored) {
        }
        return bitsandbalance$standardHalfShape(half);
    }

    private static VoxelShape bitsandbalance$halfCollisionShape(@Nullable BlockState slabState,
                                                                SlabType half,
                                                                BlockGetter level,
                                                                BlockPos pos,
                                                                CollisionContext context) {
        if (slabState == null) {
            return Shapes.empty();
        }

        BlockState halfState = bitsandbalance$slabStateForHalf(slabState, half);
        try {
            VoxelShape shape = halfState.getCollisionShape(level, pos, context);
            if (!shape.isEmpty()) {
                return shape;
            }
        } catch (Throwable ignored) {
        }
        return bitsandbalance$halfOutlineShape(slabState, half, level, pos, context);
    }

    // ── Block-state definition ──────────────────────────────────────────────

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    // ── Shapes ──────────────────────────────────────────────────────────────

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState[] slabs = bitsandbalance$storedSlabs(level, pos);
        BlockState bottom = bitsandbalance$bottom(slabs);
        BlockState top = bitsandbalance$top(slabs);
        if (bottom == null && top == null) {
            return Shapes.block();
        }
        if (bottom == null) {
            return bitsandbalance$halfOutlineShape(top, SlabType.TOP, level, pos, context);
        }
        if (top == null) {
            return bitsandbalance$halfOutlineShape(bottom, SlabType.BOTTOM, level, pos, context);
        }

        // Selection outline shape: show either the top or bottom half based on where the
        // player is aiming, so each slab half outlines separately.
        if (context instanceof EntityCollisionContext entityContext) {
            if (entityContext.getEntity() instanceof Player player) {
                SlabType half = getTargetedHalfFromRayToFullCube(player, pos);
                return half == SlabType.TOP
                        ? bitsandbalance$halfOutlineShape(top, SlabType.TOP, level, pos, context)
                        : bitsandbalance$halfOutlineShape(bottom, SlabType.BOTTOM, level, pos, context);
            }
        }
        return Shapes.or(
                bitsandbalance$halfOutlineShape(bottom, SlabType.BOTTOM, level, pos, context),
                bitsandbalance$halfOutlineShape(top, SlabType.TOP, level, pos, context)
        );
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState[] slabs = bitsandbalance$storedSlabs(level, pos);
        BlockState bottom = bitsandbalance$bottom(slabs);
        BlockState top = bitsandbalance$top(slabs);
        if (bottom == null && top == null) {
            return Shapes.block();
        }

        VoxelShape shape = Shapes.empty();
        if (bottom != null) {
            shape = Shapes.or(shape, bitsandbalance$halfCollisionShape(bottom, SlabType.BOTTOM, level, pos, context));
        }
        if (top != null) {
            shape = Shapes.or(shape, bitsandbalance$halfCollisionShape(top, SlabType.TOP, level, pos, context));
        }
        return shape.isEmpty() ? Shapes.block() : shape;
    }

    // ── Rendering ───────────────────────────────────────────────────────────

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack,
                                          BlockState state,
                                          Level level,
                                          BlockPos pos,
                                          Player player,
                                          InteractionHand hand,
                                          BlockHitResult hitResult) {
        BlockState[] slabs = bitsandbalance$storedSlabs(level, pos);
        if (slabs == null) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        BlockState bottomSlab = bitsandbalance$bottom(slabs);
        BlockState topSlab = bitsandbalance$top(slabs);
        SlabType targetedHalf = EnhancedSlabHelper.getTargetedHalf(pos, hitResult.getLocation());
        BlockState targetSlab = targetedHalf == SlabType.TOP ? topSlab : bottomSlab;
        DynamicVariantMaterialHelper.CopperUseResult result = DynamicVariantMaterialHelper.getCopperUseResult(stack, targetSlab);
        if (result == null) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (targetedHalf == SlabType.TOP) {
            topSlab = result.slabState();
        } else {
            bottomSlab = result.slabState();
        }

        bitsandbalance$setStoredSlabs(level, pos, state, bottomSlab, topSlab);
        bitsandbalance$finishCopperUse(level, pos, player, hand, stack, result);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState[] slabs = bitsandbalance$storedSlabs(level, pos);
        if (slabs == null) {
            return;
        }

        BlockState bottomSlab = bitsandbalance$bottom(slabs);
        BlockState topSlab = bitsandbalance$top(slabs);
        BlockState nextBottom = DynamicVariantMaterialHelper.getCopperRandomTickNextState(bottomSlab, level, pos, random);
        BlockState nextTop = DynamicVariantMaterialHelper.getCopperRandomTickNextState(topSlab, level, pos, random);

        if (nextBottom == null && nextTop == null) {
            return;
        }

        if (nextBottom != null && nextTop != null) {
            if (random.nextBoolean()) {
                nextBottom = null;
            } else {
                nextTop = null;
            }
        }

        bitsandbalance$setStoredSlabs(
                level,
                pos,
                state,
                nextBottom != null ? nextBottom : bottomSlab,
                nextTop != null ? nextTop : topSlab
        );
    }

    @Override
    //? if mc == 26.3
    public void spawnDestroyByEntityParticles(Level level, Entity entity, BlockPos pos, BlockState state) {
        Player player = entity instanceof Player breakingPlayer ? breakingPlayer : null;
    //? else
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
    //? endif
        // Vanilla calls this from Block#playerWillDestroy() and uses the block's own
        // ID for particle/sound effects. Since this block is render-invisible, we
        // emit particles/sounds for the slab half the player is actually breaking.
        try {
            //? if mc == 26.3
            BlockState mined = bitsandbalance$peekCachedMinedSlab(pos);
            if (mined != null) {
                level.levelEvent(entity, 2001, pos, Block.getId(mined));
                return;
            }
            //? endif
            BlockState[] slabs = bitsandbalance$storedSlabs(level, pos);
            if (slabs != null) {
                SlabType targetedHalf = getTargetedHalfFromRayToFullCube(player, pos);
                BlockState halfState = (targetedHalf == SlabType.TOP) ? bitsandbalance$top(slabs) : bitsandbalance$bottom(slabs);
                if (halfState != null) {
                    //? if mc == 26.3
                    level.levelEvent(entity, 2001, pos, Block.getId(halfState));
                    //? else
                    level.levelEvent(player, 2001, pos, Block.getId(halfState));
                    //? endif
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        //? if mc == 26.3
        super.spawnDestroyByEntityParticles(level, entity, pos, state);
        //? else
        super.spawnDestroyParticles(level, player, pos, state);
        //? endif
    }

    // ── Block entity ────────────────────────────────────────────────────────

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
        if (level instanceof Level actualLevel) {
            BlockState[] slabs = bitsandbalance$storedSlabs(actualLevel, pos);
            // Return whichever slab the player is "looking at".
            // Since no player context is available here, return the bottom slab.
            BlockState bottom = bitsandbalance$bottom(slabs);
            if (bottom != null) {
                return bitsandbalance$makeHalfDropItem(bottom);
            }
        }
        return ItemStack.EMPTY;
    }

    // ── Mining speed (per-half) ─────────────────────────────────────────────

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockState[] slabs = bitsandbalance$storedSlabs(level, pos);
        if (slabs == null) {
            return super.getDestroyProgress(state, player, level, pos);
        }

        // Determine which half the player is looking at.
        // Avoid relying on player.pick() here because it can be sensitive to timing/air state;
        // instead, ray-clip directly against this block's shape.
        SlabType targetedHalf = getTargetedHalfFromRay(player, state, level, pos);
        BlockState halfState = (targetedHalf == SlabType.TOP)
                ? bitsandbalance$top(slabs) : bitsandbalance$bottom(slabs);
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
        return Collections.emptyList();
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
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
        return; // Do NOT call super — we handle all drops ourselves.
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockState[] slabs = bitsandbalance$storedSlabs(level, pos);
            if (slabs != null) {
                CachedBreak cached = new CachedBreak();
                cached.posLong = pos.asLong();
                cached.bottomSlab = bitsandbalance$bottom(slabs);
                cached.topSlab = bitsandbalance$top(slabs);

                cached.targetedHalf = getTargetedHalfFromRayToFullCube(player, pos);

                cached.minedSlab = (cached.targetedHalf == SlabType.TOP) ? cached.topSlab : cached.bottomSlab;
                BlockState remaining = (cached.targetedHalf == SlabType.TOP) ? cached.bottomSlab : cached.topSlab;
                if (remaining != null) {
                    SlabType remainingHalf = cached.targetedHalf == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP;
                    remaining = bitsandbalance$singleSlabHalf(remaining, remainingHalf, state.getValue(WATERLOGGED));
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
        if (level instanceof Level actualLevel) {
            EnhancedSlabMaterialSavedData.remember(actualLevel, pos.asLong());
        }
        super.destroy(level, pos, state);
    }

    private static SlabType getTargetedHalfFromRay(Player player, BlockState state, BlockGetter level, BlockPos pos) {
        return getTargetedHalfFromRayToFullCube(player, pos);
    }

    private static SlabType getTargetedHalfFromRayToFullCube(Player player, BlockPos pos) {
        return EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);
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
        if (existingState.getBlock().builtInRegistryHolder().is(NO_MIXED_SLABS)) return false;
        if (placingBlock.builtInRegistryHolder().is(NO_MIXED_SLABS)) return false;

        SlabType existingType = existingState.getValue(SlabBlock.TYPE);
        if (existingType == SlabType.DOUBLE) return false; // Already double

        if (existingType == SlabType.BOTTOM) {
            // Side-face clicks merge when the hit is in the empty upper half.
            return clickedFace == Direction.UP
                    || (clickedFace.getAxis().isHorizontal() && hitY >= 0.5D);
        } else { // TOP
            // Side-face clicks merge when the hit is in the empty lower half.
            return clickedFace == Direction.DOWN
                    || (clickedFace.getAxis().isHorizontal() && hitY <= 0.5D);
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

    private static BlockState bitsandbalance$singleSlabHalf(BlockState slab, SlabType half, boolean waterlogged) {
        BlockState result = slab;
        if (result.hasProperty(SlabBlock.TYPE)) {
            result = result.setValue(SlabBlock.TYPE, half);
        }
        if (result.hasProperty(SlabBlock.WATERLOGGED)) {
            result = result.setValue(SlabBlock.WATERLOGGED, waterlogged);
        }
        return result;
    }

    private static void bitsandbalance$setStoredSlabs(Level level,
                                                      BlockPos pos,
                                                      BlockState ownerState,
                                                      @Nullable BlockState bottomSlab,
                                                      @Nullable BlockState topSlab) {
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), bottomSlab, topSlab);
        level.sendBlockUpdated(pos, ownerState, ownerState, Block.UPDATE_ALL);
        EnhancedSlabStorageOptimizer.optimizeAt(level, pos);
        org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabFluidGeometryRefresh.afterStoredShapeChange(
                level, pos, ownerState);
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
}
