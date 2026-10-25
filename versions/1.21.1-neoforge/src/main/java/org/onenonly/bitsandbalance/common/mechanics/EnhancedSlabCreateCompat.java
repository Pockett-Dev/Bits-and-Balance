package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Matrix3d;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.SlabStateTagHelper;
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
import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class EnhancedSlabCreateCompat {
    public static final String TAG_MATERIALS = "BitsAndBalanceEnhancedSlabMaterials";
    private static final String TAG_SLOT_PREFIX = "Slot";
    private static final int MAX_SLOTS = 4;
    private static final String[] WRAPPED_LEVEL_FIELDS = { "wrapped", "level", "world", "delegate" };
    private static final Map<Object, Map<Long, BlockState[]>> CLIENT_RENDER_LEVEL_SLOTS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Map<Long, Long>> CLIENT_RENDER_LEVEL_WORLD_POS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Map<SableRapierColliderKey, Object>> SABLE_RAPIER_COLLIDERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile java.lang.reflect.Method sableBlockMassMethod;
    private static volatile boolean sableBlockMassMethodInitialized;
    private static final Map<Long, Integer> PENDING_CLIENT_CONTRAPTION_INVALIDATIONS = new HashMap<>();
    private static final Map<NoArgMethodKey, java.lang.reflect.Method> NO_ARG_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<FieldKey, java.lang.reflect.Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final int CLIENT_CONTRAPTION_INVALIDATION_RETRY_TICKS = 80;
    private static boolean simulatedMovementRegistered;

    private EnhancedSlabCreateCompat() {
    }

    public static boolean supports(@Nullable BlockState state) {
        if (state == null) return false;
        return state.getBlock() instanceof StepBlock
                || state.getBlock() instanceof VerticalSlabBlock
                || state.getBlock() instanceof VerticalStepBlock
                || state.getBlock() instanceof MixedSlabBlock
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock
                || EnhancedSlabPistonHooks.supports(state);
    }

    public static boolean supportsMovement(@Nullable BlockState state) {
        if (state == null) return false;
        return supports(state);
    }

    public static @Nullable CompoundTag withCapturedMaterials(Level level, BlockPos pos, BlockState state, @Nullable CompoundTag existingTag) {
        BlockState[] slots = captureSlots(level, pos, state);
        if (isEmpty(slots)) return existingTag;

        CompoundTag tag = existingTag == null ? new CompoundTag() : existingTag.copy();
        tag.put(TAG_MATERIALS, saveSlots(slots));
        return tag;
    }

    public static @Nullable BlockState[] captureMaterialSlots(Level level, BlockPos pos, BlockState state) {
        BlockState[] slots = captureSlots(level, pos, state);
        return isEmpty(slots) ? null : copySlots(slots);
    }

    public static void restoreMaterialSlots(Level level, BlockPos pos, BlockState state, @Nullable BlockState[] slots) {
        if (level == null || level.isClientSide() || pos == null || !supports(state) || isEmpty(slots)) return;
        if (SableCompat.rememberPlotEnhancedSlabMaterials(level, pos, state, slots, Block.UPDATE_ALL)) {
            return;
        }
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), slots);
    }

    public static @Nullable BlockState[] transformMovedSlots(@Nullable BlockState sourceState,
                                                             @Nullable BlockState targetState,
                                                             @Nullable BlockState[] slots,
                                                             @Nullable Object transform) {
        if (isEmpty(slots) || sourceState == null || targetState == null) {
            return slots;
        }

        Rotation rotation = extractRotation(transform);
        if (rotation == null || rotation == Rotation.NONE) {
            return slots;
        }

        if (sourceState.getBlock() instanceof QuadStepBlock
                && targetState.getBlock() instanceof QuadStepBlock
                && sourceState.hasProperty(QuadStepBlock.AXIS)
                && targetState.hasProperty(QuadStepBlock.AXIS)) {
            return rotateQuadStepSlots(sourceState, targetState, slots, rotation);
        }
        if (sourceState.getBlock() instanceof QuadVerticalStepBlock
                && targetState.getBlock() instanceof QuadVerticalStepBlock) {
            return rotateQuadVerticalStepSlots(slots, rotation);
        }
        return slots;
    }

    public static boolean hasMaterialSlots(@Nullable BlockState[] slots) {
        return !isEmpty(slots);
    }

    public static boolean isAttachedTowards(@Nullable BlockState state, @Nullable Level level, @Nullable BlockPos pos, @Nullable Direction direction) {
        if (state == null || level == null || pos == null || direction == null || !supportsMovement(state)) {
            return false;
        }

        if (!touchesOccupiedFace(state, level, pos, direction)) {
            return false;
        }

        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        return !supportsMovement(neighborState) || touchesOccupiedFace(neighborState, level, neighborPos, direction.getOpposite());
    }

    public static boolean isAttachedTowardsOffset(@Nullable BlockState state, @Nullable Level level, @Nullable BlockPos pos, @Nullable BlockPos offset) {
        if (state == null || level == null || pos == null || offset == null || !supportsMovement(state)) {
            return false;
        }

        BlockPos normalizedOffset = normalizeAttachmentOffset(offset);
        if (normalizedOffset == null) {
            return false;
        }

        Direction direction = directionFromOffset(normalizedOffset);
        if (direction != null) {
            return isAttachedTowards(state, level, pos, direction);
        }

        VoxelShape shape = occupiedShape(state, level, pos);
        if (shape.isEmpty()) {
            return false;
        }

        BlockPos neighborPos = pos.offset(normalizedOffset);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.isAir()) {
            return false;
        }

        VoxelShape neighborShape = occupiedShape(neighborState, level, neighborPos);
        if (neighborShape.isEmpty()) {
            return false;
        }

        for (AABB sourceBox : shape.toAabbs()) {
            for (AABB targetBox : neighborShape.toAabbs()) {
                if (touchesConnection(sourceBox, targetBox, normalizedOffset)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void restoreMovedSlots(Level level, BlockPos pos, BlockState state, @Nullable CompoundTag blockTag) {
        BlockState[] slots = loadSlots(blockTag);
        restoreMaterialSlots(level, pos, state, slots);
    }

    public static void rememberRenderSlots(Object renderLevel, BlockPos localPos, BlockState state, @Nullable CompoundTag blockTag) {
        rememberRenderSlots(renderLevel, localPos, state, blockTag, null);
    }

    public static void rememberRenderSlots(Object renderLevel,
                                           BlockPos localPos,
                                           BlockState state,
                                           @Nullable CompoundTag blockTag,
                                           @Nullable BlockState[] fallbackSlots) {
        if (renderLevel == null || localPos == null || !supports(state)) return;
        BlockState[] slots = loadSlots(blockTag);
        if (isEmpty(slots) && !isEmpty(fallbackSlots)) {
            slots = fallbackSlots;
        }
        if (isEmpty(slots)) return;
        rememberRenderSlots(renderLevel, localPos, slots);
    }

    public static void seedRenderSlots(Object renderLevel, Object contraption) {
        if (renderLevel == null || contraption == null) return;

        try {
            Object blocksObject = invokeNoArg(contraption, "getBlocks");
            if (!(blocksObject instanceof Map<?, ?> blocks)) {
                blocksObject = findFieldValue(contraption, "blocks");
            }
            if (!(blocksObject instanceof Map<?, ?> blocks)) return;

            Map<?, ?> updateTags = findMapFieldValue(contraption, "updateTags");
            for (Map.Entry<?, ?> entry : blocks.entrySet()) {
                Object value = entry.getValue();
                if (!(value instanceof StructureBlockInfo info)) continue;
                BlockPos localPos = info.pos();
                rememberRenderWorldPos(renderLevel, contraption, localPos);
                CompoundTag blockTag = renderTag(info.nbt(), tagFor(updateTags, localPos));
                rememberRenderSlots(
                        renderLevel,
                        localPos,
                        info.state(),
                        blockTag,
                        clientFallbackSlots(contraption, localPos)
                );
                if (entry.getKey() instanceof BlockPos keyPos && !keyPos.equals(localPos)) {
                    rememberRenderWorldPos(renderLevel, contraption, keyPos);
                    CompoundTag keyTag = renderTag(info.nbt(), tagFor(updateTags, keyPos));
                    rememberRenderSlots(
                            renderLevel,
                            keyPos,
                            info.state(),
                            keyTag,
                            clientFallbackSlots(contraption, keyPos)
                    );
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    public static @Nullable BlockState[] getRenderSlots(Object level, BlockPos pos) {
        if (level == null || pos == null) return null;

        BlockState[] sableSlots = SableCompat.enhancedSlabMaterials(level, pos);
        if (!isEmpty(sableSlots)) {
            return copySlots(sableSlots);
        }

        Object current = level;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            Map<Long, BlockState[]> byPos = CLIENT_RENDER_LEVEL_SLOTS.get(current);
            if (byPos != null) {
                BlockState[] slots = byPos.get(pos.asLong());
                if (slots != null) {
                    return copySlots(slots);
                }
            }
            BlockState[] mappedSlots = mappedClientSlots(current, pos);
            if (!isEmpty(mappedSlots)) {
                return mappedSlots;
            }

            Object wrapped = unwrapWrappedLevel(current);
            if (wrapped == null || wrapped == current) {
                break;
            }
            current = wrapped;
        }
        BlockState[] seededSlots = findSeededRenderSlots(pos);
        if (!isEmpty(seededSlots)) {
            return seededSlots;
        }
        return findMappedClientSlots(pos);
    }

    public static @Nullable BlockState[] getContraptionSlots(Object level, BlockPos pos, @Nullable BlockState state) {
        if (level == null || pos == null || !supports(state)) return null;

        BlockState[] sableSlots = SableCompat.enhancedSlabMaterials(level, pos);
        if (!isEmpty(sableSlots)) {
            return copySlots(sableSlots);
        }

        BlockState[] slots = contraptionSlots(level, pos, state, new java.util.IdentityHashMap<>());
        if (!isEmpty(slots)) {
            return copySlots(slots);
        }

        if (isCreateLike(level)) {
            slots = getRenderSlots(level, pos);
            if (!isEmpty(slots)) {
                return copySlots(slots);
            }
        }
        return null;
    }

    public static VoxelShape getContraptionCollisionShape(Object contraption,
                                                          BlockGetter level,
                                                          BlockPos pos,
                                                          @Nullable BlockState state) {
        if (pos == null || state == null) return Shapes.empty();

        BlockState[] slots = getContraptionSlots(contraption, pos, state);
        if (isEmpty(slots)) {
            return Shapes.empty();
        }

        if (state.getBlock() instanceof QuadStepBlock) {
            Direction.Axis axis = state.hasProperty(QuadStepBlock.AXIS)
                    ? state.getValue(QuadStepBlock.AXIS)
                    : Direction.Axis.X;
            return QuadStepBlock.bitsandbalance$occupiedShape(axis, slots, level, pos);
        }
        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            return QuadVerticalStepBlock.bitsandbalance$occupiedShape(slots, level, pos);
        }
        return Shapes.empty();
    }

    /**
     * Sable normally memoizes Rapier voxel colliders by {@link BlockState}. Enhanced-slab geometry,
     * especially quad-step occupancy, also depends on position data, so build and cache a collider
     * from the actual plot-position shape instead.
     */
    public static Object createSableRapierCollider(Object bakery,
                                                   @Nullable Object fallback,
                                                   BlockGetter level,
                                                   BlockPos pos,
                                                   @Nullable BlockState state) {
        if (bakery == null || level == null || pos == null || state == null || !supports(state)) {
            return fallback;
        }

        VoxelShape shape = getContraptionCollisionShape(level, level, pos, state);
        if (shape.isEmpty()) {
            try {
                shape = state.getCollisionShape(level, pos, CollisionContext.empty());
            } catch (RuntimeException ignored) {
                shape = Shapes.empty();
            }
        }
        if (shape.isEmpty()) {
            return fallback;
        }

        List<SableRapierBox> boxes = shape.toAabbs().stream()
                .map(SableRapierBox::new)
                .toList();
        if (boxes.isEmpty()) {
            return fallback;
        }

        SableRapierColliderKey key = new SableRapierColliderKey(state, boxes);
        synchronized (SABLE_RAPIER_COLLIDERS) {
            Map<SableRapierColliderKey, Object> colliders = SABLE_RAPIER_COLLIDERS.computeIfAbsent(
                    bakery,
                    ignored -> new HashMap<>()
            );
            Object cached = colliders.get(key);
            if (cached != null) {
                recordSableRapierColliderUse(cached, boxes.size());
                return cached;
            }

            Object built = buildSableRapierCollider(bakery, state, boxes);
            if (built != null) {
                colliders.put(key, built);
                recordSableRapierColliderUse(built, boxes.size());
                return built;
            }
        }
        return fallback;
    }

    private static @Nullable Object buildSableRapierCollider(Object bakery,
                                                              BlockState state,
                                                              List<SableRapierBox> boxes) {
        try {
            java.lang.reflect.Method build = bakery.getClass().getDeclaredMethod(
                    "buildPhysicsDataForBlock",
                    BlockState.class
            );
            build.setAccessible(true);
            Object collider = build.invoke(bakery, state);
            if (!sableRapierColliderHandleValid(collider)) {
                collider = createSableRapierColliderEntry(bakery, state);
            }
            if (!sableRapierColliderHandleValid(collider)) {
                return null;
            }

            java.lang.reflect.Method clearBoxes = collider.getClass().getMethod("clearBoxes");
            java.lang.reflect.Method addBox = collider.getClass().getMethod(
                    "addBox",
                    Vector3dc.class,
                    Vector3dc.class
            );
            clearBoxes.invoke(collider);
            for (SableRapierBox box : boxes) {
                addBox.invoke(
                        collider,
                        new Vector3d(box.minX(), box.minY(), box.minZ()),
                        new Vector3d(box.maxX(), box.maxY(), box.maxZ())
                );
            }
            return collider;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    /** Records every position-aware collider returned to Sable, including cache hits. */
    private static void recordSableRapierColliderUse(Object collider, int boxCount) {
        if (collider == null || boxCount <= 0) {
            return;
        }
        try {
            java.lang.reflect.Method handle = collider.getClass().getMethod("handle");
            Object rawHandle = handle.invoke(collider);
            if (rawHandle instanceof Number number) {
                SableCompat.recordPositionAwareRapierCollider(number.intValue(), boxCount);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    private static boolean sableRapierColliderHandleValid(@Nullable Object collider) {
        if (collider == null) {
            return false;
        }
        try {
            java.lang.reflect.Method handle = collider.getClass().getMethod("handle");
            Object value = handle.invoke(collider);
            return value instanceof Number number && number.intValue() >= 0;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    /**
     * Allocates the native Rapier entry which Sable's bakery discards when a position-backed
     * collision shape looks empty at BlockPos.ZERO. Both supported Sable API lines expose the
     * same property helpers and allocation method; reflection keeps Sable an optional dependency.
     */
    private static @Nullable Object createSableRapierColliderEntry(Object bakery, BlockState state)
            throws ReflectiveOperationException {
        ClassLoader loader = bakery.getClass().getClassLoader();
        Class<?> properties = Class.forName(
                "dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper",
                false,
                loader
        );
        double friction = ((Number) properties.getMethod("getFriction", BlockState.class)
                .invoke(null, state)).doubleValue();
        double volume = ((Number) properties.getMethod("getVolume", BlockState.class)
                .invoke(null, state)).doubleValue();
        double restitution = ((Number) properties.getMethod("getRestitution", BlockState.class)
                .invoke(null, state)).doubleValue();

        Class<?> voxelNeighborhood = Class.forName(
                "dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState",
                false,
                loader
        );
        boolean liquid = (Boolean) voxelNeighborhood.getMethod("isLiquid", BlockState.class)
                .invoke(null, state);

        Class<?> callbackOwner = Class.forName(
                "dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback",
                false,
                loader
        );
        Object callback = callbackOwner.getMethod("sable$getCallback", BlockState.class)
                .invoke(null, state);

        Class<?> rapier = Class.forName(
                "dev.ryanhcode.sable.physics.impl.rapier.Rapier3D",
                false,
                loader
        );
        for (java.lang.reflect.Method method : rapier.getMethods()) {
            if (method.getName().equals("createVoxelColliderEntry") && method.getParameterCount() == 5) {
                return method.invoke(null, friction, volume, restitution, liquid, callback);
            }
        }
        throw new NoSuchMethodException("Rapier3D.createVoxelColliderEntry");
    }

    public static @Nullable Vector3dc getSableCenterOfMass(@Nullable BlockGetter level, @Nullable BlockState state) {
        if (state == null || !supports(state)) {
            return null;
        }

        VoxelShape shape = sableMassShape(level, state);
        if (shape.isEmpty()) {
            return new Vector3d(0.5D, 0.5D, 0.5D);
        }

        try {
            AABB bounds = shape.bounds().intersect(new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D));
            if (bounds.getXsize() <= 0.0D || bounds.getYsize() <= 0.0D || bounds.getZsize() <= 0.0D) {
                return new Vector3d(0.5D, 0.5D, 0.5D);
            }
            Vec3 center = bounds.getCenter();
            return new Vector3d(center.x, center.y, center.z);
        } catch (RuntimeException ignored) {
            return new Vector3d(0.5D, 0.5D, 0.5D);
        }
    }

    /**
     * Resolves the center of mass for a storage-backed Quad Step at its real plot position.
     *
     * <p>Sable's normal center lookup is memoized only by {@link BlockState}, while occupied quad
     * slots and their source materials live in position-backed saved data. Resolve each occupied
     * quarter independently and weight its center by the same source-slab mass used by the Step
     * mass compatibility hook. Returning {@code null} preserves Sable's original state-only path
     * for every non-quad block and for any unsupported reflective API.</p>
     */
    public static @Nullable Vector3dc getSablePositionAwareQuadCenterOfMass(@Nullable BlockGetter level,
                                                                            @Nullable BlockPos pos,
                                                                            @Nullable BlockState state) {
        if (level == null || pos == null || state == null
                || (!(state.getBlock() instanceof QuadStepBlock)
                && !(state.getBlock() instanceof QuadVerticalStepBlock))) {
            return null;
        }

        BlockState[] slots = getContraptionSlots(level, pos, state);
        if (isEmpty(slots)) {
            return null;
        }

        Vector3d weightedCenter = new Vector3d();
        double totalMass = 0.0D;
        for (int index = 0; index < Math.min(MAX_SLOTS, slots.length); index++) {
            BlockState sourceSlab = slots[index];
            if (sourceSlab == null || sourceSlab.isAir()) {
                continue;
            }

            VoxelShape slotShape = sableQuadSlotShape(level, pos, state, index, sourceSlab);
            if (slotShape.isEmpty()) {
                return null;
            }

            double sourceMass = sableBlockMass(level, pos, sourceSlab);
            if (!Double.isFinite(sourceMass) || sourceMass < 0.0D) {
                return null;
            }
            double stepMass = sourceMass * 0.5D;
            if (stepMass <= 0.0D) {
                continue;
            }

            try {
                Vec3 center = slotShape.bounds().getCenter();
                weightedCenter.fma(stepMass, new Vector3d(center.x, center.y, center.z));
                totalMass += stepMass;
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        return totalMass > 0.0D && Double.isFinite(totalMass)
                ? weightedCenter.div(totalMass)
                : null;
    }

    /**
     * Adds the exact occupied Step cuboids to Sable's body-space inertia accumulator.
     *
     * <p>The supplied offset is the owner block's center of mass relative to the whole body's
     * center. Each occupied material slot is split into its actual AABB cuboids, assigned the same
     * half-slab mass used by the mass hook, and accumulated with both cuboid intrinsic inertia and
     * the parallel-axis term. No accumulator state is changed unless every required value can be
     * resolved and its summed mass agrees with Sable's owner mass.</p>
     */
    public static boolean addSablePositionAwareStepInertia(@Nullable BlockGetter level,
                                                            @Nullable BlockPos pos,
                                                            @Nullable BlockState state,
                                                            @Nullable Vector3dc ownerToBodyCenterOffset,
                                                            double ownerMass,
                                                            @Nullable Matrix3d accumulator) {
        if (level == null || pos == null || state == null || ownerToBodyCenterOffset == null
                || accumulator == null || !(ownerMass > 0.0D) || !Double.isFinite(ownerMass)) {
            return false;
        }

        Block block = state.getBlock();
        boolean horizontalQuad = block instanceof QuadStepBlock;
        boolean verticalQuad = block instanceof QuadVerticalStepBlock;
        boolean singleStep = block instanceof StepBlock;
        boolean singleVerticalStep = block instanceof VerticalStepBlock;
        if (!horizontalQuad && !verticalQuad && !singleStep && !singleVerticalStep) {
            return false;
        }

        BlockState[] sourceSlabs = getContraptionSlots(level, pos, state);
        if (isEmpty(sourceSlabs)) {
            if (block instanceof FixedStepBlock fixedStep) {
                sourceSlabs = new BlockState[]{fixedStep.getSourceSlab().defaultBlockState()};
            } else if (block instanceof FixedVerticalStepBlock fixedVerticalStep) {
                Block sourceVerticalSlab = fixedVerticalStep.getSourceVerticalSlab();
                Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
                sourceSlabs = new BlockState[]{
                        (sourceSlab != null ? sourceSlab : sourceVerticalSlab).defaultBlockState()
                };
            } else {
                return false;
            }
        }

        List<SableMassBox> massBoxes = new ArrayList<>();
        for (int index = 0; index < Math.min(MAX_SLOTS, sourceSlabs.length); index++) {
            BlockState sourceSlab = sourceSlabs[index];
            if (sourceSlab == null || sourceSlab.isAir()) {
                continue;
            }
            double slotMass = sableBlockMass(level, pos, sourceSlab) * 0.5D;
            if (!Double.isFinite(slotMass) || slotMass < 0.0D) {
                return false;
            }
            if (slotMass == 0.0D) {
                continue;
            }

            VoxelShape slotShape = horizontalQuad || verticalQuad
                    ? sableQuadSlotShape(level, pos, state, index, sourceSlab)
                    : sableMassShape(level, state);
            List<AABB> boxes = slotShape.toAabbs();
            if (boxes.isEmpty()) {
                return false;
            }
            double totalVolume = 0.0D;
            for (AABB boxShape : boxes) {
                double volume = boxShape.getXsize() * boxShape.getYsize() * boxShape.getZsize();
                if (!(volume > 0.0D) || !Double.isFinite(volume)) {
                    return false;
                }
                totalVolume += volume;
            }
            if (!(totalVolume > 0.0D) || !Double.isFinite(totalVolume)) {
                return false;
            }
            for (AABB boxShape : boxes) {
                double volume = boxShape.getXsize() * boxShape.getYsize() * boxShape.getZsize();
                massBoxes.add(new SableMassBox(boxShape, slotMass * volume / totalVolume));
            }
        }
        if (massBoxes.isEmpty()) {
            return false;
        }

        double resolvedMass = 0.0D;
        Vector3d ownerCenter = new Vector3d();
        for (SableMassBox massBox : massBoxes) {
            Vec3 center = massBox.box().getCenter();
            ownerCenter.fma(massBox.mass(), new Vector3d(center.x, center.y, center.z));
            resolvedMass += massBox.mass();
        }
        double massTolerance = Math.max(1.0E-6D, ownerMass * 1.0E-6D);
        if (!(resolvedMass > 0.0D) || !Double.isFinite(resolvedMass)
                || Math.abs(resolvedMass - ownerMass) > massTolerance) {
            return false;
        }
        ownerCenter.div(resolvedMass);

        double ixx = 0.0D;
        double iyy = 0.0D;
        double izz = 0.0D;
        double ixy = 0.0D;
        double ixz = 0.0D;
        double iyz = 0.0D;
        for (SableMassBox massBox : massBoxes) {
            AABB boxShape = massBox.box();
            double boxMass = massBox.mass();
            Vec3 center = boxShape.getCenter();
            double rx = ownerToBodyCenterOffset.x() + center.x - ownerCenter.x;
            double ry = ownerToBodyCenterOffset.y() + center.y - ownerCenter.y;
            double rz = ownerToBodyCenterOffset.z() + center.z - ownerCenter.z;
            double dx = boxShape.getXsize();
            double dy = boxShape.getYsize();
            double dz = boxShape.getZsize();

            ixx += boxMass * ((dy * dy + dz * dz) / 12.0D + ry * ry + rz * rz);
            iyy += boxMass * ((dx * dx + dz * dz) / 12.0D + rx * rx + rz * rz);
            izz += boxMass * ((dx * dx + dy * dy) / 12.0D + rx * rx + ry * ry);
            ixy -= boxMass * rx * ry;
            ixz -= boxMass * rx * rz;
            iyz -= boxMass * ry * rz;
        }
        if (!Double.isFinite(ixx) || !Double.isFinite(iyy) || !Double.isFinite(izz)
                || !Double.isFinite(ixy) || !Double.isFinite(ixz) || !Double.isFinite(iyz)) {
            return false;
        }

        accumulator.m00(accumulator.m00() + ixx);
        accumulator.m11(accumulator.m11() + iyy);
        accumulator.m22(accumulator.m22() + izz);
        accumulator.m01(accumulator.m01() + ixy);
        accumulator.m10(accumulator.m10() + ixy);
        accumulator.m02(accumulator.m02() + ixz);
        accumulator.m20(accumulator.m20() + ixz);
        accumulator.m12(accumulator.m12() + iyz);
        accumulator.m21(accumulator.m21() + iyz);
        return true;
    }

    private static VoxelShape sableQuadSlotShape(BlockGetter level,
                                                  BlockPos pos,
                                                  BlockState ownerState,
                                                  int occupiedIndex,
                                                  BlockState sourceSlab) {
        BlockState[] isolatedSlot = new BlockState[MAX_SLOTS];
        isolatedSlot[occupiedIndex] = sourceSlab;
        if (ownerState.getBlock() instanceof QuadStepBlock) {
            Direction.Axis axis = ownerState.hasProperty(QuadStepBlock.AXIS)
                    ? ownerState.getValue(QuadStepBlock.AXIS)
                    : Direction.Axis.X;
            return QuadStepBlock.bitsandbalance$occupiedShape(axis, isolatedSlot, level, pos);
        }
        return QuadVerticalStepBlock.bitsandbalance$occupiedShape(isolatedSlot, level, pos);
    }

    private static double sableBlockMass(BlockGetter level, BlockPos pos, BlockState state) {
        java.lang.reflect.Method method = sableBlockMassMethod();
        if (method == null) {
            return Double.NaN;
        }
        try {
            Object result = method.invoke(null, level, pos, state);
            return result instanceof Number number ? number.doubleValue() : Double.NaN;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return Double.NaN;
        }
    }

    private static @Nullable java.lang.reflect.Method sableBlockMassMethod() {
        if (sableBlockMassMethodInitialized) {
            return sableBlockMassMethod;
        }
        synchronized (EnhancedSlabCreateCompat.class) {
            if (sableBlockMassMethodInitialized) {
                return sableBlockMassMethod;
            }
            try {
                Class<?> helper = Class.forName(
                        "dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper",
                        false,
                        EnhancedSlabCreateCompat.class.getClassLoader()
                );
                sableBlockMassMethod = helper.getMethod(
                        "getMass",
                        BlockGetter.class,
                        BlockPos.class,
                        BlockState.class
                );
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                sableBlockMassMethod = null;
            }
            sableBlockMassMethodInitialized = true;
            return sableBlockMassMethod;
        }
    }

    private static VoxelShape sableMassShape(@Nullable BlockGetter level, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof StepBlock && state.hasProperty(StepBlock.FACING) && state.hasProperty(StepBlock.TOP)) {
            return StepBlock.quadrantShape(state.getValue(StepBlock.FACING), state.getValue(StepBlock.TOP));
        }
        if (block instanceof VerticalStepBlock
                && state.hasProperty(VerticalStepBlock.FACING)
                && state.hasProperty(VerticalStepBlock.SIDE)) {
            return VerticalStepBlock.quadrantShape(state.getValue(VerticalStepBlock.FACING), state.getValue(VerticalStepBlock.SIDE));
        }
        if (block instanceof VerticalSlabBlock && state.hasProperty(VerticalSlabBlock.FACING)) {
            return switch (state.getValue(VerticalSlabBlock.FACING)) {
                case NORTH -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 8.0D);
                case SOUTH -> Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D);
                case WEST -> Block.box(0.0D, 0.0D, 0.0D, 8.0D, 16.0D, 16.0D);
                case EAST -> Block.box(8.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
                default -> Shapes.block();
            };
        }
        if (block instanceof QuadStepBlock || block instanceof QuadVerticalStepBlock || block instanceof MixedSlabBlock) {
            return Shapes.block();
        }
        try {
            return state.getCollisionShape(level == null ? EmptyBlockGetter.INSTANCE : level, BlockPos.ZERO);
        } catch (RuntimeException ignored) {
            return Shapes.block();
        }
    }

    private static boolean touchesFace(VoxelShape shape, Direction direction) {
        if (shape == null || shape.isEmpty()) {
            return false;
        }
        return Shapes.joinIsNotEmpty(shape, faceSlice(direction), BooleanOp.AND);
    }

    private static boolean touchesOccupiedFace(BlockState state, Level level, BlockPos pos, Direction direction) {
        VoxelShape shape = getContraptionCollisionShape(level, level, pos, state);
        if (shape.isEmpty()) {
            shape = state.getCollisionShape(level, pos);
        }
        if (shape.isEmpty()) {
            shape = state.getBlockSupportShape(level, pos);
        }
        return touchesFace(shape, direction);
    }

    private static VoxelShape faceSlice(Direction direction) {
        double thickness = 1.0E-4D;
        AABB box = switch (direction) {
            case DOWN -> new AABB(0.0D, 0.0D, 0.0D, 1.0D, thickness, 1.0D);
            case UP -> new AABB(0.0D, 1.0D - thickness, 0.0D, 1.0D, 1.0D, 1.0D);
            case NORTH -> new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, thickness);
            case SOUTH -> new AABB(0.0D, 0.0D, 1.0D - thickness, 1.0D, 1.0D, 1.0D);
            case WEST -> new AABB(0.0D, 0.0D, 0.0D, thickness, 1.0D, 1.0D);
            case EAST -> new AABB(1.0D - thickness, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        };
        return Shapes.create(box);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static synchronized void registerSimulatedMovementChecks() {
        if (simulatedMovementRegistered) {
            return;
        }

        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> checksClass = Class.forName("dev.simulated_team.simulated.index.SimBlockMovementChecks", false, loader);
            Class<?> attachedCheckClass = Class.forName("dev.simulated_team.simulated.index.SimBlockMovementChecks$AttachedCheck", false, loader);
            Class<?> resultClass = Class.forName("com.simibubi.create.api.contraption.BlockMovementChecks$CheckResult", false, loader);
            Object success = Enum.valueOf(resultClass.asSubclass(Enum.class), "SUCCESS");
            Object fail = Enum.valueOf(resultClass.asSubclass(Enum.class), "FAIL");
            Object pass = Enum.valueOf(resultClass.asSubclass(Enum.class), "PASS");

            Object attachedProxy = java.lang.reflect.Proxy.newProxyInstance(
                    loader,
                    new Class<?>[]{attachedCheckClass},
                    (ignoredProxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(EnhancedSlabCreateCompat.class, args);
                        }
                        if (args == null
                                || args.length != 4
                                || !(args[0] instanceof BlockState state)
                                || !(args[1] instanceof Level level)
                                || !(args[2] instanceof BlockPos pos)
                                || !(args[3] instanceof BlockPos offset)
                                || !supportsMovement(state)) {
                            return pass;
                        }
                        return isAttachedTowardsOffset(state, level, pos, offset) ? success : fail;
                    }
            );

            checksClass.getMethod("registerAttachedCheck", attachedCheckClass).invoke(null, attachedProxy);
            simulatedMovementRegistered = true;
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    private static @Nullable Direction directionFromOffset(BlockPos offset) {
        return directionFromOffset((Vec3i) offset);
    }

    private static @Nullable Direction directionFromOffset(Vec3i offset) {
        int x = Integer.compare(offset.getX(), 0);
        int y = Integer.compare(offset.getY(), 0);
        int z = Integer.compare(offset.getZ(), 0);
        int axes = Math.abs(x) + Math.abs(y) + Math.abs(z);
        if (axes != 1) {
            return null;
        }
        if (x > 0) return Direction.EAST;
        if (x < 0) return Direction.WEST;
        if (y > 0) return Direction.UP;
        if (y < 0) return Direction.DOWN;
        if (z > 0) return Direction.SOUTH;
        return Direction.NORTH;
    }

    private static @Nullable BlockPos normalizeAttachmentOffset(BlockPos offset) {
        int x = Integer.compare(offset.getX(), 0);
        int y = Integer.compare(offset.getY(), 0);
        int z = Integer.compare(offset.getZ(), 0);
        int axes = Math.abs(x) + Math.abs(y) + Math.abs(z);
        if (axes != 1 && axes != 2) {
            return null;
        }
        return new BlockPos(x, y, z);
    }

    private static VoxelShape occupiedShape(BlockState state, Level level, BlockPos pos) {
        VoxelShape shape = getContraptionCollisionShape(level, level, pos, state);
        if (shape.isEmpty()) {
            shape = state.getCollisionShape(level, pos);
        }
        if (shape.isEmpty()) {
            shape = state.getBlockSupportShape(level, pos);
        }
        return shape;
    }

    private static boolean touchesConnection(AABB sourceBox, AABB targetBox, Vec3i offset) {
        int axes = Math.abs(offset.getX()) + Math.abs(offset.getY()) + Math.abs(offset.getZ());
        if (axes == 1) {
            Direction direction = directionFromOffset(offset);
            return direction != null
                    && touchesFace(sourceBox, direction)
                    && touchesFace(targetBox, direction.getOpposite())
                    && overlapsOnSharedFace(sourceBox, targetBox, direction);
        }
        if (axes == 2) {
            return touchesEdge(sourceBox, targetBox, offset);
        }
        return false;
    }

    private static boolean touchesEdge(AABB sourceBox, AABB targetBox, Vec3i offset) {
        Direction xDirection = axisDirection(Direction.Axis.X, offset.getX());
        Direction yDirection = axisDirection(Direction.Axis.Y, offset.getY());
        Direction zDirection = axisDirection(Direction.Axis.Z, offset.getZ());
        if (xDirection != null
                && (!touchesFace(sourceBox, xDirection) || !touchesFace(targetBox, xDirection.getOpposite()))) {
            return false;
        }
        if (yDirection != null
                && (!touchesFace(sourceBox, yDirection) || !touchesFace(targetBox, yDirection.getOpposite()))) {
            return false;
        }
        if (zDirection != null
                && (!touchesFace(sourceBox, zDirection) || !touchesFace(targetBox, zDirection.getOpposite()))) {
            return false;
        }

        double epsilon = 1.0E-5D;
        if (offset.getX() == 0) {
            return overlaps(sourceBox.minX, sourceBox.maxX, targetBox.minX, targetBox.maxX, epsilon);
        }
        if (offset.getY() == 0) {
            return overlaps(sourceBox.minY, sourceBox.maxY, targetBox.minY, targetBox.maxY, epsilon);
        }
        return overlaps(sourceBox.minZ, sourceBox.maxZ, targetBox.minZ, targetBox.maxZ, epsilon);
    }

    private static Direction axisDirection(Direction.Axis axis, int value) {
        if (value > 0) {
            return switch (axis) {
                case X -> Direction.EAST;
                case Y -> Direction.UP;
                case Z -> Direction.SOUTH;
            };
        }
        if (value < 0) {
            return switch (axis) {
                case X -> Direction.WEST;
                case Y -> Direction.DOWN;
                case Z -> Direction.NORTH;
            };
        }
        return null;
    }

    private static boolean touchesFace(AABB box, Direction direction) {
        double epsilon = 1.0E-5D;
        return switch (direction) {
            case DOWN -> box.minY <= epsilon;
            case UP -> box.maxY >= 1.0D - epsilon;
            case NORTH -> box.minZ <= epsilon;
            case SOUTH -> box.maxZ >= 1.0D - epsilon;
            case WEST -> box.minX <= epsilon;
            case EAST -> box.maxX >= 1.0D - epsilon;
        };
    }

    private static boolean overlapsOnSharedFace(AABB sourceBox, AABB targetBox, Direction direction) {
        double epsilon = 1.0E-5D;
        return switch (direction.getAxis()) {
            case X -> overlaps(sourceBox.minY, sourceBox.maxY, targetBox.minY, targetBox.maxY, epsilon)
                    && overlaps(sourceBox.minZ, sourceBox.maxZ, targetBox.minZ, targetBox.maxZ, epsilon);
            case Y -> overlaps(sourceBox.minX, sourceBox.maxX, targetBox.minX, targetBox.maxX, epsilon)
                    && overlaps(sourceBox.minZ, sourceBox.maxZ, targetBox.minZ, targetBox.maxZ, epsilon);
            case Z -> overlaps(sourceBox.minX, sourceBox.maxX, targetBox.minX, targetBox.maxX, epsilon)
                    && overlaps(sourceBox.minY, sourceBox.maxY, targetBox.minY, targetBox.maxY, epsilon);
        };
    }

    private static boolean overlaps(double firstMin, double firstMax, double secondMin, double secondMax, double epsilon) {
        return Math.max(firstMin, secondMin) < Math.min(firstMax, secondMax) - epsilon;
    }

    private static @Nullable Rotation extractRotation(@Nullable Object transform) {
        if (transform == null) {
            return null;
        }

        for (String methodName : List.of("getRotation", "rotation")) {
            try {
                java.lang.reflect.Method method = transform.getClass().getMethod(methodName);
                method.setAccessible(true);
                Object value = method.invoke(transform);
                if (value instanceof Rotation rotation) {
                    return rotation;
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            }
        }

        for (String fieldName : List.of("rotation", "rot")) {
            try {
                java.lang.reflect.Field field = transform.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(transform);
                if (value instanceof Rotation rotation) {
                    return rotation;
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static BlockState[] rotateQuadStepSlots(BlockState sourceState,
                                                    BlockState targetState,
                                                    BlockState[] slots,
                                                    Rotation rotation) {
        BlockState[] rotatedSlots = new BlockState[Math.max(MAX_SLOTS, slots.length)];
        Direction.Axis sourceAxis = sourceState.getValue(QuadStepBlock.AXIS);
        Direction.Axis targetAxis = targetState.getValue(QuadStepBlock.AXIS);

        for (int index = 0; index < Math.min(MAX_SLOTS, slots.length); index++) {
            BlockState slot = slots[index];
            if (slot == null) {
                continue;
            }

            Direction rotatedSide = rotation.rotate(QuadStepBlock.sideForIndex(sourceAxis, index));
            int targetIndex = QuadStepBlock.toIndex(isPositiveOnAxis(rotatedSide, targetAxis), QuadStepBlock.isTop(index));
            rotatedSlots[targetIndex] = slot;
        }
        return copySlots(rotatedSlots);
    }

    private static BlockState[] rotateQuadVerticalStepSlots(BlockState[] slots, Rotation rotation) {
        BlockState[] rotatedSlots = new BlockState[Math.max(MAX_SLOTS, slots.length)];

        for (int index = 0; index < Math.min(MAX_SLOTS, slots.length); index++) {
            BlockState slot = slots[index];
            if (slot == null) {
                continue;
            }

            Direction rotatedFacing = rotation.rotate(QuadVerticalStepBlock.facingForIndex(index));
            Direction rotatedSide = rotation.rotate(QuadVerticalStepBlock.sideForIndex(index));
            Direction facing = horizontalDirectionOnAxis(Direction.Axis.Z, rotatedFacing, rotatedSide);
            Direction side = horizontalDirectionOnAxis(Direction.Axis.X, rotatedFacing, rotatedSide);
            if (facing == null || side == null) {
                rotatedSlots[index] = slot;
                continue;
            }

            int targetIndex = QuadVerticalStepBlock.toIndex(side == Direction.EAST, facing == Direction.SOUTH);
            rotatedSlots[targetIndex] = slot;
        }
        return copySlots(rotatedSlots);
    }

    private static boolean isPositiveOnAxis(Direction direction, Direction.Axis axis) {
        if (axis == Direction.Axis.Z) {
            return direction == Direction.SOUTH;
        }
        return direction == Direction.EAST;
    }

    private static @Nullable Direction horizontalDirectionOnAxis(Direction.Axis axis, Direction first, Direction second) {
        if (first != null && first.getAxis() == axis) {
            return first;
        }
        if (second != null && second.getAxis() == axis) {
            return second;
        }
        return null;
    }

    public static void invalidateClientContraptionsForSource(Level level, long sourcePosLong) {
        if (level == null || !level.isClientSide()) return;

        synchronized (PENDING_CLIENT_CONTRAPTION_INVALIDATIONS) {
            PENDING_CLIENT_CONTRAPTION_INVALIDATIONS.put(sourcePosLong, CLIENT_CONTRAPTION_INVALIDATION_RETRY_TICKS);
        }
        if (invalidateClientContraptionsNow(level, sourcePosLong)) {
            synchronized (PENDING_CLIENT_CONTRAPTION_INVALIDATIONS) {
                PENDING_CLIENT_CONTRAPTION_INVALIDATIONS.remove(sourcePosLong);
            }
        }
    }

    public static void tickClientContraptionInvalidations(Level level) {
        if (level == null || !level.isClientSide()) return;

        List<Long> positions = new ArrayList<>();
        synchronized (PENDING_CLIENT_CONTRAPTION_INVALIDATIONS) {
            Iterator<Map.Entry<Long, Integer>> iterator = PENDING_CLIENT_CONTRAPTION_INVALIDATIONS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, Integer> entry = iterator.next();
                int remaining = entry.getValue();
                if (remaining <= 0) {
                    iterator.remove();
                    continue;
                }
                entry.setValue(remaining - 1);
                positions.add(entry.getKey());
            }
        }

        for (Long position : positions) {
            if (position != null && invalidateClientContraptionsNow(level, position)) {
                synchronized (PENDING_CLIENT_CONTRAPTION_INVALIDATIONS) {
                    PENDING_CLIENT_CONTRAPTION_INVALIDATIONS.remove(position);
                }
            }
        }
    }

    private static boolean invalidateClientContraptionsNow(Level level, long sourcePosLong) {
        Object entitiesObject = invokeNoArg(level, "entitiesForRendering");
        if (!(entitiesObject instanceof Iterable<?> entities)) return false;

        boolean invalidated = false;
        BlockPos sourcePos = BlockPos.of(sourcePosLong);
        for (Object entity : entities) {
            Object contraption = invokeNoArg(entity, "getContraption");
            if (contraption == null || !containsSourcePos(contraption, sourcePos)) {
                continue;
            }
            invokeNoArg(contraption, "resetClientContraption");
            invokeNoArg(contraption, "invalidateClientContraptionStructure");
            invokeNoArg(contraption, "invalidateClientContraptionChildren");
            invalidated = true;
        }
        return invalidated;
    }

    private static void rememberRenderSlots(Object renderLevel, BlockPos localPos, BlockState[] slots) {
        if (renderLevel == null || localPos == null || isEmpty(slots)) return;

        EnhancedSlabMaterialSavedData.rememberClient(localPos.asLong(), slots);
        if (renderLevel instanceof Level level) {
            EnhancedSlabMaterialSavedData.rememberClient(level, localPos.asLong(), slots);
        }

        Object current = renderLevel;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            synchronized (CLIENT_RENDER_LEVEL_SLOTS) {
                CLIENT_RENDER_LEVEL_SLOTS
                        .computeIfAbsent(current, ignored -> new java.util.HashMap<>())
                        .put(localPos.asLong(), copySlots(slots));
            }

            Object wrapped = unwrapWrappedLevel(current);
            if (wrapped == null || wrapped == current) {
                break;
            }
            current = wrapped;
        }
    }

    private static void rememberRenderWorldPos(Object renderLevel, Object contraption, BlockPos localPos) {
        BlockPos worldPos = renderWorldPos(contraption, localPos);
        if (renderLevel == null || localPos == null || worldPos == null) return;

        Object current = renderLevel;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            synchronized (CLIENT_RENDER_LEVEL_WORLD_POS) {
                CLIENT_RENDER_LEVEL_WORLD_POS
                        .computeIfAbsent(current, ignored -> new java.util.HashMap<>())
                        .put(localPos.asLong(), worldPos.asLong());
            }

            Object wrapped = unwrapWrappedLevel(current);
            if (wrapped == null || wrapped == current) {
                break;
            }
            current = wrapped;
        }
    }

    private static @Nullable BlockState[] mappedClientSlots(Object renderLevel, BlockPos localPos) {
        Long worldPosKey = null;
        synchronized (CLIENT_RENDER_LEVEL_WORLD_POS) {
            Map<Long, Long> byLocalPos = CLIENT_RENDER_LEVEL_WORLD_POS.get(renderLevel);
            if (byLocalPos != null) {
                worldPosKey = byLocalPos.get(localPos.asLong());
            }
        }
        if (worldPosKey == null) return null;

        BlockState[] slots = EnhancedSlabMaterialSavedData.getClient(worldPosKey);
        if (isEmpty(slots)) return null;
        rememberRenderSlots(renderLevel, localPos, slots);
        return copySlots(slots);
    }

    private static @Nullable Object unwrapWrappedLevel(Object candidate) {
        for (String fieldName : WRAPPED_LEVEL_FIELDS) {
            java.lang.reflect.Field field = findField(candidate.getClass(), fieldName);
            if (field == null) {
                continue;
            }
            try {
                return field.get(candidate);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static @Nullable BlockState[] findSeededRenderSlots(BlockPos pos) {
        BlockState[] found = null;
        long key = pos.asLong();
        synchronized (CLIENT_RENDER_LEVEL_SLOTS) {
            for (Map<Long, BlockState[]> byPos : CLIENT_RENDER_LEVEL_SLOTS.values()) {
                BlockState[] slots = byPos.get(key);
                if (slots == null) {
                    continue;
                }
                if (found != null && !sameSlots(found, slots)) {
                    return null;
                }
                found = slots;
            }
        }
        return found == null ? null : copySlots(found);
    }

    private static @Nullable BlockState[] findMappedClientSlots(BlockPos localPos) {
        BlockState[] found = null;
        long key = localPos.asLong();
        synchronized (CLIENT_RENDER_LEVEL_WORLD_POS) {
            for (Map<Long, Long> byLocalPos : CLIENT_RENDER_LEVEL_WORLD_POS.values()) {
                Long worldPosKey = byLocalPos.get(key);
                if (worldPosKey == null) {
                    continue;
                }
                BlockState[] slots = EnhancedSlabMaterialSavedData.getClient(worldPosKey);
                if (isEmpty(slots)) {
                    continue;
                }
                if (found != null && !sameSlots(found, slots)) {
                    return null;
                }
                found = slots;
            }
        }
        return found == null ? null : copySlots(found);
    }

    private static boolean sameSlots(BlockState[] first, BlockState[] second) {
        int length = Math.max(first.length, second.length);
        for (int i = 0; i < Math.min(length, MAX_SLOTS); i++) {
            if (!Objects.equals(slot(first, i), slot(second, i))) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable BlockState[] clientFallbackSlots(Object contraption, BlockPos localPos) {
        if (localPos == null) return null;

        BlockPos worldPos = renderWorldPos(contraption, localPos);
        if (worldPos != null) {
            BlockState[] anchored = EnhancedSlabMaterialSavedData.getClient(worldPos.asLong());
            if (!isEmpty(anchored)) {
                return anchored;
            }
        }
        BlockState[] local = EnhancedSlabMaterialSavedData.getClient(localPos.asLong());
        return isEmpty(local) ? null : local;
    }

    private static @Nullable BlockPos renderWorldPos(Object contraption, BlockPos localPos) {
        if (contraption == null || localPos == null) return null;

        Object anchorObject = findFieldValue(contraption, "anchor");
        return anchorObject instanceof BlockPos anchor ? anchor.offset(localPos) : null;
    }

    private static @Nullable BlockState[] contraptionSlots(Object target,
                                                           BlockPos localPos,
                                                           @Nullable BlockState state,
                                                           java.util.IdentityHashMap<Object, Boolean> visited) {
        if (target == null || localPos == null || visited.containsKey(target)) return null;
        visited.put(target, Boolean.TRUE);

        BlockState[] direct = slotsFromContraption(target, localPos, state);
        if (!isEmpty(direct)) {
            return direct;
        }

        Object contraption = findFieldValue(target, "contraption");
        BlockState[] fromContraption = contraptionSlots(contraption, localPos, state, visited);
        if (!isEmpty(fromContraption)) {
            return fromContraption;
        }

        Object getterContraption = invokeNoArg(target, "getContraption");
        BlockState[] fromGetter = contraptionSlots(getterContraption, localPos, state, visited);
        if (!isEmpty(fromGetter)) {
            return fromGetter;
        }

        Object wrapped = unwrapWrappedLevel(target);
        BlockState[] fromWrapped = contraptionSlots(wrapped, localPos, state, visited);
        return isEmpty(fromWrapped) ? null : fromWrapped;
    }

    private static @Nullable BlockState[] slotsFromContraption(Object target, BlockPos localPos, @Nullable BlockState state) {
        if (target == null || localPos == null) return null;

        Map<?, ?> updateTags = findMapFieldValue(target, "updateTags");
        CompoundTag updateTag = tagFor(updateTags, localPos);
        BlockState[] updateSlots = loadSlots(updateTag);
        if (!isEmpty(updateSlots)) {
            return updateSlots;
        }

        Object blocksObject = invokeNoArg(target, "getBlocks");
        if (!(blocksObject instanceof Map<?, ?> blocks)) {
            blocksObject = findFieldValue(target, "blocks");
        }
        if (!(blocksObject instanceof Map<?, ?> blocks)) {
            return null;
        }

        StructureBlockInfo info = structureInfoAt(blocks, localPos);
        if (info == null || !supports(info.state())) {
            return null;
        }

        CompoundTag blockTag = renderTag(info.nbt(), updateTag);
        BlockState[] blockSlots = loadSlots(blockTag);
        if (!isEmpty(blockSlots)) {
            return blockSlots;
        }

        BlockState[] fallbackSlots = clientFallbackSlots(target, localPos);
        return isEmpty(fallbackSlots) ? null : fallbackSlots;
    }

    private static @Nullable StructureBlockInfo structureInfoAt(Map<?, ?> blocks, BlockPos localPos) {
        Object direct = blocks.get(localPos);
        if (direct instanceof StructureBlockInfo info) {
            return info;
        }

        for (Map.Entry<?, ?> entry : blocks.entrySet()) {
            if (entry.getValue() instanceof StructureBlockInfo info) {
                if (localPos.equals(info.pos()) || localPos.equals(entry.getKey())) {
                    return info;
                }
            }
        }
        return null;
    }

    private static boolean isCreateLike(Object target) {
        if (target == null) return false;

        String className = target.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        if (className.contains("create") || className.contains("contraption")) {
            return true;
        }

        return findFieldValue(target, "contraption") != null
                || findMapFieldValue(target, "blocks") != null
                || findMapFieldValue(target, "updateTags") != null;
    }

    private static boolean containsSourcePos(Object contraption, BlockPos sourcePos) {
        if (contraption == null || sourcePos == null) return false;

        Object blocksObject = invokeNoArg(contraption, "getBlocks");
        if (!(blocksObject instanceof Map<?, ?> blocks)) {
            blocksObject = findFieldValue(contraption, "blocks");
        }
        if (!(blocksObject instanceof Map<?, ?> blocks)) return false;

        for (Map.Entry<?, ?> entry : blocks.entrySet()) {
            if (entry.getKey() instanceof BlockPos localPos && sourcePos.equals(renderWorldPos(contraption, localPos))) {
                return true;
            }
            if (entry.getValue() instanceof StructureBlockInfo info
                    && sourcePos.equals(renderWorldPos(contraption, info.pos()))) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable Object invokeNoArg(Object target, String name) {
        java.lang.reflect.Method method = findNoArgMethod(target.getClass(), name);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static @Nullable java.lang.reflect.Method findNoArgMethod(Class<?> owner, String name) {
        NoArgMethodKey key = new NoArgMethodKey(owner, name);
        java.lang.reflect.Method cached = NO_ARG_METHOD_CACHE.get(key);
        if (cached != null) {
            return cached == MissingNoArgMethod.INSTANCE ? null : cached;
        }

        Class<?> type = owner;
        while (type != null) {
            for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && method.getName().equals(name)) {
                    method.setAccessible(true);
                    NO_ARG_METHOD_CACHE.put(key, method);
                    return method;
                }
            }
            type = type.getSuperclass();
        }
        NO_ARG_METHOD_CACHE.put(key, MissingNoArgMethod.INSTANCE);
        return null;
    }

    private record NoArgMethodKey(Class<?> owner, String name) {
    }

    private static final class MissingNoArgMethod {
        private static final java.lang.reflect.Method INSTANCE = missing();

        private static java.lang.reflect.Method missing() {
            try {
                return MissingNoArgMethod.class.getDeclaredMethod("missing");
            } catch (NoSuchMethodException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    private static @Nullable CompoundTag renderTag(@Nullable CompoundTag blockTag, @Nullable CompoundTag updateTag) {
        if (blockTag != null && blockTag.contains(TAG_MATERIALS, Tag.TAG_COMPOUND)) {
            return blockTag;
        }
        return updateTag;
    }

    private static @Nullable CompoundTag tagFor(@Nullable Map<?, ?> tags, BlockPos pos) {
        if (tags == null || pos == null) {
            return null;
        }
        Object tag = tags.get(pos);
        return tag instanceof CompoundTag compound ? compound : null;
    }

    private static @Nullable Map<?, ?> findMapFieldValue(Object target, String name) {
        Object value = findFieldValue(target, name);
        return value instanceof Map<?, ?> map ? map : null;
    }

    private static @Nullable Object findFieldValue(Object target, String name) {
        if (target == null) return null;
        java.lang.reflect.Field field = findField(target.getClass(), name);
        if (field == null) {
            return null;
        }
        try {
            return field.get(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static @Nullable java.lang.reflect.Field findField(Class<?> owner, String name) {
        FieldKey key = new FieldKey(owner, name);
        java.lang.reflect.Field cached = FIELD_CACHE.get(key);
        if (cached != null) {
            return cached == MissingField.INSTANCE ? null : cached;
        }

        Class<?> type = owner;
        while (type != null) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                if (field.getName().equals(name)) {
                    field.setAccessible(true);
                    FIELD_CACHE.put(key, field);
                    return field;
                }
            }
            type = type.getSuperclass();
        }
        FIELD_CACHE.put(key, MissingField.INSTANCE);
        return null;
    }

    private record FieldKey(Class<?> owner, String name) {
    }

    private static final class MissingField {
        private static final java.lang.reflect.Field INSTANCE = missing();

        private static java.lang.reflect.Field missing() {
            try {
                return MissingField.class.getDeclaredField("INSTANCE");
            } catch (NoSuchFieldException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    private static @Nullable BlockState[] captureSlots(Level level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || !supports(state)) return null;

        BlockEntity blockEntity = SableCompat.plotBlockEntity(level, pos);
        if (blockEntity == null) {
            blockEntity = level.getBlockEntity(pos);
        }
        if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            return new BlockState[]{verticalSlab.getFacingSlab(), verticalSlab.getOppositeSlab()};
        }
        if (blockEntity instanceof StepBlockEntity step) {
            return new BlockState[]{step.getSlabState()};
        }
        if (blockEntity instanceof VerticalStepBlockEntity verticalStep) {
            return new BlockState[]{verticalStep.getSlabState()};
        }
        if (blockEntity instanceof QuadStepBlockEntity quadStep) {
            return quadStep.getAllSlabs();
        }
        if (blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep) {
            return quadVerticalStep.getAllSlabs();
        }

        BlockState[] fallback = fixedVariantSlots(state);
        if (!isEmpty(fallback)) return fallback;

        BlockState[] stored = EnhancedSlabMaterialSavedData.get(level, pos.asLong());
        if (!isEmpty(stored)) return stored;

        EnhancedSlabPistonData pistonData = EnhancedSlabPistonHooks.capture(level, pos);
        return pistonData == null ? null : pistonData.copySlabs();
    }

    private static @Nullable BlockState[] fixedVariantSlots(BlockState state) {
        if (state == null) return null;

        Block block = state.getBlock();
        if (block instanceof FixedStepBlock fixedStep) {
            return new BlockState[]{normalizeSlabState(fixedStep.getSourceSlab().defaultBlockState())};
        }
        if (block instanceof FixedVerticalStepBlock fixedVerticalStep) {
            return new BlockState[]{normalizeVerticalSourceSlab(fixedVerticalStep.getSourceVerticalSlab())};
        }
        if (block instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            BlockState slab = normalizeSlabState(fixedVerticalSlab.getSourceSlab().defaultBlockState());
            return state.hasProperty(VerticalSlabBlock.DOUBLE) && state.getValue(VerticalSlabBlock.DOUBLE)
                    ? new BlockState[]{slab, slab}
                    : new BlockState[]{slab};
        }

        Block sourceSlab = StepDynamicRegistry.getSlabForStep(block);
        if (sourceSlab != null) {
            return new BlockState[]{normalizeSlabState(sourceSlab.defaultBlockState())};
        }

        Block sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(block);
        if (sourceVerticalSlab != null) {
            return new BlockState[]{normalizeVerticalSourceSlab(sourceVerticalSlab)};
        }

        Block verticalSourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(block);
        if (verticalSourceSlab != null) {
            BlockState slab = normalizeSlabState(verticalSourceSlab.defaultBlockState());
            return state.hasProperty(VerticalSlabBlock.DOUBLE) && state.getValue(VerticalSlabBlock.DOUBLE)
                    ? new BlockState[]{slab, slab}
                    : new BlockState[]{slab};
        }

        return null;
    }

    private static BlockState normalizeVerticalSourceSlab(Block sourceVerticalSlab) {
        Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
        return normalizeSlabState((sourceSlab != null ? sourceSlab : sourceVerticalSlab).defaultBlockState());
    }

    private static BlockState normalizeSlabState(BlockState state) {
        BlockState normalized = state;
        if (normalized.hasProperty(SlabBlock.TYPE)) {
            normalized = normalized.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (normalized.hasProperty(SlabBlock.WATERLOGGED)) {
            normalized = normalized.setValue(SlabBlock.WATERLOGGED, false);
        }
        return normalized;
    }

    private static CompoundTag saveSlots(BlockState[] slots) {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < Math.min(MAX_SLOTS, slots.length); i++) {
            BlockState slot = slots[i];
            if (slot != null && slot.getBlock() instanceof SlabBlock) {
                tag.put(TAG_SLOT_PREFIX + i, SlabStateTagHelper.saveSlabState(slot, Blocks.OAK_SLAB));
            }
        }
        return tag;
    }

    private static @Nullable BlockState[] loadSlots(@Nullable CompoundTag blockTag) {
        if (blockTag == null || !blockTag.contains(TAG_MATERIALS, Tag.TAG_COMPOUND)) return null;

        CompoundTag tag = blockTag.getCompound(TAG_MATERIALS);
        BlockState[] slots = new BlockState[MAX_SLOTS];
        boolean any = false;
        for (int i = 0; i < MAX_SLOTS; i++) {
            String key = TAG_SLOT_PREFIX + i;
            if (tag.contains(key, Tag.TAG_COMPOUND)) {
                slots[i] = SlabStateTagHelper.loadOptionalStoredSlabState(tag.getCompound(key), Blocks.OAK_SLAB);
                any |= slots[i] != null;
            }
        }
        return any ? slots : null;
    }

    private static boolean isEmpty(@Nullable BlockState[] slots) {
        if (slots == null) return true;
        for (BlockState slot : slots) {
            if (slot != null) return false;
        }
        return true;
    }

    private static @Nullable BlockState slot(BlockState[] slots, int index) {
        return index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static BlockState[] copySlots(BlockState[] slots) {
        BlockState[] copy = new BlockState[Math.min(MAX_SLOTS, slots.length)];
        System.arraycopy(slots, 0, copy, 0, copy.length);
        return copy;
    }

    private record SableRapierColliderKey(BlockState state, List<SableRapierBox> boxes) {
    }

    private record SableMassBox(AABB box, double mass) {
    }

    private record SableRapierBox(double minX,
                                  double minY,
                                  double minZ,
                                  double maxX,
                                  double maxY,
                                  double maxZ) {
        private SableRapierBox(AABB box) {
            this(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        }
    }
}
