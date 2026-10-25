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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabParticleStateCache;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import net.minecraft.world.item.DyeColor;

import java.util.Collections;
import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior — Vertical Steps
 *
 * A ¼-block vertical step occupying one of four quadrants in a block position.
 * Geometry: 8 wide (X) × 16 tall (Y) × 8 deep (Z).
 *
 * Single vertical step blocks are always {@link FixedVerticalStepBlock} instances keyed to
 * a source vertical slab. When 2–4 vertical steps share a position, a
 * {@link QuadVerticalStepBlock} container takes over.
 */
public class VerticalStepBlock extends Block implements SimpleWaterloggedBlock {

    private static final double BITSANDBALANCE_FACE_EPS = 1.0E-6;
    private static final ResourceLocation bitsandbalance$VINERY_DIRT_PATH_SLAB_ID = ResourceLocation.fromNamespaceAndPath("vinery", "dirt_path_slab");

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** Which depth half — north (Z 0-8) or south (Z 8-16). */
    public static final EnumProperty<Direction> FACING =
            EnumProperty.create("facing", Direction.class, Direction.NORTH, Direction.SOUTH);
    /** Which horizontal half — east (X 8-16) or west (X 0-8). */
    public static final EnumProperty<Direction> SIDE =
            EnumProperty.create("side", Direction.class, Direction.EAST, Direction.WEST);

    private static final ThreadLocal<CachedBreak> CACHED_BREAK = new ThreadLocal<>();

    // ── Quadrant shapes (X × Y × Z) ──────────────────────────────────────────
    public static final VoxelShape NORTH_WEST = Block.box(0.0, 0.0, 0.0,  8.0, 16.0,  8.0);
    public static final VoxelShape NORTH_EAST = Block.box(8.0, 0.0, 0.0, 16.0, 16.0,  8.0);
    public static final VoxelShape SOUTH_WEST = Block.box(0.0, 0.0, 8.0,  8.0, 16.0, 16.0);
    public static final VoxelShape SOUTH_EAST = Block.box(8.0, 0.0, 8.0, 16.0, 16.0, 16.0);

