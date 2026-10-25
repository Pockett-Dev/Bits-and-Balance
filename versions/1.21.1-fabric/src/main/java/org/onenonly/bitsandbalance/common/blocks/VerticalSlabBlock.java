package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabVerticalDestroyParticleCapture;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabStorageOptimizer;
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
public class VerticalSlabBlock extends Block implements SimpleWaterloggedBlock {

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

    private static @Nullable BlockState[] bitsandbalance$storedSlabs(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VerticalSlabBlockEntity vbe) {
            return new BlockState[]{vbe.getFacingSlab(), vbe.getOppositeSlab()};
        }
        BlockState[] createSlots = EnhancedSlabCreateCompat.getRenderSlots(level, pos);
        if (createSlots != null) {
            return createSlots;
        }
        return level instanceof Level actualLevel ? EnhancedSlabMaterialSavedData.get(actualLevel, pos.asLong()) : null;
    }

    private static @Nullable BlockState bitsandbalance$facingSlab(@Nullable BlockState[] slabs) {
        return slabs != null && slabs.length > 0 ? slabs[0] : null;
    }

    private static @Nullable BlockState bitsandbalance$oppositeSlab(@Nullable BlockState[] slabs) {
        return slabs != null && slabs.length > 1 ? slabs[1] : null;
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
        return bitsandbalance$shouldTargetHalf(player, state, verticalSlab, null);
    }

    public static boolean bitsandbalance$shouldTargetHalf(@Nullable Player player,
                                                          BlockState state,
                                                          @Nullable VerticalSlabBlockEntity verticalSlab,
                                                          @Nullable BlockState[] storedSlabs) {
        if (player != null && player.isShiftKeyDown()) return true;
        if (state == null || !state.getValue(DOUBLE)) return false;
        if (state.getBlock() instanceof FixedVerticalSlabBlock) return false;
        BlockState facing = verticalSlab != null ? verticalSlab.getFacingSlab() : bitsandbalance$slot(storedSlabs, 0);
        BlockState opposite = verticalSlab != null ? verticalSlab.getOppositeSlab() : bitsandbalance$slot(storedSlabs, 1);
        if (facing == null && opposite == null) return true;
        return facing == null || opposite == null || facing.getBlock() != opposite.getBlock();
    }

    private static @Nullable BlockState bitsandbalance$slot(@Nullable BlockState[] slabs, int index) {
        return slabs != null && index >= 0 && index < slabs.length ? slabs[index] : null;
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
        BlockState facingSlab = bitsandbalance$getFacingStoredSlab(level, pos, state);
        BlockState oppositeSlab = bitsandbalance$getOppositeStoredSlab(level, pos, state);
        double facingHeight = bitsandbalance$uprightHeight(level, pos, facingSlab);
        double oppositeHeight = bitsandbalance$uprightHeight(level, pos, oppositeSlab);

        if (!isDouble) {
            return halfShape(facing, facingHeight);
        }

        // KneeSlab-style selection for doubles: outline the half you're aiming at.
        // IMPORTANT: do NOT call player.pick() here (it clips using getShape()).
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            VerticalSlabBlockEntity vbe = blockEntity instanceof VerticalSlabBlockEntity verticalSlab ? verticalSlab : null;
            BlockState[] storedSlabs = vbe == null ? bitsandbalance$storedSlabs(level, pos) : null;
            if (!bitsandbalance$shouldTargetHalf(player, state, vbe, storedSlabs)) {
                return fullShape(Math.max(facingHeight, oppositeHeight));
            }
            try {
                BlockHitResult clipped = EnhancedSlabHelper.clipInteractionRayToFullCube(player, pos);
                if (clipped != null && clipped.getBlockPos().equals(pos)) {
                    Direction targeted = getTargetedHalf(pos, clipped.getLocation(), facing);
                    return halfShape(targeted, targeted == facing ? facingHeight : oppositeHeight);
                }
            } catch (Throwable ignored) {
            }
        }

        return Shapes.or(halfShape(facing, facingHeight), halfShape(facing.getOpposite(), oppositeHeight));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        BlockState facingSlab = bitsandbalance$getFacingStoredSlab(level, pos, state);
        double facingHeight = bitsandbalance$uprightHeight(level, pos, facingSlab);
        if (!state.getValue(DOUBLE)) {
            return halfShape(facing, facingHeight);
        }

        BlockState oppositeSlab = bitsandbalance$getOppositeStoredSlab(level, pos, state);
        double oppositeHeight = bitsandbalance$uprightHeight(level, pos, oppositeSlab);
        return Shapes.or(halfShape(facing, facingHeight), halfShape(facing.getOpposite(), oppositeHeight));
    }

    private static VoxelShape halfShape(Direction dir) {
        return halfShape(dir, 16.0);
    }

    private static VoxelShape halfShape(Direction dir, double maxY) {
        double clampedMaxY = Math.max(0.0, Math.min(16.0, maxY));
        return switch (dir) {
            case NORTH -> Block.box(0.0, 0.0, 0.0, 16.0, clampedMaxY, 8.0);
            case SOUTH -> Block.box(0.0, 0.0, 8.0, 16.0, clampedMaxY, 16.0);
            case WEST -> Block.box(0.0, 0.0, 0.0, 8.0, clampedMaxY, 16.0);
            case EAST -> Block.box(8.0, 0.0, 0.0, 16.0, clampedMaxY, 16.0);
            default -> fullShape(clampedMaxY);
        };
    }

    private static VoxelShape fullShape(double maxY) {
        double clampedMaxY = Math.max(0.0, Math.min(16.0, maxY));
        if (Math.abs(clampedMaxY - 16.0) < 0.001) {
            return Shapes.block();
        }
        return Block.box(0.0, 0.0, 0.0, 16.0, clampedMaxY, 16.0);
    }

    // ── Rendering ───────────────────────────────────────────────────────────

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack,
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
            return ItemInteractionResult.SUCCESS;
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
        if (ownerBlock instanceof FixedVerticalSlabBlock) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
        } else if (isDouble) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), newFacingSlab, newOppositeSlab);
        } else {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), newFacingSlab);
        }

        bitsandbalance$finishCopperUse(level, pos, player, hand, stack, result);
        return ItemInteractionResult.SUCCESS;
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
        if (ownerBlock instanceof FixedVerticalSlabBlock) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
        } else if (isDouble) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), updatedFacing, updatedOpposite);
        } else {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), updatedFacing);
        }
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        try {
            BlockEntity be = level.getBlockEntity(pos);
            VerticalSlabBlockEntity vbe = be instanceof VerticalSlabBlockEntity verticalSlab ? verticalSlab : null;
            BlockState[] slabs = bitsandbalance$storedSlabs(level, pos);
            if (slabs != null) {
                Direction facing = state.getValue(FACING);
                Direction targeted = facing;
                boolean targetSingleHalf = !state.getValue(DOUBLE)
                        || bitsandbalance$shouldTargetHalf(player, state, vbe, slabs);
                if (state.getValue(DOUBLE) && targetSingleHalf) {
                    targeted = getTargetedHalfFromRayToFullCube(player, pos, facing);
                }

                BlockState halfState = (targeted == facing)
                        ? bitsandbalance$facingSlab(slabs)
                        : bitsandbalance$oppositeSlab(slabs);

                if (halfState != null) {
                    if (level.isClientSide()) {
                        EnhancedSlabVerticalDestroyParticleCapture.capture(
                                level, pos, state, halfState, targeted,
                                state.getValue(DOUBLE) && !targetSingleHalf);
                    }
                    level.levelEvent(player, 2001, pos, Block.getId(halfState));
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        // Fallback for FixedVerticalSlabBlock when the block entity is absent.
        if (this instanceof FixedVerticalSlabBlock fixed) {
            BlockState material = fixed.getSourceSlab().defaultBlockState();
            Direction facing = state.getValue(FACING);
            boolean targetSingleHalf = !state.getValue(DOUBLE)
                    || bitsandbalance$shouldTargetHalf(player, state, null, null);
            Direction targeted = state.getValue(DOUBLE) && targetSingleHalf
                    ? getTargetedHalfFromRayToFullCube(player, pos, facing)
                    : facing;
            if (level.isClientSide()) {
                EnhancedSlabVerticalDestroyParticleCapture.capture(
                        level, pos, state, material, targeted,
                        state.getValue(DOUBLE) && !targetSingleHalf);
            }
            level.levelEvent(player, 2001, pos, Block.getId(material));
            return;
        }
        super.spawnDestroyParticles(level, player, pos, state);
    }

    // ── Block entity ────────────────────────────────────────────────────────

    // ── Waterlogging ────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction,
                                     BlockState neighborState, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
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
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        BlockState slab = bitsandbalance$getFacingStoredSlab(level, pos, state);
        if (slab != null) return bitsandbalance$makeHalfDropItem(slab);
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

        // Generic VerticalSlabBlock (mixed-material double slabs): consult stored slots for
        // the targeted half so each half's tool requirement is honoured independently.
        BlockState[] slabs = bitsandbalance$storedSlabs(level, pos);
        if (slabs == null) {
            return super.getDestroyProgress(state, player, level, pos);
        }

        BlockState halfState;
        if (!state.getValue(DOUBLE)) {
            halfState = bitsandbalance$facingSlab(slabs);
        } else {
            Direction facing = state.getValue(FACING);
            Direction targeted = getTargetedHalfFromRay(player, state, level, pos, facing);
            halfState = (targeted == facing) ? bitsandbalance$facingSlab(slabs) : bitsandbalance$oppositeSlab(slabs);
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
            BlockState[] slabs = blockEntity instanceof VerticalSlabBlockEntity vbe
                    ? new BlockState[]{vbe.getFacingSlab(), vbe.getOppositeSlab()}
                    : EnhancedSlabMaterialSavedData.get(level, pos.asLong());
            if (!player.isCreative()) {
                if (slabs != null) {
                    bitsandbalance$dropAllStoredSlabs(level, player, pos, bitsandbalance$facingSlab(slabs), bitsandbalance$oppositeSlab(slabs));
                    player.awardStat(Stats.BLOCK_MINED.get(this));
                    player.causeFoodExhaustion(0.005F);
                } else if (this instanceof FixedVerticalSlabBlock fixed) {
                    BlockState slab = fixed.getSourceSlab().defaultBlockState();
                    bitsandbalance$dropStoredSlab(level, player, pos, slab);
                    if (state.getValue(DOUBLE)) {
                        bitsandbalance$dropStoredSlab(level, player, pos, slab);
                    }
                    player.awardStat(Stats.BLOCK_MINED.get(this));
                    player.causeFoodExhaustion(0.005F);
                }
            }
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
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
            if (cached.remainingSlab != null) {
                EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), cached.remainingSlab);
            }
        }
        CACHED_BREAK.remove();
        if (cached.remainingSlab == null) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
        }
        return;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            VerticalSlabBlockEntity vbe = be instanceof VerticalSlabBlockEntity verticalSlab ? verticalSlab : null;
            BlockState[] slabs = bitsandbalance$storedSlabs(level, pos);
            if (slabs != null) {
                CachedBreak cached = new CachedBreak();
                cached.posLong = pos.asLong();

                Direction facingDir = state.getValue(FACING);
                boolean isDouble = state.getValue(DOUBLE);

                cached.facingSlab = bitsandbalance$facingSlab(slabs);
                cached.oppositeSlab = bitsandbalance$oppositeSlab(slabs);

                if (!isDouble) {
                    cached.targetedHalf = facingDir;
                    cached.minedSlab = cached.facingSlab;
                    cached.remainingBlockState = null;
                    cached.remainingSlab = null;
                } else {
                    if (!bitsandbalance$shouldTargetHalf(player, state, vbe, slabs)) {
                        CACHED_BREAK.remove();
                        return super.playerWillDestroy(level, pos, state, player);
                    }
                    Direction targeted = getTargetedHalfFromRayToFullCube(player, pos, facingDir);
                    cached.targetedHalf = targeted;
                    cached.minedSlab = (targeted == facingDir) ? cached.facingSlab : cached.oppositeSlab;

                    Direction remainingDir = (targeted == facingDir) ? facingDir.getOpposite() : facingDir;
                    cached.remainingSlab = (targeted == facingDir) ? cached.oppositeSlab : cached.facingSlab;

                    cached.remainingBlockState = bitsandbalance$singleVerticalStateFor(cached.remainingSlab,
                            state.getValue(WATERLOGGED),
                            remainingDir);
                }

                CACHED_BREAK.set(cached);
            } else if (this instanceof FixedVerticalSlabBlock fixed) {
                // BE not loaded (rare edge case): use the statically known source slab so that
                // drops still occur correctly for single-slab variants.
                if (state.getValue(DOUBLE) && !player.isShiftKeyDown()) {
                    CACHED_BREAK.remove();
                    return super.playerWillDestroy(level, pos, state, player);
                }
                BlockState sourceSlab = fixed.getSourceSlab().defaultBlockState();
                CachedBreak cached = new CachedBreak();
                cached.posLong = pos.asLong();
                cached.facingSlab = sourceSlab;
                cached.oppositeSlab = sourceSlab;
                Direction facingDir = state.getValue(FACING);
                Direction targeted = state.getValue(DOUBLE)
                        ? getTargetedHalfFromRayToFullCube(player, pos, facingDir)
                        : facingDir;
                cached.targetedHalf = targeted;
                cached.minedSlab = sourceSlab;
                if (state.getValue(DOUBLE)) {
                    Direction remainingDir = (targeted == facingDir) ? facingDir.getOpposite() : facingDir;
                    cached.remainingBlockState = bitsandbalance$singleVerticalStateFor(sourceSlab,
                            state.getValue(WATERLOGGED),
                            remainingDir);
                    cached.remainingSlab = sourceSlab;
                } else {
                    cached.remainingBlockState = null;
                    cached.remainingSlab = null;
                }
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
                if (level instanceof Level actualLevel && cached.remainingSlab != null) {
                    EnhancedSlabMaterialSavedData.remember(actualLevel, pos.asLong(), cached.remainingSlab);
                }
            }
        }
        super.destroy(level, pos, state);
    }

    // ── Targeting helpers ──────────────────────────────────────────────────

    private static Direction getTargetedHalfFromRayToFullCube(Player player, BlockPos pos, Direction facingDir) {
        BlockHitResult clipped = EnhancedSlabHelper.clipInteractionRayToFullCube(player, pos);
        if (clipped != null) {
            return getTargetedHalf(pos, clipped.getLocation(), facingDir);
        }
        return facingDir;
    }

    private static Direction getTargetedHalfFromRay(Player player, BlockState state, BlockGetter level, BlockPos pos, Direction facingDir) {
        var ray = EnhancedSlabHelper.getInteractionRay(player, pos);

        VoxelShape shape = state.getShape(level, pos, CollisionContext.of(player));
        BlockHitResult clipped = shape.clip(ray.start(), ray.end(), pos);
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
        if (level instanceof Level actualLevel) {
            BlockState[] stored = EnhancedSlabMaterialSavedData.get(actualLevel, pos.asLong());
            BlockState facing = bitsandbalance$facingSlab(stored);
            if (facing != null) {
                return DynamicVariantMaterialHelper.fallbackSlabState(facing);
            }
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
        if (level instanceof Level actualLevel) {
            BlockState[] stored = EnhancedSlabMaterialSavedData.get(actualLevel, pos.asLong());
            BlockState opposite = bitsandbalance$oppositeSlab(stored);
            if (opposite != null) {
                return DynamicVariantMaterialHelper.fallbackSlabState(opposite);
            }
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

    private static double bitsandbalance$uprightHeight(BlockGetter level, BlockPos pos, @Nullable BlockState slabState) {
        return 8.0 + bitsandbalance$sourceThickness(level, pos, slabState);
    }

    private static double bitsandbalance$sourceThickness(BlockGetter level, BlockPos pos, @Nullable BlockState slabState) {
        if (slabState == null) {
            return 8.0;
        }

        try {
            VoxelShape shape = slabState.getShape(level, pos, CollisionContext.empty());
            if (!shape.isEmpty()) {
                double thickness = (shape.max(Direction.Axis.Y) - shape.min(Direction.Axis.Y)) * 16.0;
                if (thickness > 0.0) {
                    return Math.max(0.0, Math.min(8.0, thickness));
                }
            }
        } catch (Throwable ignored) {
        }
        return 8.0;
    }

    private static VerticalSlabBlock bitsandbalance$ownerBlockForSingle(BlockState slabState) {
        if (slabState != null) {
            Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabState.getBlock());
            if (verticalBlock instanceof VerticalSlabBlock verticalSlab) {
                return verticalSlab;
            }
        }
        return CommonBlocks.VERTICAL_SLAB;
    }

    private static BlockState bitsandbalance$singleVerticalStateFor(BlockState slabState, boolean waterlogged, Direction facing) {
        return bitsandbalance$ownerBlockForSingle(slabState).defaultBlockState()
                .setValue(WATERLOGGED, waterlogged)
                .setValue(FACING, facing)
                .setValue(DOUBLE, false);
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
