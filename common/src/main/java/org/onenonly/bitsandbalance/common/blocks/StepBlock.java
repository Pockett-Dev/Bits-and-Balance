package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.entity.ItemDropHomingHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabParticleStateCache;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import net.minecraft.world.item.DyeColor;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Tweaks: Enhanced Slab Behavior — Steps
 *
 * A ¼-block step occupying one of four quadrants in a block position.
 * Geometry: 8 wide (X) × 8 tall (Y) × 16 deep (Z).
 *
 * Single step blocks are always {@link FixedStepBlock} instances keyed to a source slab.
 * When 2–4 steps share a position, a {@link QuadStepBlock} container takes over.
 */
public class StepBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {

    private static final double bitsandbalance$EDGE_ZONE = 0.25;
    private static final String bitsandbalance$STEP_DROP_HOME_UUID_PREFIX = "bitsandbalance:step_drop_home_uuid:";
    private static final String bitsandbalance$STEP_DROP_HOME_EXP_PREFIX = "bitsandbalance:step_drop_home_exp:";
    private static final long bitsandbalance$STEP_DROP_HOME_DURATION_TICKS = 5L;
    private static Field bitsandbalance$entityNoPhysicsField;
    private static boolean bitsandbalance$entityNoPhysicsFieldResolved;

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** Which horizontal half (E/W/N/S). */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Which vertical half — true = top (Y 8-16), false = bottom (Y 0-8). */
    public static final BooleanProperty TOP = BooleanProperty.create("top");

    private static final ThreadLocal<CachedBreak> CACHED_BREAK = new ThreadLocal<>();

