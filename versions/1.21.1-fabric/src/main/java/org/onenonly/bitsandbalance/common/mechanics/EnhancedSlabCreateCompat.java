package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.SlabStateTagHelper;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class EnhancedSlabCreateCompat {
    public static final String TAG_MATERIALS = "BitsAndBalanceEnhancedSlabMaterials";
    private static final String TAG_SLOT_PREFIX = "Slot";
    private static final int MAX_SLOTS = 4;
    private static final Map<Object, Map<Long, BlockState[]>> CLIENT_RENDER_LEVEL_SLOTS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<NoArgMethodKey, java.lang.reflect.Method> NO_ARG_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<FieldKey, java.lang.reflect.Field> FIELD_CACHE = new ConcurrentHashMap<>();

    private EnhancedSlabCreateCompat() {
    }

    public static boolean supports(@Nullable BlockState state) {
        if (state == null) return false;
        if (state.getBlock() instanceof FixedStepBlock
                || state.getBlock() instanceof FixedVerticalSlabBlock
                || state.getBlock() instanceof FixedVerticalStepBlock) {
            return false;
        }
        return state.getBlock() instanceof MixedSlabBlock || EnhancedSlabPistonHooks.supports(state);
    }

    public static boolean isAttachedTowards(@Nullable BlockState state, @Nullable Level level, @Nullable BlockPos pos, @Nullable Direction direction) {
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

    public static void restoreMovedSlots(Level level, BlockPos pos, BlockState state, @Nullable CompoundTag blockTag) {
        if (level == null || level.isClientSide() || !supports(state)) return;
        BlockState[] slots = loadSlots(blockTag);
        if (isEmpty(slots)) return;
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), slots);
    }

    public static boolean hasMaterialSlots(@Nullable BlockState[] slots) {
        return !isEmpty(slots);
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
        if (renderLevel == null || contraption == null) return;

        try {
            Object blocksObject = invokeNoArg(contraption, "getBlocks");
            if (!(blocksObject instanceof Map<?, ?> blocks)) {
                blocksObject = findFieldValue(contraption, "blocks");
            }
            if (!(blocksObject instanceof Map<?, ?> blocks)) return;

            for (Object value : blocks.values()) {
                if (!(value instanceof StructureBlockInfo info)) continue;
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
        if (level == null || pos == null) return null;

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

    private static @Nullable Object unwrapWrappedLevel(Object candidate) {
        java.lang.reflect.Field field = findField(candidate.getClass(), "wrapped");
        if (field == null) {
            return null;
        }
        try {
            return field.get(candidate);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static @Nullable BlockState[] clientFallbackSlots(Object contraption, BlockPos localPos) {
        if (localPos == null) return null;

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

        BlockState[] stored = EnhancedSlabMaterialSavedData.get(level, pos.asLong());
        if (!isEmpty(stored)) return stored;

        BlockEntity blockEntity = level.getBlockEntity(pos);

        EnhancedSlabPistonData pistonData = EnhancedSlabPistonHooks.capture(level, pos);
        return pistonData == null ? null : pistonData.copySlabs();
    }

    private static CompoundTag saveSlots(BlockState[] slots) {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < Math.min(MAX_SLOTS, slots.length); i++) {
            BlockState slot = slots[i];
            if (slot != null) {
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
                slots[i] = SlabStateTagHelper.loadStoredSlabState(tag.getCompound(key), Blocks.OAK_SLAB);
                any = true;
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

    private static BlockState[] copySlots(BlockState[] slots) {
        BlockState[] copy = new BlockState[Math.min(MAX_SLOTS, slots.length)];
        System.arraycopy(slots, 0, copy, 0, copy.length);
        return copy;
    }
}