    public VerticalStepBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(FACING, Direction.NORTH)
                .setValue(SIDE, Direction.EAST));
    }

    // ── Static cache accessors (for destroy mixin) ───────────────────────────

    public static @Nullable BlockState bitsandbalance$peekCachedMinedSlab(BlockPos pos) {
        CachedBreak c = CACHED_BREAK.get();
        return (c != null && c.posLong == pos.asLong()) ? c.minedSlab : null;
    }

    public static void bitsandbalance$clearCachedBreak() {
        CACHED_BREAK.remove();
    }

    /** Returns the quadrant facing (north/south) from the player's click location. */
    public static Direction bitsandbalance$getFacingFromHit(BlockPos pos, Vec3 hitVec) {
        double rz = hitVec.z - pos.getZ();
        return rz > 0.5 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Returns the side (east/west) from the player's click location. */
    public static Direction bitsandbalance$getSideFromHit(BlockPos pos, Vec3 hitVec) {
        double rx = hitVec.x - pos.getX();
        return rx > 0.5 ? Direction.EAST : Direction.WEST;
    }

    /** Returns the targeted quadrant index with face-aware boundary handling. */
    public static int bitsandbalance$getPlacementIndex(BlockPos pos, Vec3 hitVec, Direction clickedFace) {
        double rx = hitVec.x - pos.getX();
        double rz = hitVec.z - pos.getZ();
        boolean east = rx > 0.5;
        boolean south = rz > 0.5;

        if (Math.abs(rx - 0.5) < BITSANDBALANCE_FACE_EPS && clickedFace.getAxis() == Direction.Axis.X) {
            east = clickedFace == Direction.EAST;
        }
        if (Math.abs(rz - 0.5) < BITSANDBALANCE_FACE_EPS && clickedFace.getAxis() == Direction.Axis.Z) {
            south = clickedFace == Direction.SOUTH;
        }

        return QuadVerticalStepBlock.toIndex(east, south);
    }

    /** Returns the depth half used for a fresh placement at the target position. */
    public static Direction bitsandbalance$getFacingForPlacement(BlockPos pos, Vec3 hitVec, Direction clickedFace) {
        if (clickedFace.getAxis() == Direction.Axis.Z) {
            return clickedFace.getOpposite();
        }
        return bitsandbalance$getFacingFromHit(pos, hitVec);
    }

    /** Returns the width half used for a fresh placement at the target position. */
    public static Direction bitsandbalance$getSideForPlacement(BlockPos pos, Vec3 hitVec, Direction clickedFace) {
        if (clickedFace.getAxis() == Direction.Axis.X) {
            return clickedFace.getOpposite();
        }
        return bitsandbalance$getSideFromHit(pos, hitVec);
    }

    // ── Block-state definition ───────────────────────────────────────────────

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, FACING, SIDE);
    }

    // ── Shapes ───────────────────────────────────────────────────────────────

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return quadrantShape(state.getValue(FACING), state.getValue(SIDE), bitsandbalance$uprightHeight(level, pos, state));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return quadrantShape(state.getValue(FACING), state.getValue(SIDE), bitsandbalance$uprightHeight(level, pos, state));
    }

    public static VoxelShape quadrantShape(Direction facing, Direction side) {
        return quadrantShape(facing, side, 16.0);
    }

    public static VoxelShape quadrantShape(Direction facing, Direction side, double maxY) {
        double clampedMaxY = Math.max(0.0, Math.min(16.0, maxY));
        if (facing == Direction.NORTH) {
            return side == Direction.EAST
                    ? Block.box(8.0, 0.0, 0.0, 16.0, clampedMaxY, 8.0)
                    : Block.box(0.0, 0.0, 0.0, 8.0, clampedMaxY, 8.0);
        } else {
            return side == Direction.EAST
                    ? Block.box(8.0, 0.0, 8.0, 16.0, clampedMaxY, 16.0)
                    : Block.box(0.0, 0.0, 8.0, 8.0, clampedMaxY, 16.0);
        }
    }

    // ── Rendering ────────────────────────────────────────────────────────────

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
        BlockState slabState = bitsandbalance$getStoredSlab(level, pos, state);
        DynamicVariantMaterialHelper.CopperUseResult result = DynamicVariantMaterialHelper.getCopperUseResult(stack, slabState);
        if (result == null) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        VerticalStepBlock ownerBlock = bitsandbalance$ownerBlockForSingle(result.slabState());
        BlockState newState = ownerBlock.defaultBlockState()
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED))
                .setValue(FACING, state.getValue(FACING))
                .setValue(SIDE, state.getValue(SIDE));
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        BlockEntity updatedBe = level.getBlockEntity(pos);
        if (updatedBe instanceof VerticalStepBlockEntity verticalStepBlockEntity) {
            verticalStepBlockEntity.setSlabState(result.slabState());
        }
        if (ownerBlock instanceof FixedVerticalStepBlock) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
        } else {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), result.slabState());
        }

        bitsandbalance$finishCopperUse(level, pos, player, hand, stack, result);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        if (this instanceof FixedVerticalStepBlock fixed) {
            return DynamicVariantMaterialHelper.isCopperRandomlyTicking(bitsandbalance$sourceRenderState(fixed.getSourceVerticalSlab()));
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

        VerticalStepBlock ownerBlock = bitsandbalance$ownerBlockForSingle(nextSlab);
        BlockState newState = ownerBlock.defaultBlockState()
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED))
                .setValue(FACING, state.getValue(FACING))
                .setValue(SIDE, state.getValue(SIDE));
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        BlockEntity updatedBe = level.getBlockEntity(pos);
        if (updatedBe instanceof VerticalStepBlockEntity verticalStepBlockEntity) {
            verticalStepBlockEntity.setSlabState(nextSlab);
        }
        if (ownerBlock instanceof FixedVerticalStepBlock) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
        } else {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), nextSlab);
        }
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VerticalStepBlockEntity vsbe && vsbe.getSlabState() != null) {
                level.levelEvent(player, 2001, pos, Block.getId(vsbe.getSlabState()));
                return;
            }
            BlockState stored = bitsandbalance$getStoredSlab(level, pos, state);
            if (stored != null) {
                level.levelEvent(player, 2001, pos, Block.getId(stored));
                return;
            }
            BlockState cached = EnhancedSlabParticleStateCache.peek(level, pos);
            if (cached != null) {
                level.levelEvent(player, 2001, pos, Block.getId(cached));
                return;
            }
        } catch (Throwable ignored) {
        }
        if (this instanceof FixedVerticalStepBlock fixed) {
            level.levelEvent(player, 2001, pos, Block.getId(bitsandbalance$sourceRenderState(fixed.getSourceVerticalSlab())));
            return;
        }
        super.spawnDestroyParticles(level, player, pos, state);
    }

    // ── Block entity ─────────────────────────────────────────────────────────

    // ── Waterlogging ─────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir,
                                     BlockState neighborState, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, dir, neighborState, level, pos, neighborPos);
    }

    // ── Placement ────────────────────────────────────────────────────────────

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
        Vec3 hit = ctx.getClickLocation();
        BlockPos pos = ctx.getClickedPos();
        Direction clickedFace = ctx.getClickedFace();
        Direction facing = bitsandbalance$getFacingForPlacement(pos, hit, clickedFace);
        Direction side = bitsandbalance$getSideForPlacement(pos, hit, clickedFace);

        return this.defaultBlockState()
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER)
                .setValue(FACING, facing)
                .setValue(SIDE, side);
    }

    // ── Pick block ────────────────────────────────────────────────────────────

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VerticalStepBlockEntity vsbe && vsbe.getSlabState() != null) {
            return bitsandbalance$makeVerticalStepDropItem(vsbe.getSlabState());
        }
        if (this instanceof FixedVerticalStepBlock fixed) {
            return bitsandbalance$makeVerticalStepDropItem(bitsandbalance$sourceRenderState(fixed.getSourceVerticalSlab()));
        }
        return ItemStack.EMPTY;
    }

    // ── Mining speed ──────────────────────────────────────────────────────────

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (this instanceof FixedVerticalStepBlock fixed) {
            return bitsandbalance$computeDestroyProgress(
                    bitsandbalance$sourceRenderState(fixed.getSourceVerticalSlab()), player, level, pos);
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VerticalStepBlockEntity vsbe && vsbe.getSlabState() != null) {
            return bitsandbalance$computeDestroyProgress(vsbe.getSlabState(), player, level, pos);
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
        if (be instanceof VerticalStepBlockEntity vsbe && vsbe.getSlabState() != null) {
            ItemStack drop = bitsandbalance$makeVerticalStepDropItem(vsbe.getSlabState());
            if (!drop.isEmpty()) return List.of(drop);
        }
        BlockPos lootPos = bitsandbalance$lootPos(params);
        if (lootPos != null) {
            BlockState[] stored = EnhancedSlabMaterialSavedData.get(params.getLevel(), lootPos.asLong());
            BlockState slab = stored != null && stored.length > 0 ? stored[0] : null;
            if (slab != null) {
                ItemStack drop = bitsandbalance$makeVerticalStepDropItem(slab);
                if (!drop.isEmpty()) return List.of(drop);
            }
        }
        if (this instanceof FixedVerticalStepBlock fixed) {
            ItemStack drop = bitsandbalance$makeVerticalStepDropItem(bitsandbalance$sourceRenderState(fixed.getSourceVerticalSlab()));
            if (!drop.isEmpty()) return List.of(drop);
        }
        return Collections.emptyList();
    }

    private static @Nullable BlockPos bitsandbalance$lootPos(LootParams.Builder params) {
        Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
        return origin != null ? BlockPos.containing(origin) : null;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockState slabState = bitsandbalance$getStoredSlab(level, pos, state);
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
                ItemStack drop = bitsandbalance$makeVerticalStepDropItem(cached.minedSlab);
                if (!drop.isEmpty()) Block.popResource(level, pos, drop);
            }
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
        }

        CACHED_BREAK.remove();
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
    }

    // ── Drop helper ───────────────────────────────────────────────────────────

    /** Returns an {@link ItemStack} for the vertical step registered to the given vertical-slab state. */
    public static ItemStack bitsandbalance$makeVerticalStepDropItem(BlockState verticalSlabState) {
        if (verticalSlabState == null) return ItemStack.EMPTY;
        Block verticalSlabBlock = verticalSlabState.getBlock();

        // Support either a vertical-slab state or an underlying slab state.
        Block maybeVertical = VerticalSlabDynamicRegistry.getVerticalForSlab(verticalSlabBlock);
        if (maybeVertical != null) {
            verticalSlabBlock = maybeVertical;
        }

        Block vstepBlock = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlabBlock);

        ItemStack item;
        if (vstepBlock != null && vstepBlock.asItem() != Items.AIR) {
            item = new ItemStack(vstepBlock.asItem());
        } else {
            item = new ItemStack(verticalSlabBlock.asItem());
        }
        return item;
    }

    private static BlockState bitsandbalance$sourceRenderState(Block sourceVerticalSlab) {
        Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
        if (sourceSlab != null) return sourceSlab.defaultBlockState();
        return sourceVerticalSlab.defaultBlockState();
    }

    private static @Nullable BlockState bitsandbalance$getStoredSlab(BlockGetter level, BlockPos pos, BlockState ownerState) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof VerticalStepBlockEntity verticalStepBlockEntity && verticalStepBlockEntity.getSlabState() != null) {
            return DynamicVariantMaterialHelper.fallbackSlabState(verticalStepBlockEntity.getSlabState());
        }
        if (ownerState.getBlock() instanceof FixedVerticalStepBlock fixed) {
            return DynamicVariantMaterialHelper.fallbackSlabState(bitsandbalance$sourceRenderState(fixed.getSourceVerticalSlab()));
        }
        if (level instanceof Level actualLevel) {
            BlockState[] stored = EnhancedSlabMaterialSavedData.get(actualLevel, pos.asLong());
            if (stored != null && stored.length > 0 && stored[0] != null) {
                return DynamicVariantMaterialHelper.fallbackSlabState(stored[0]);
            }
        }
        Block sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(ownerState.getBlock());
        if (sourceVerticalSlab != null) {
            return DynamicVariantMaterialHelper.fallbackSlabState(bitsandbalance$sourceRenderState(sourceVerticalSlab));
        }
        return null;
    }

    private static double bitsandbalance$uprightHeight(BlockGetter level, BlockPos pos, BlockState ownerState) {
        return 8.0 + bitsandbalance$sourceThickness(level, pos, ownerState);
    }

    private static double bitsandbalance$sourceThickness(BlockGetter level, BlockPos pos, BlockState ownerState) {
        BlockState slabState = bitsandbalance$getStoredSlab(level, pos, ownerState);
        if (slabState == null) {
            return 8.0;
        }

        ResourceLocation slabId = BuiltInRegistries.BLOCK.getKey(slabState.getBlock());
        if (bitsandbalance$VINERY_DIRT_PATH_SLAB_ID.equals(slabId)) {
            return 7.0;
        }

        return 8.0;
    }

    private static VerticalStepBlock bitsandbalance$ownerBlockForSingle(BlockState slabState) {
        Block verticalSlabBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabState.getBlock());
        if (verticalSlabBlock == null) {
            return CommonBlocks.VERTICAL_STEP;
        }
        Block verticalStepBlock = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlabBlock);
        if (verticalStepBlock instanceof VerticalStepBlock verticalStep) {
            return verticalStep;
        }
        return CommonBlocks.VERTICAL_STEP;
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

    // ── Inner cache ───────────────────────────────────────────────────────────

    private static final class CachedBreak {
        long posLong;
        @Nullable BlockState minedSlab;
    }
}