    // ── Step shapes ─────────────────────────────────────────────────────────
    // Each step is 8 tall, and occupies one horizontal half:
    // - EAST/WEST: X is split (8 × 8 × 16)
    // - NORTH/SOUTH: Z is split (16 × 8 × 8)
    public static final VoxelShape BOTTOM_EAST  = Block.box(8.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    public static final VoxelShape BOTTOM_WEST  = Block.box(0.0, 0.0, 0.0,  8.0, 8.0, 16.0);
    public static final VoxelShape BOTTOM_SOUTH = Block.box(0.0, 0.0, 8.0, 16.0, 8.0, 16.0);
    public static final VoxelShape BOTTOM_NORTH = Block.box(0.0, 0.0, 0.0, 16.0, 8.0,  8.0);

    public static final VoxelShape TOP_EAST  = Block.box(8.0, 8.0, 0.0, 16.0, 16.0, 16.0);
    public static final VoxelShape TOP_WEST  = Block.box(0.0, 8.0, 0.0,  8.0, 16.0, 16.0);
    public static final VoxelShape TOP_SOUTH = Block.box(0.0, 8.0, 8.0, 16.0, 16.0, 16.0);
    public static final VoxelShape TOP_NORTH = Block.box(0.0, 8.0, 0.0, 16.0, 16.0,  8.0);

    public StepBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(FACING, Direction.EAST)
                .setValue(TOP, false));
    }

    // ── Static cache accessors (for destroy mixin) ──────────────────────────

    public static @Nullable BlockState bitsandbalance$peekCachedMinedSlab(BlockPos pos) {
        CachedBreak c = CACHED_BREAK.get();
        return (c != null && c.posLong == pos.asLong()) ? c.minedSlab : null;
    }

    public static void bitsandbalance$clearCachedBreak() {
        CACHED_BREAK.remove();
    }

    /**
     * Regular steps behave slab-like: placement chooses a horizontal half based on hit position.
     * Clicking a horizontal face pins the half adjacent to that face (opposite direction in the
     * placed block); otherwise chooses the closest horizontal side.
     */
    public static Direction bitsandbalance$getFacingForPlacement(BlockPos pos, Vec3 hitVec, Direction clickedFace) {
        return bitsandbalance$getFacingForPlacement(pos, hitVec, clickedFace, null);
    }

    /**
     * Placement orientation, matching vanilla slab-style targeting:
     * <ul>
      *   <li>Clicking a horizontal face (N/S/E/W) pins the adjacent half in the placed block.</li>
     *   <li>Otherwise, chooses the closest horizontal side based on the hit position.</li>
     * </ul>
     */
    public static Direction bitsandbalance$getFacingForPlacement(BlockPos pos, Vec3 hitVec,
                                                                 Direction clickedFace,
                                                                 @Nullable Player player) {
          if (clickedFace.getAxis().isHorizontal()) {
              double ratio = bitsandbalance$leftToRightRatio(pos, clickedFace, hitVec);
              if (ratio < bitsandbalance$EDGE_ZONE) return bitsandbalance$leftDirection(clickedFace);
              if (ratio > (1.0 - bitsandbalance$EDGE_ZONE)) return bitsandbalance$rightDirection(clickedFace);
              return clickedFace.getOpposite();
          }

        double rx = hitVec.x - pos.getX();
        double rz = hitVec.z - pos.getZ();
        double dx = rx - 0.5;
        double dz = rz - 0.5;

        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx > 0.0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0.0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static double bitsandbalance$leftToRightRatio(BlockPos pos, Direction clickedFace, Vec3 hitVec) {
        double rx = hitVec.x - pos.getX();
        double rz = hitVec.z - pos.getZ();
        rx = Math.max(0.0, Math.min(1.0, rx));
        rz = Math.max(0.0, Math.min(1.0, rz));

        return switch (clickedFace) {
            case NORTH -> 1.0 - rx;
            case SOUTH -> rx;
            case EAST -> 1.0 - rz;
            case WEST -> rz;
            default -> 0.5;
        };
    }

    private static Direction bitsandbalance$leftDirection(Direction clickedFace) {
        return switch (clickedFace) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.NORTH;
            default -> clickedFace.getOpposite();
        };
    }

    private static Direction bitsandbalance$rightDirection(Direction clickedFace) {
        return switch (clickedFace) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            default -> clickedFace.getOpposite();
        };
    }

    /**
     * Returns whether a fresh step placement should use the upper half.
     *
     * <p>For side-face placement, the clicked height on that side controls top vs bottom.
     * Clicking the underside of a block places a top step so it sits flush to that face.
     * Clicking the top face places a bottom step, matching slab-like behavior.</p>
     */
    public static boolean bitsandbalance$getTopForPlacement(BlockPos pos, Vec3 hitVec, Direction clickedFace) {
        if (clickedFace == Direction.DOWN) return true;
        if (clickedFace == Direction.UP) return false;
        return bitsandbalance$getTopFromHit(pos, hitVec);
    }

    /** Treats legacy north/south step facings as west/east halves (positive = east/south). */
    public static boolean bitsandbalance$isPositiveFacing(Direction facing) {
        return facing == Direction.EAST || facing == Direction.SOUTH;
    }

    /** Returns whether the player is aiming at the top half of the block. */
    public static boolean bitsandbalance$getTopFromHit(BlockPos pos, Vec3 hitVec) {
        double ry = hitVec.y - pos.getY();
        return ry > 0.5;
    }

    // ── Block-state definition ───────────────────────────────────────────────

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, FACING, TOP);
    }

    // ── Shapes ───────────────────────────────────────────────────────────────

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return quadrantShape(state.getValue(FACING), state.getValue(TOP));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return quadrantShape(state.getValue(FACING), state.getValue(TOP));
    }

    public static VoxelShape quadrantShape(Direction facing, boolean top) {
        return switch (facing) {
            case EAST  -> top ? TOP_EAST  : BOTTOM_EAST;
            case WEST  -> top ? TOP_WEST  : BOTTOM_WEST;
            case SOUTH -> top ? TOP_SOUTH : BOTTOM_SOUTH;
            case NORTH -> top ? TOP_NORTH : BOTTOM_NORTH;
            default -> top ? TOP_EAST : BOTTOM_EAST;
        };
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack,
                                          BlockState state,
                                          Level level,
                                          BlockPos pos,
                                          Player player,
                                          InteractionHand hand,
                                          BlockHitResult hitResult) {
        BlockState slabState = bitsandbalance$getStoredSlab(level, pos, state);
        DynamicVariantMaterialHelper.CopperUseResult result = DynamicVariantMaterialHelper.getCopperUseResult(stack, slabState);
        if (result == null) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        StepBlock ownerBlock = bitsandbalance$ownerBlockForSingle(result.slabState());
        BlockState newState = ownerBlock.defaultBlockState()
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED))
                .setValue(FACING, state.getValue(FACING))
                .setValue(TOP, state.getValue(TOP));
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        BlockEntity updatedBe = level.getBlockEntity(pos);
        if (updatedBe instanceof StepBlockEntity stepBlockEntity) {
            stepBlockEntity.setSlabState(result.slabState());
        }

        bitsandbalance$finishCopperUse(level, pos, player, hand, stack, result);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        if (this instanceof FixedStepBlock fixed) {
            return DynamicVariantMaterialHelper.isCopperRandomlyTicking(fixed.getSourceSlab().defaultBlockState());
        }
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState slabState = bitsandbalance$getStoredSlab(level, pos, state);
        BlockState nextSlab = DynamicVariantMaterialHelper.getCopperRandomTickNextState(slabState, level, pos, random);
        if (nextSlab == null) {
            return;
        }

        StepBlock ownerBlock = bitsandbalance$ownerBlockForSingle(nextSlab);
        BlockState newState = ownerBlock.defaultBlockState()
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED))
                .setValue(FACING, state.getValue(FACING))
                .setValue(TOP, state.getValue(TOP));
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        BlockEntity updatedBe = level.getBlockEntity(pos);
        if (updatedBe instanceof StepBlockEntity stepBlockEntity) {
            stepBlockEntity.setSlabState(nextSlab);
        }
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StepBlockEntity sbe && sbe.getSlabState() != null) {
                level.levelEvent(player, 2001, pos, Block.getId(sbe.getSlabState()));
                return;
            }
            BlockState cached = EnhancedSlabParticleStateCache.peek(level, pos);
            if (cached != null) {
                level.levelEvent(player, 2001, pos, Block.getId(cached));
                return;
            }
        } catch (Throwable ignored) {
        }
        if (this instanceof FixedStepBlock fixed) {
            level.levelEvent(player, 2001, pos, Block.getId(fixed.getSourceSlab().defaultBlockState()));
            return;
        }
        super.spawnDestroyParticles(level, player, pos, state);
    }

    // ── Block entity ─────────────────────────────────────────────────────────

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StepBlockEntity(pos, state);
    }

    // ── Waterlogging ─────────────────────────────────────────────────────────

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

    // ── Placement ────────────────────────────────────────────────────────────

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
        Vec3 hit = ctx.getClickLocation();
        BlockPos pos = ctx.getClickedPos();

        Direction face = ctx.getClickedFace();
        Direction facing = bitsandbalance$getFacingForPlacement(pos, hit, face, ctx.getPlayer());
        boolean top = bitsandbalance$getTopForPlacement(pos, hit, face);

        return this.defaultBlockState()
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER)
                .setValue(FACING, facing)
                .setValue(TOP, top);
    }

    // ── Pick block ────────────────────────────────────────────────────────────

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StepBlockEntity sbe && sbe.getSlabState() != null) {
            return bitsandbalance$makeStepDropItem(sbe.getSlabState());
        }
        if (this instanceof FixedStepBlock fixed) {
            return bitsandbalance$makeStepDropItem(fixed.getSourceSlab().defaultBlockState());
        }
        return ItemStack.EMPTY;
    }

    // ── Mining speed ──────────────────────────────────────────────────────────

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (this instanceof FixedStepBlock fixed) {
            return bitsandbalance$computeDestroyProgress(
                    fixed.getSourceSlab().defaultBlockState(), player, level, pos);
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StepBlockEntity sbe && sbe.getSlabState() != null) {
            return bitsandbalance$computeDestroyProgress(sbe.getSlabState(), player, level, pos);
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

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof StepBlockEntity sbe && sbe.getSlabState() != null) {
            ItemStack drop = bitsandbalance$makeStepDropItem(sbe.getSlabState());
            if (!drop.isEmpty()) return List.of(drop);
        }
        if (this instanceof FixedStepBlock fixed) {
            ItemStack drop = bitsandbalance$makeStepDropItem(fixed.getSourceSlab().defaultBlockState());
            if (!drop.isEmpty()) return List.of(drop);
        }
        return Collections.emptyList();
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockState slabState = null;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StepBlockEntity sbe) {
                slabState = sbe.getSlabState();
            } else if (this instanceof FixedStepBlock fixed) {
                slabState = fixed.getSourceSlab().defaultBlockState();
            }
            if (slabState != null) {
                CachedBreak cached = new CachedBreak();
                cached.posLong = pos.asLong();
                cached.minedSlab = slabState;
                CACHED_BREAK.set(cached);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos,
                              BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (level.isClientSide()) return;

        CachedBreak cached = CACHED_BREAK.get();
        if (cached == null || cached.posLong != pos.asLong()) {
            return;
        }

        if (!player.isCreative() && cached.minedSlab != null) {
            boolean correctTool = !cached.minedSlab.requiresCorrectToolForDrops()
                    || player.hasCorrectToolForDrops(cached.minedSlab);
            if (correctTool) {
                ItemStack drop = bitsandbalance$makeStepDropItem(cached.minedSlab);
                if (!drop.isEmpty()) bitsandbalance$popExactResource(level, pos, drop, player);
            }
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
        }

        CACHED_BREAK.remove();
    }

    // ── Drop helper ───────────────────────────────────────────────────────────

    /** Returns an {@link ItemStack} for the step registered to the given slab state. */
    public static ItemStack bitsandbalance$makeStepDropItem(BlockState slabState) {
        if (slabState == null) return ItemStack.EMPTY;
        Block slabBlock = slabState.getBlock();
        Block stepBlock = StepDynamicRegistry.getStepForSlab(slabBlock);

        ItemStack item;
        if (stepBlock != null && stepBlock.asItem() != Items.AIR) {
            item = new ItemStack(stepBlock.asItem());
        } else {
            // Fallback: return the slab item itself
            item = new ItemStack(slabBlock.asItem());
        }
        return item;
    }

    private static @Nullable BlockState bitsandbalance$getStoredSlab(BlockGetter level, BlockPos pos, BlockState ownerState) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof StepBlockEntity stepBlockEntity && stepBlockEntity.getSlabState() != null) {
            return DynamicVariantMaterialHelper.fallbackSlabState(stepBlockEntity.getSlabState());
        }
        if (ownerState.getBlock() instanceof FixedStepBlock fixed) {
            return DynamicVariantMaterialHelper.fallbackSlabState(fixed.getSourceSlab().defaultBlockState());
        }
        Block slabBlock = StepDynamicRegistry.getSlabForStep(ownerState.getBlock());
        if (slabBlock != null) {
            return DynamicVariantMaterialHelper.fallbackSlabState(slabBlock.defaultBlockState());
        }
        return null;
    }

    private static StepBlock bitsandbalance$ownerBlockForSingle(BlockState slabState) {
        Block stepBlock = StepDynamicRegistry.getStepForSlab(slabState.getBlock());
        if (stepBlock instanceof StepBlock step) {
            return step;
        }
        return CommonBlocks.STEP;
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
                    stack.hurtAndBreak(1, player, hand);
                }
            }
        } else if (result.consumeItem()) {
            stack.shrink(1);
        }
    }

    public static void bitsandbalance$popExactResource(Level level, BlockPos pos, ItemStack stack, @Nullable Player player) {
        ItemDropHomingHelper.popExactResource(level, pos, stack, player, bitsandbalance$STEP_DROP_HOME_DURATION_TICKS);
    }

    public static void bitsandbalance$markStepDropHomeToPlayer(ItemEntity itemEntity, @Nullable Player player) {
        ItemDropHomingHelper.markDropHomeToPlayer(itemEntity, player, bitsandbalance$STEP_DROP_HOME_DURATION_TICKS);
    }

    public static @Nullable String bitsandbalance$getStepDropHomeUuid(ItemEntity itemEntity) {
        return ItemDropHomingHelper.getDropHomeUuid(itemEntity);
    }

    public static long bitsandbalance$getStepDropHomeExpiresAt(ItemEntity itemEntity) {
        return ItemDropHomingHelper.getDropHomeExpiresAt(itemEntity);
    }

    public static void bitsandbalance$clearStepDropHoming(ItemEntity itemEntity) {
        ItemDropHomingHelper.clearDropHoming(itemEntity);
    }

    public static void bitsandbalance$setStepDropNoPhysics(ItemEntity itemEntity, boolean noPhysics) {
        ItemDropHomingHelper.setDropNoPhysics(itemEntity, noPhysics);
    }

    private static @Nullable Field bitsandbalance$getEntityNoPhysicsField(Class<?> startClass) {
        if (bitsandbalance$entityNoPhysicsFieldResolved) {
            return bitsandbalance$entityNoPhysicsField;
        }

        Class<?> current = startClass;
        while (current != null) {
            try {
                Field field = current.getDeclaredField("noPhysics");
                field.setAccessible(true);
                bitsandbalance$entityNoPhysicsField = field;
                bitsandbalance$entityNoPhysicsFieldResolved = true;
                return field;
            } catch (ReflectiveOperationException ignored) {
                current = current.getSuperclass();
            }
        }

        bitsandbalance$entityNoPhysicsFieldResolved = true;
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> bitsandbalance$getEntityTags(Entity entity) {
        if (entity == null) {
            return Collections.emptySet();
        }

        try {
            Method entityTagsMethod = entity.getClass().getMethod("entityTags");
            Object tags = entityTagsMethod.invoke(entity);
            if (tags instanceof Set<?> typedSet) {
                return (Set<String>) typedSet;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method getTagsMethod = entity.getClass().getMethod("getTags");
            Object tags = getTagsMethod.invoke(entity);
            if (tags instanceof Set<?> typedSet) {
                return (Set<String>) typedSet;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return Collections.emptySet();
    }

    // ── Inner cache ───────────────────────────────────────────────────────────

    private static final class CachedBreak {
        long posLong;
        @Nullable BlockState minedSlab;
    }
}
