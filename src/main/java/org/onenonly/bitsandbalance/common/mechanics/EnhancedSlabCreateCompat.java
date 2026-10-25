package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
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
import org.onenonly.bitsandbalance.common.compat.SableCompat;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class EnhancedSlabCreateCompat {
    public static final String TAG_MATERIALS = "BitsAndBalanceEnhancedSlabMaterials";
    private static final String TAG_SLOT_PREFIX = "Slot";
    private static final int MAX_SLOTS = 4;
    private static final String[] WRAPPED_LEVEL_FIELDS = { "wrapped", "level", "world", "delegate" };
    private static final Map<Object, Map<Long, BlockState[]>> CLIENT_RENDER_LEVEL_SLOTS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<NoArgMethodKey, java.lang.reflect.Method> NO_ARG_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<FieldKey, java.lang.reflect.Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static boolean simulatedMovementRegistered;

    private EnhancedSlabCreateCompat() {
    }

    public static boolean supports(@Nullable BlockState state) {
        if (state == null) {
            return false;
        }
        return state.getBlock() instanceof FixedStepBlock
                || state.getBlock() instanceof FixedVerticalSlabBlock
                || state.getBlock() instanceof FixedVerticalStepBlock
                || state.getBlock() instanceof MixedSlabBlock
                || EnhancedSlabPistonHooks.supports(state);
    }

    public static @Nullable CompoundTag withCapturedMaterials(Level level, BlockPos pos, BlockState state, @Nullable CompoundTag existingTag) {
        BlockState[] slots = captureSlots(level, pos, state);
        if (isEmpty(slots)) {
            return existingTag;
        }

        CompoundTag tag = existingTag == null ? new CompoundTag() : existingTag.copy();
        tag.put(TAG_MATERIALS, saveSlots(slots));
        return tag;
    }

    public static @Nullable BlockState[] captureMaterialSlots(Level level, BlockPos pos, BlockState state) {
        BlockState[] slots = captureSlots(level, pos, state);
        return isEmpty(slots) ? null : copySlots(slots);
    }

    public static void restoreMaterialSlots(Level level, BlockPos pos, BlockState state, @Nullable BlockState[] slots) {
        if (level == null || level.isClientSide() || pos == null || !supports(state) || isEmpty(slots)) {
            return;
        }
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), slots);
    }

    public static boolean hasMaterialSlots(@Nullable BlockState[] slots) {
        return !isEmpty(slots);
    }

    public static boolean isAttachedTowards(@Nullable BlockState state, @Nullable Level level, @Nullable BlockPos pos, @Nullable Direction direction) {
        if (state == null || level == null || pos == null || direction == null || !supports(state)) {
            return false;
        }

        if (!touchesOccupiedFace(state, level, pos, direction)) {
            return false;
        }

        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        return !supports(neighborState) || touchesOccupiedFace(neighborState, level, neighborPos, direction.getOpposite());
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

            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
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
                                || !supports(state)) {
                            return pass;
                        }
                        Direction direction = directionFromOffset(offset);
                        if (direction == null) {
                            return fail;
                        }
                        return isAttachedTowards(state, level, pos, direction) ? success : fail;
                    }
            );
            checksClass.getMethod("registerAttachedCheck", attachedCheckClass).invoke(null, proxy);
            simulatedMovementRegistered = true;
        } catch (ClassNotFoundException ignored) {
            // Simulated / Aeronautics is optional.
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    public static void restoreMovedSlots(Level level, BlockPos pos, BlockState state, @Nullable CompoundTag blockTag) {
        if (level == null || level.isClientSide() || !supports(state)) {
            return;
        }
        BlockState[] slots = loadSlots(blockTag);
        if (isEmpty(slots)) {
            return;
        }
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), slots);
    }

    public static void rememberRenderSlots(Object renderLevel, BlockPos localPos, BlockState state, @Nullable CompoundTag blockTag) {
        rememberRenderSlots(renderLevel, localPos, state, blockTag, null);
    }

    public static void rememberRenderSlots(Object renderLevel,
                                           BlockPos localPos,
                                           BlockState state,
                                           @Nullable CompoundTag blockTag,
                                           @Nullable BlockState[] fallbackSlots) {
        if (renderLevel == null || localPos == null || !supports(state)) {
            return;
        }
        BlockState[] slots = loadSlots(blockTag);
        if (isEmpty(slots) && !isEmpty(fallbackSlots)) {
            slots = fallbackSlots;
        }
        if (isEmpty(slots)) {
            return;
        }

        Object current = renderLevel;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            CLIENT_RENDER_LEVEL_SLOTS
                    .computeIfAbsent(current, ignored -> new java.util.HashMap<>())
                    .put(localPos.asLong(), copySlots(slots));

            Object wrapped = unwrapWrappedLevel(current);
            if (wrapped == null || wrapped == current) {
                break;
            }
            current = wrapped;
        }
    }

    public static void seedRenderSlots(Object renderLevel, Object contraption) {
        if (renderLevel == null || contraption == null) {
            return;
        }

        try {
            Object blocksObject = invokeNoArg(contraption, "getBlocks");
            if (!(blocksObject instanceof Map<?, ?> blocks)) {
                blocksObject = findFieldValue(contraption, "blocks");
            }
            if (!(blocksObject instanceof Map<?, ?> blocks)) {
                return;
            }

            for (Object value : blocks.values()) {
                if (!(value instanceof StructureBlockInfo info)) {
                    continue;
                }
                BlockPos localPos = info.pos();
                rememberRenderSlots(
                        renderLevel,
                        localPos,
                        info.state(),
                        info.nbt(),
                        clientFallbackSlots(contraption, localPos)
                );
            }
        } catch (RuntimeException ignored) {
        }
    }

    public static @Nullable BlockState[] getRenderSlots(Object level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }

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

            Object wrapped = unwrapWrappedLevel(current);
            if (wrapped == null || wrapped == current) {
                break;
            }
            current = wrapped;
        }
        return null;
    }

    public static @Nullable BlockState[] getContraptionSlots(Object level, BlockPos pos, @Nullable BlockState state) {
        if (level == null || pos == null || !supports(state)) {
            return null;
        }

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
        if (pos == null || state == null) {
            return Shapes.empty();
        }

        BlockState[] slots = getContraptionSlots(contraption, pos, state);
        if (isEmpty(slots)) {
            return Shapes.empty();
        }

        if (state.getBlock() instanceof QuadStepBlock) {
            return QuadStepBlock.bitsandbalance$occupiedShape(state.getValue(QuadStepBlock.AXIS), slots);
        }
        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            return QuadVerticalStepBlock.bitsandbalance$occupiedShape(slots);
        }
        return Shapes.empty();
    }

    public static @Nullable Vector3dc getSableCenterOfMass(@Nullable BlockGetter level, @Nullable BlockState state) {
        if (state == null || !supports(state)) {
            return null;
        }

        VoxelShape shape;
        try {
            shape = state.getCollisionShape(level == null ? EmptyBlockGetter.INSTANCE : level, BlockPos.ZERO);
        } catch (RuntimeException ignored) {
            shape = Shapes.block();
        }

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

    private static @Nullable Direction directionFromOffset(BlockPos offset) {
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

    private static @Nullable BlockState[] clientFallbackSlots(Object contraption, BlockPos localPos) {
        if (localPos == null) {
            return null;
        }

        Object anchorObject = findFieldValue(contraption, "anchor");
        if (anchorObject instanceof BlockPos anchor) {
            BlockState[] anchored = EnhancedSlabMaterialSavedData.getClient(anchor.offset(localPos).asLong());
            if (!isEmpty(anchored)) {
                return anchored;
            }
        }
        BlockState[] local = EnhancedSlabMaterialSavedData.getClient(localPos.asLong());
        return isEmpty(local) ? null : local;
    }

    private static @Nullable BlockState[] contraptionSlots(Object target,
                                                           BlockPos localPos,
                                                           @Nullable BlockState state,
                                                           java.util.IdentityHashMap<Object, Boolean> visited) {
        if (target == null || localPos == null || visited.containsKey(target)) {
            return null;
        }
        visited.put(target, Boolean.TRUE);

        BlockState[] direct = slotsFromContraption(target, localPos);
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

    private static @Nullable BlockState[] slotsFromContraption(Object target, BlockPos localPos) {
        if (target == null || localPos == null) {
            return null;
        }

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

        BlockState[] blockSlots = loadSlots(renderTag(info.nbt(), updateTag));
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
            if (entry.getValue() instanceof StructureBlockInfo info
                    && (localPos.equals(info.pos()) || localPos.equals(entry.getKey()))) {
                return info;
            }
        }
        return null;
    }

    private static @Nullable CompoundTag renderTag(@Nullable CompoundTag blockTag, @Nullable CompoundTag updateTag) {
        if (containsCompound(blockTag, TAG_MATERIALS)) {
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

    private static boolean isCreateLike(Object target) {
        if (target == null) {
            return false;
        }

        String className = target.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return className.contains("create")
                || className.contains("contraption")
                || findFieldValue(target, "contraption") != null
                || findMapFieldValue(target, "blocks") != null
                || findMapFieldValue(target, "updateTags") != null;
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

    private static @Nullable Object findFieldValue(Object target, String name) {
        if (target == null) {
            return null;
        }
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
        if (level == null || pos == null || !supports(state)) {
            return null;
        }

        BlockState[] stored = EnhancedSlabMaterialSavedData.get(level, pos.asLong());
        if (!isEmpty(stored)) {
            return stored;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
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

        EnhancedSlabPistonData pistonData = EnhancedSlabPistonHooks.capture(level, pos);
        return pistonData == null ? null : pistonData.copySlabs();
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
        if (blockTag == null || !containsCompound(blockTag, TAG_MATERIALS)) {
            return null;
        }

        CompoundTag tag = blockTag.getCompoundOrEmpty(TAG_MATERIALS);
        BlockState[] slots = new BlockState[MAX_SLOTS];
        boolean any = false;
        for (int i = 0; i < MAX_SLOTS; i++) {
            String key = TAG_SLOT_PREFIX + i;
            if (containsCompound(tag, key)) {
                slots[i] = SlabStateTagHelper.loadOptionalStoredSlabState(tag.getCompoundOrEmpty(key), Blocks.OAK_SLAB);
                any |= slots[i] != null;
            }
        }
        return any ? slots : null;
    }

    private static boolean containsCompound(CompoundTag tag, String key) {
        return tag != null && tag.get(key) instanceof CompoundTag;
    }

    private static boolean isEmpty(@Nullable BlockState[] slots) {
        if (slots == null) {
            return true;
        }
        for (BlockState slot : slots) {
            if (slot != null) {
                return false;
            }
        }
        return true;
    }

    private static BlockState[] copySlots(BlockState[] slots) {
        BlockState[] copy = new BlockState[Math.min(MAX_SLOTS, slots.length)];
        System.arraycopy(slots, 0, copy, 0, copy.length);
        return copy;
    }
}
