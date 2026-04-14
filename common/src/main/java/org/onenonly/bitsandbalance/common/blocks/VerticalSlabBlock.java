package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SlabBlock;
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
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior — Vertical Slabs
 *
 * A compound block that stores one or two slab types as vertical halves inside a single
 * block position. Created by intercepting slab placement, so it works automatically with
 * vanilla + modded slabs.
 */
public class VerticalSlabBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {

    private static final double bitsandbalance$EDGE_ZONE = 0.25;

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty DOUBLE = BooleanProperty.create("double");

    private static final ThreadLocal<CachedBreak> CACHED_BREAK = new ThreadLocal<>();

    private static final VoxelShape NORTH_HALF_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0);
    private static final VoxelShape SOUTH_HALF_SHAPE = Block.box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0);
    private static final VoxelShape WEST_HALF_SHAPE  = Block.box(0.0, 0.0, 0.0, 8.0, 16.0, 16.0);
    private static final VoxelShape EAST_HALF_SHAPE  = Block.box(8.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public VerticalSlabBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(FACING, Direction.NORTH)
                .setValue(DOUBLE, false));
    }

    public static @Nullable BlockState bitsandbalance$peekCachedMinedSlab(BlockPos pos) {
        CachedBreak cached = CACHED_BREAK.get();
        if (cached == null || cached.posLong != pos.asLong()) return null;
        return cached.minedSlab;
    }

    public static @Nullable BlockState bitsandbalance$peekCachedRemainingState(BlockPos pos) {
        CachedBreak cached = CACHED_BREAK.get();
        if (cached == null || cached.posLong != pos.asLong()) return null;
        return cached.remainingBlockState;
    }

    public static @Nullable BlockState bitsandbalance$peekCachedRemainingSlab(BlockPos pos) {
        CachedBreak cached = CACHED_BREAK.get();
        if (cached == null || cached.posLong != pos.asLong()) return null;
        return cached.remainingSlab;
    }

    public static void bitsandbalance$clearCachedBreak() {
        CACHED_BREAK.remove();
    }

    public static Direction bitsandbalance$getTargetedHalf(Player player, BlockPos pos, Direction facingDir) {
        return getTargetedHalfFromRayToFullCube(player, pos, facingDir);
    }

    public static Direction bitsandbalance$getTargetedHalf(BlockPos pos, Vec3 hitVec, Direction facingDir) {
        return getTargetedHalf(pos, hitVec, facingDir);
    }

    public static boolean bitsandbalance$shouldTargetHalf(@Nullable Player player,
                                                          BlockState state,
                                                          @Nullable VerticalSlabBlockEntity verticalSlab) {
        if (player != null && player.isShiftKeyDown()) return true;
        if (state == null || !state.getValue(DOUBLE)) return false;
        if (verticalSlab == null) return true;
        BlockState facing = verticalSlab.getFacingSlab();
        BlockState opposite = verticalSlab.getOppositeSlab();
        return facing == null || opposite == null || !facing.equals(opposite);
    }

    // ── Block-state definition ──────────────────────────────────────────────

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, FACING, DOUBLE);
    }

    // ── Shapes ──────────────────────────────────────────────────────────────

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean isDouble = state.getValue(DOUBLE);
        Direction facing = state.getValue(FACING);

        if (!isDouble) {
            return halfShape(facing);
        }

        // KneeSlab-style selection for doubles: outline the half you're aiming at.
        // IMPORTANT: do NOT call player.pick() here (it clips using getShape()).
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player) {
            if (level.getBlockEntity(pos) instanceof VerticalSlabBlockEntity vbe
                    && !bitsandbalance$shouldTargetHalf(player, state, vbe)) {
                return Shapes.block();
            }
            try {
                Vec3 start = player.getEyePosition();
                Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));

                BlockHitResult clipped = Shapes.block().clip(start, end, pos);
                if (clipped != null && clipped.getBlockPos().equals(pos)) {
                    Direction targeted = getTargetedHalf(pos, clipped.getLocation(), facing);
                    return halfShape(targeted);
                }
            } catch (Throwable ignored) {
            }
        }

        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(DOUBLE) ? Shapes.block() : halfShape(state.getValue(FACING));
    }

    private static VoxelShape halfShape(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH_HALF_SHAPE;
            case SOUTH -> SOUTH_HALF_SHAPE;
            case WEST -> WEST_HALF_SHAPE;
            case EAST -> EAST_HALF_SHAPE;
            default -> Shapes.block();
        };
    }

    // ── Rendering ───────────────────────────────────────────────────────────

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
        BlockState facingSlab = bitsandbalance$getFacingStoredSlab(level, pos, state);
        BlockState oppositeSlab = bitsandbalance$getOppositeStoredSlab(level, pos, state);
        if (facingSlab == null) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        Direction facing = state.getValue(FACING);
        boolean isDouble = state.getValue(DOUBLE);
        Direction targetedHalf = isDouble ? getTargetedHalf(pos, hitResult.getLocation(), facing) : facing;
        BlockState targetSlab = targetedHalf == facing ? facingSlab : oppositeSlab;
        DynamicVariantMaterialHelper.CopperUseResult result = DynamicVariantMaterialHelper.getCopperUseResult(stack, targetSlab);
        if (result == null) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockState newFacingSlab = facingSlab;
        BlockState newOppositeSlab = oppositeSlab;
        if (targetedHalf == facing) {
            newFacingSlab = result.slabState();
        } else {
            newOppositeSlab = result.slabState();
        }

        VerticalSlabBlock ownerBlock = isDouble
                ? bitsandbalance$ownerBlockForDouble(newFacingSlab, newOppositeSlab)
                : bitsandbalance$ownerBlockForSingle(newFacingSlab);

        BlockState newState = ownerBlock.defaultBlockState()
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED))
                .setValue(FACING, state.getValue(FACING))
                .setValue(DOUBLE, isDouble);
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        BlockEntity updatedBe = level.getBlockEntity(pos);
        if (updatedBe instanceof VerticalSlabBlockEntity verticalSlab) {
            if (isDouble) {
                verticalSlab.setSlabs(newFacingSlab, newOppositeSlab);
            } else {
                verticalSlab.setSingleFacingSlab(newFacingSlab);
            }
        }

        bitsandbalance$finishCopperUse(level, pos, player, hand, stack, result);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        if (this instanceof FixedVerticalSlabBlock fixed) {
            return DynamicVariantMaterialHelper.isCopperRandomlyTicking(fixed.getSourceSlab().defaultBlockState());
        }
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState facingSlab = bitsandbalance$getFacingStoredSlab(level, pos, state);
        if (facingSlab == null) {
            return;
        }

        Direction facing = state.getValue(FACING);
        boolean isDouble = state.getValue(DOUBLE);
        BlockState oppositeSlab = bitsandbalance$getOppositeStoredSlab(level, pos, state);

        BlockState nextFacing = DynamicVariantMaterialHelper.getCopperRandomTickNextState(facingSlab, level, pos, random);
        BlockState nextOpposite = isDouble
                ? DynamicVariantMaterialHelper.getCopperRandomTickNextState(oppositeSlab, level, pos, random)
                : null;

        if (nextFacing == null && nextOpposite == null) {
            return;
        }

        if (nextFacing != null && nextOpposite != null) {
            if (random.nextBoolean()) {
                nextFacing = null;
            } else {
                nextOpposite = null;
            }
        }

        BlockState updatedFacing = nextFacing != null ? nextFacing : facingSlab;
        BlockState updatedOpposite = nextOpposite != null ? nextOpposite : oppositeSlab;
        VerticalSlabBlock ownerBlock = isDouble
                ? bitsandbalance$ownerBlockForDouble(updatedFacing, updatedOpposite)
                : bitsandbalance$ownerBlockForSingle(updatedFacing);

        BlockState newState = ownerBlock.defaultBlockState()
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED))
                .setValue(FACING, facing)
                .setValue(DOUBLE, isDouble);
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        BlockEntity updatedBe = level.getBlockEntity(pos);
        if (updatedBe instanceof VerticalSlabBlockEntity verticalSlab) {
            if (isDouble) {
                verticalSlab.setSlabs(updatedFacing, updatedOpposite);
            } else {
                verticalSlab.setSingleFacingSlab(updatedFacing);
            }
        }
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VerticalSlabBlockEntity vbe) {
                Direction facing = state.getValue(FACING);
                Direction targeted = facing;
                if (state.getValue(DOUBLE) && bitsandbalance$shouldTargetHalf(player, state, vbe)) {
                    targeted = getTargetedHalfFromRayToFullCube(player, pos, facing);
                }

                BlockState halfState = (targeted == facing)
                        ? vbe.getFacingSlab()
                        : vbe.getOppositeSlab();

                if (halfState != null) {
                    level.levelEvent(player, 2001, pos, Block.getId(halfState));
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        // Fallback for FixedVerticalSlabBlock when the block entity is absent.
        if (this instanceof FixedVerticalSlabBlock fixed) {
            level.levelEvent(player, 2001, pos, Block.getId(fixed.getSourceSlab().defaultBlockState()));
            return;
        }
        super.spawnDestroyParticles(level, player, pos, state);
    }

    // ── Block entity ────────────────────────────────────────────────────────

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VerticalSlabBlockEntity(pos, state);
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

    /**
     * Determines the half used for vertical slab placement.
     *
     * <p>Clicking a horizontal face places the slab flush against that face in the adjacent block,
     * so the occupied half is the opposite direction. Top/bottom face placement uses the hit X/Z
     * position to choose the nearest side.</p>
     */
    public static Direction bitsandbalance$getFacingForPlacement(BlockPos pos, Vec3 hitVec, Direction clickedFace) {
        if (clickedFace.getAxis().isHorizontal()) {
            double ratio = bitsandbalance$leftToRightRatio(pos, clickedFace, hitVec);
            if (ratio < bitsandbalance$EDGE_ZONE) return bitsandbalance$leftDirection(clickedFace);
            if (ratio > (1.0 - bitsandbalance$EDGE_ZONE)) return bitsandbalance$rightDirection(clickedFace);
            return clickedFace.getOpposite();
        }

        if (pos == null || hitVec == null) return Direction.NORTH;
        double rx = hitVec.x - pos.getX();
        double rz = hitVec.z - pos.getZ();

        if (rx < 0.0) rx = 0.0;
        if (rx > 1.0) rx = 1.0;
        if (rz < 0.0) rz = 0.0;
        if (rz > 1.0) rz = 1.0;

        double dx = rx - 0.5;
        double dz = rz - 0.5;
        if (Math.abs(dx) > Math.abs(dz)) {
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

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        Direction facing = bitsandbalance$getFacingForPlacement(
                context.getClickedPos(), context.getClickLocation(), context.getClickedFace());
        return this.defaultBlockState()
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER)
                .setValue(FACING, facing)
                .setValue(DOUBLE, false);
    }

    // ── Pick block ─────────────────────────────────────────────────────────

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VerticalSlabBlockEntity vbe) {
            BlockState slab = vbe.getFacingSlab();
            if (slab != null) return bitsandbalance$makeHalfDropItem(slab);
        }
        return ItemStack.EMPTY;
    }

    // ── Mining speed (per-half) ─────────────────────────────────────────────

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // For FixedVerticalSlabBlock the source slab is always known statically —
        // no block entity is needed. This makes tool/tier checking reliable regardless
        // of whether the BE has been synced yet (avoids the OAK default fallback).
        if (this instanceof FixedVerticalSlabBlock fixed) {
            return bitsandbalance$computeDestroyProgress(
                    fixed.getSourceSlab().defaultBlockState(), player, level, pos);
        }

        // Generic VerticalSlabBlock (mixed-material double slabs): consult the BE for
        // the targeted half so each half's tool requirement is honoured independently.
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof VerticalSlabBlockEntity vbe)) {
            return super.getDestroyProgress(state, player, level, pos);
        }

        BlockState halfState;
        if (!state.getValue(DOUBLE)) {
            halfState = vbe.getFacingSlab();
        } else {
            Direction facing = state.getValue(FACING);
            Direction targeted = getTargetedHalfFromRay(player, state, level, pos, facing);
            halfState = (targeted == facing) ? vbe.getFacingSlab() : vbe.getOppositeSlab();
        }

        if (halfState == null) {
            return super.getDestroyProgress(state, player, level, pos);
        }
        return bitsandbalance$computeDestroyProgress(halfState, player, level, pos);
    }

    private static float bitsandbalance$computeDestroyProgress(BlockState ref, Player player,
                                                                BlockGetter level, BlockPos pos) {
        float hardness = ref.getDestroySpeed(level, pos);
        if (hardness == -1.0F) return 0.0F;
        boolean correctTool = !ref.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(ref);
        float speedFactor = player.getDestroySpeed(ref);
        return correctTool
                ? speedFactor / hardness / 30.0F
                : speedFactor / hardness / 100.0F;
    }

    // ── Breaking / drops ────────────────────────────────────────────────────

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof VerticalSlabBlockEntity vbe) {
            List<ItemStack> drops = new ArrayList<>(2);
            BlockState facing = vbe.getFacingSlab();
            BlockState opposite = vbe.getOppositeSlab();

            if (!state.getValue(DOUBLE)) {
                if (facing != null) drops.add(bitsandbalance$makeHalfDropItem(facing));
                return drops;
            }

            if (facing != null) drops.add(bitsandbalance$makeHalfDropItem(facing));
            if (opposite != null) drops.add(bitsandbalance$makeHalfDropItem(opposite));
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
            if (!player.isCreative() && blockEntity instanceof VerticalSlabBlockEntity vbe) {
                bitsandbalance$dropAllStoredSlabs(level, player, pos, vbe.getFacingSlab(), vbe.getOppositeSlab());
                player.awardStat(Stats.BLOCK_MINED.get(this));
                player.causeFoodExhaustion(0.005F);
            }
            return;
        }

        if (!player.isCreative() && cached.minedSlab != null) {
            boolean correctTool = !cached.minedSlab.requiresCorrectToolForDrops()
                    || player.hasCorrectToolForDrops(cached.minedSlab);
            if (correctTool) {
                ItemStack drop = bitsandbalance$makeHalfDropItem(cached.minedSlab);
                if (!drop.isEmpty()) {
                    Block.popResource(level, pos, drop);
                }
            }

            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
        }

        if (cached.remainingBlockState != null && (level.getBlockState(pos).isAir() || level.getBlockState(pos).is(Blocks.WATER))) {
            level.setBlock(pos, cached.remainingBlockState, Block.UPDATE_ALL);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VerticalSlabBlockEntity vbe && cached.remainingSlab != null) {
                vbe.setSingleFacingSlab(cached.remainingSlab);
            }
        }
        CACHED_BREAK.remove();
        return;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VerticalSlabBlockEntity vbe) {
                CachedBreak cached = new CachedBreak();
                cached.posLong = pos.asLong();

                Direction facingDir = state.getValue(FACING);
                boolean isDouble = state.getValue(DOUBLE);

                cached.facingSlab = vbe.getFacingSlab();
                cached.oppositeSlab = vbe.getOppositeSlab();

                if (!isDouble) {
                    cached.targetedHalf = facingDir;
                    cached.minedSlab = cached.facingSlab;
                    cached.remainingBlockState = null;
                    cached.remainingSlab = null;
                } else {
                    if (!bitsandbalance$shouldTargetHalf(player, state, vbe)) {
                        CACHED_BREAK.remove();
                        return super.playerWillDestroy(level, pos, state, player);
                    }
                    Direction targeted = getTargetedHalfFromRayToFullCube(player, pos, facingDir);
                    cached.targetedHalf = targeted;
                    cached.minedSlab = (targeted == facingDir) ? cached.facingSlab : cached.oppositeSlab;

                    Direction remainingDir = (targeted == facingDir) ? facingDir.getOpposite() : facingDir;
                    cached.remainingSlab = (targeted == facingDir) ? cached.oppositeSlab : cached.facingSlab;

                    cached.remainingBlockState = this.defaultBlockState()
                            .setValue(WATERLOGGED, state.getValue(WATERLOGGED))
                            .setValue(FACING, remainingDir)
                            .setValue(DOUBLE, false);
                }

                CACHED_BREAK.set(cached);
            } else if (this instanceof FixedVerticalSlabBlock fixed) {
                // BE not loaded (rare edge case): use the statically known source slab so that
                // drops still occur correctly for single-slab variants.
                BlockState sourceSlab = fixed.getSourceSlab().defaultBlockState();
                CachedBreak cached = new CachedBreak();
                cached.posLong = pos.asLong();
                cached.facingSlab = sourceSlab;
                cached.oppositeSlab = sourceSlab;
                cached.targetedHalf = state.getValue(FACING);
                cached.minedSlab = sourceSlab;
                cached.remainingBlockState = null;
                cached.remainingSlab = null;
                CACHED_BREAK.set(cached);
            } else {
                CACHED_BREAK.remove();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        CachedBreak cached = CACHED_BREAK.get();
        if (cached != null && cached.posLong == pos.asLong() && cached.remainingBlockState != null) {
            if (level.getBlockState(pos).isAir() || level.getBlockState(pos).is(Blocks.WATER)) {
                level.setBlock(pos, cached.remainingBlockState, Block.UPDATE_ALL);
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof VerticalSlabBlockEntity vbe && cached.remainingSlab != null) {
                    vbe.setSingleFacingSlab(cached.remainingSlab);
                }
            }
        }
        super.destroy(level, pos, state);
    }

    // ── Targeting helpers ──────────────────────────────────────────────────

    private static Direction getTargetedHalfFromRayToFullCube(Player player, BlockPos pos, Direction facingDir) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        BlockHitResult clipped = Shapes.block().clip(start, end, pos);
        if (clipped != null) {
            return getTargetedHalf(pos, clipped.getLocation(), facingDir);
        }
        return facingDir;
    }

    private static Direction getTargetedHalfFromRay(Player player, BlockState state, BlockGetter level, BlockPos pos, Direction facingDir) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));

        VoxelShape shape = state.getShape(level, pos, CollisionContext.of(player));
        BlockHitResult clipped = shape.clip(start, end, pos);
        if (clipped != null) {
            return getTargetedHalf(pos, clipped.getLocation(), facingDir);
        }

        return facingDir;
    }

    private static Direction getTargetedHalf(BlockPos pos, Vec3 hitVec, Direction facingDir) {
        double rx = hitVec.x - pos.getX();
        double rz = hitVec.z - pos.getZ();

        if (facingDir == Direction.EAST || facingDir == Direction.WEST) {
            boolean hitEast = rx > 0.5;
            return hitEast ? Direction.EAST : Direction.WEST;
        }

        boolean hitSouth = rz > 0.5;
        return hitSouth ? Direction.SOUTH : Direction.NORTH;
    }

    private static @Nullable BlockState bitsandbalance$getFacingStoredSlab(BlockGetter level, BlockPos pos, BlockState ownerState) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            return DynamicVariantMaterialHelper.fallbackSlabState(verticalSlab.getFacingSlab());
        }
        if (ownerState.getBlock() instanceof FixedVerticalSlabBlock fixed) {
            return DynamicVariantMaterialHelper.fallbackSlabState(fixed.getSourceSlab().defaultBlockState());
        }
        Block slabBlock = VerticalSlabDynamicRegistry.getSlabForVertical(ownerState.getBlock());
        if (slabBlock != null) {
            return DynamicVariantMaterialHelper.fallbackSlabState(slabBlock.defaultBlockState());
        }
        return null;
    }

    private static @Nullable BlockState bitsandbalance$getOppositeStoredSlab(BlockGetter level, BlockPos pos, BlockState ownerState) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            return DynamicVariantMaterialHelper.fallbackSlabState(verticalSlab.getOppositeSlab());
        }
        if (ownerState.getBlock() instanceof FixedVerticalSlabBlock fixed) {
            return DynamicVariantMaterialHelper.fallbackSlabState(fixed.getSourceSlab().defaultBlockState());
        }
        Block slabBlock = VerticalSlabDynamicRegistry.getSlabForVertical(ownerState.getBlock());
        if (slabBlock != null) {
            return DynamicVariantMaterialHelper.fallbackSlabState(slabBlock.defaultBlockState());
        }
        return null;
    }

    private static VerticalSlabBlock bitsandbalance$ownerBlockForSingle(BlockState slabState) {
        Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabState.getBlock());
        if (verticalBlock instanceof VerticalSlabBlock verticalSlab) {
            return verticalSlab;
        }
        return CommonBlocks.VERTICAL_SLAB;
    }

    private static VerticalSlabBlock bitsandbalance$ownerBlockForDouble(@Nullable BlockState facingSlab, @Nullable BlockState oppositeSlab) {
        if (facingSlab != null && oppositeSlab != null && facingSlab.getBlock() == oppositeSlab.getBlock()) {
            return bitsandbalance$ownerBlockForSingle(facingSlab);
        }
        return CommonBlocks.VERTICAL_SLAB;
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

    public static ItemStack bitsandbalance$makeHalfDropItem(BlockState halfState) {
        Block slabBlock = halfState.getBlock();
        Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock);

        ItemStack item;
        if (verticalBlock != null && verticalBlock.asItem() != Items.AIR) {
            item = new ItemStack(verticalBlock.asItem());
        } else {
            item = new ItemStack(slabBlock.asItem());
        }
        return item;
    }

    private static void bitsandbalance$dropAllStoredSlabs(Level level,
                                                          Player player,
                                                          BlockPos pos,
                                                          @Nullable BlockState facingSlab,
                                                          @Nullable BlockState oppositeSlab) {
        bitsandbalance$dropStoredSlab(level, player, pos, facingSlab);
        bitsandbalance$dropStoredSlab(level, player, pos, oppositeSlab);
    }

    private static void bitsandbalance$dropStoredSlab(Level level,
                                                      Player player,
                                                      BlockPos pos,
                                                      @Nullable BlockState slabState) {
        if (slabState == null) return;

        boolean correctTool = !slabState.requiresCorrectToolForDrops()
                || player.hasCorrectToolForDrops(slabState);
        if (!correctTool) return;

        ItemStack drop = bitsandbalance$makeHalfDropItem(slabState);
        if (drop.isEmpty()) return;

        Block.popResource(level, pos, drop);
    }

    private static final class CachedBreak {
        long posLong;
        @Nullable BlockState facingSlab;
        @Nullable BlockState oppositeSlab;
        @Nullable Direction targetedHalf;
        @Nullable BlockState minedSlab;
        @Nullable BlockState remainingBlockState;
        @Nullable BlockState remainingSlab;
    }
}
