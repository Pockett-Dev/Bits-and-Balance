package org.onenonly.bitsandbalance.common.compat.litematica;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.SlabStateTagHelper;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LitematicaEnhancedSlabCompat {
    public static final String MARKER_ID = "bitsandbalance:litematica_enhanced_slab_materials";
    private static final String TAG_ID = "id";
    private static final String TAG_MARKER = "BitsAndBalanceEnhancedSlabMaterials";
    private static final String TAG_KIND = "Kind";
    private static final String TAG_SLOT_PREFIX = "Slot";
    private static final int MAX_SLOTS = 4;
    private static final String COMPOUND_DATA_CLASS = "fi.dy.masa.malilib.util.data.tag.CompoundData";
    private static final String DATA_CONVERTER_CLASS = "fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt";

    private LitematicaEnhancedSlabCompat() {
    }

    public static boolean handles(BlockState state) {
        return state != null && (state.getBlock() instanceof MixedSlabBlock
                || state.getBlock() instanceof VerticalSlabBlock
                || state.getBlock() instanceof StepBlock
                || state.getBlock() instanceof VerticalStepBlock
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock);
    }

    public static @Nullable CompoundTag exportTag(LevelReader level, BlockPos worldPos, BlockPos schematicPos, BlockState state) {
        BlockState[] slots = resolveSlots(level, worldPos, state);
        if (isEmpty(slots)) {
            return null;
        }

        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ID, MARKER_ID);
        tag.putBoolean(TAG_MARKER, true);
        tag.putString(TAG_KIND, kind(state));
        tag.putInt("x", schematicPos.getX());
        tag.putInt("y", schematicPos.getY());
        tag.putInt("z", schematicPos.getZ());

        for (int i = 0; i < Math.min(MAX_SLOTS, slots.length); i++) {
            if (slots[i] != null) {
                tag.put(TAG_SLOT_PREFIX + i, SlabStateTagHelper.saveSlabState(slots[i], Blocks.OAK_SLAB));
            }
        }
        return tag;
    }

    public static boolean importTag(Level level, BlockPos pos, BlockState state, @Nullable Object tag) {
        if (!handles(state) || !isCompatTag(tag)) {
            return false;
        }

        BlockState[] slots = slotsFromTag(tag);
        if (isEmpty(slots)) {
            return false;
        }

        if (isSchematicWorld(level)) {
            EnhancedSlabCreateCompat.rememberRenderSlots(level, pos, state, null, slots);
        } else {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), slots);
        }
        return true;
    }

    public static void restorePlacementChunk(Level level, BlockPos chunkOrigin, Object schematicPlacement) {
        if (level == null || chunkOrigin == null || schematicPlacement == null) {
            return;
        }

        try {
            Object schematic = invokeNoArgs(schematicPlacement, "getSchematic");
            BlockPos origin = (BlockPos) invokeNoArgs(schematicPlacement, "getOrigin");
            Object regionNamesObject = invoke(
                    schematicPlacement,
                    "getRegionsTouchingChunk",
                    new Class<?>[]{int.class, int.class},
                    chunkOrigin.getX() >> 4,
                    chunkOrigin.getZ() >> 4
            );
            if (!(regionNamesObject instanceof Iterable<?> regionNames)) {
                return;
            }

            for (Object regionNameObject : regionNames) {
                if (regionNameObject == null) {
                    continue;
                }
                String regionName = regionNameObject.toString();
                Object container = invoke(
                        schematic,
                        "getSubRegionContainer",
                        new Class<?>[]{String.class},
                        regionName
                );
                Object blockEntityMapObject = invoke(
                        schematic,
                        "getBlockEntityMapForRegion",
                        new Class<?>[]{String.class},
                        regionName
                );
                Object placement = invoke(
                        schematicPlacement,
                        "getRelativeSubRegionPlacement",
                        new Class<?>[]{String.class},
                        regionName
                );
                if (container != null && blockEntityMapObject instanceof Map<?, ?> rawMap && placement != null) {
                    @SuppressWarnings("unchecked")
                    Map<BlockPos, ?> blockEntityMap = (Map<BlockPos, ?>) rawMap;
                    restorePlacedTags(
                            level,
                            regionName,
                            container,
                            blockEntityMap,
                            origin,
                            schematicPlacement,
                            placement,
                            chunkOrigin
                    );
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void restorePlacedTags(Level level,
                                         String regionName,
                                         Object container,
                                         Map<BlockPos, ?> blockEntityMap,
                                         BlockPos origin,
                                         Object schematicPlacement,
                                         Object placement,
                                         BlockPos chunkOrigin) {
        if (level == null || regionName == null || container == null || blockEntityMap == null
                || origin == null || schematicPlacement == null || placement == null || chunkOrigin == null) {
            return;
        }

        try {
            Object schematic = invokeNoArgs(schematicPlacement, "getSchematic");
            Vec3iShim transforms = createTransformContext(regionName, origin, schematic, schematicPlacement, placement);
            if (transforms == null) {
                return;
            }

            java.lang.reflect.Method get = container.getClass().getMethod("get", int.class, int.class, int.class);
            for (Map.Entry<BlockPos, ?> entry : blockEntityMap.entrySet()) {
                if (!isCompatTag(entry.getValue())) {
                    continue;
                }

                BlockPos rel = entry.getKey();
                BlockPos pos = transforms.toWorld(rel);
                if ((pos.getX() >> 4) != (chunkOrigin.getX() >> 4)
                        || (pos.getZ() >> 4) != (chunkOrigin.getZ() >> 4)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (!handles(state)) {
                    state = (BlockState) get.invoke(container, rel.getX(), rel.getY(), rel.getZ());
                }
                importTag(level, pos, state, entry.getValue());
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean isCompatTag(@Nullable Object storedTag) {
        CompoundTag tag = asVanillaTag(storedTag);
        if (tag == null || tag.isEmpty()) {
            return false;
        }
        //? if mc == 1.21.1
        return tag.getBoolean(TAG_MARKER) || MARKER_ID.equals(tag.getString(TAG_ID));
        //? else
        return tag.getBooleanOr(TAG_MARKER, false) || MARKER_ID.equals(tag.getStringOr(TAG_ID, ""));
        //? endif
    }

    public static @Nullable BlockState[] resolveSlots(LevelReader level, BlockPos pos, BlockState state) {
        if (!handles(state)) {
            return null;
        }

        if (level instanceof Level actualLevel) {
            BlockState[] saved = EnhancedSlabMaterialSavedData.peek(actualLevel, pos.asLong());
            if (saved != null) {
                return saved;
            }
        }

        BlockEntity be = level instanceof BlockGetter blockGetter ? blockGetter.getBlockEntity(pos) : null;
        if (be instanceof VerticalSlabBlockEntity vertical) {
            return new BlockState[]{vertical.getFacingSlab(), vertical.getOppositeSlab()};
        }
        if (be instanceof StepBlockEntity step) {
            return new BlockState[]{step.getSlabState()};
        }
        if (be instanceof VerticalStepBlockEntity verticalStep) {
            return new BlockState[]{verticalStep.getSlabState()};
        }
        if (be instanceof QuadStepBlockEntity quad) {
            return quad.getAllSlabs();
        }
        if (be instanceof QuadVerticalStepBlockEntity quadVertical) {
            return quadVertical.getAllSlabs();
        }
        return null;
    }

    public static BlockState[] slotsFromTag(@Nullable Object storedTag) {
        BlockState[] slots = new BlockState[MAX_SLOTS];
        CompoundTag tag = asVanillaTag(storedTag);
        if (!isCompatTag(tag)) {
            return slots;
        }
        for (int i = 0; i < MAX_SLOTS; i++) {
            //? if mc == 1.21.1
            if (tag.contains(TAG_SLOT_PREFIX + i, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                slots[i] = SlabStateTagHelper.loadOptionalStoredSlabState(tag.getCompound(TAG_SLOT_PREFIX + i), Blocks.OAK_SLAB);
            }
            //? else
            CompoundTag slotTag = tag.getCompoundOrEmpty(TAG_SLOT_PREFIX + i);
            if (!slotTag.isEmpty()) {
                slots[i] = SlabStateTagHelper.loadOptionalStoredSlabState(slotTag, Blocks.OAK_SLAB);
            }
            //? endif
        }
        return slots;
    }

    public static @Nullable Object toLitematicaTag(Object schematic,
                                                    Map<BlockPos, ?> existingTags,
                                                    @Nullable CompoundTag tag) {
        if (tag == null) {
            return null;
        }

        Object existing = existingTags == null || existingTags.isEmpty()
                ? null
                : existingTags.values().iterator().next();
        if (existing instanceof CompoundTag) {
            return tag;
        }
        if (existing != null && COMPOUND_DATA_CLASS.equals(existing.getClass().getName())) {
            return fromVanillaTag(tag, existing.getClass().getClassLoader());
        }

        try {
            java.lang.reflect.Method getter = schematic.getClass().getMethod("getBlockEntityMapForRegion", String.class);
            if (getter.getGenericReturnType().getTypeName().contains(COMPOUND_DATA_CLASS)) {
                return fromVanillaTag(tag, schematic.getClass().getClassLoader());
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return tag;
    }

    private static @Nullable CompoundTag asVanillaTag(@Nullable Object tag) {
        if (tag instanceof CompoundTag compoundTag) {
            return compoundTag;
        }
        if (tag == null || !COMPOUND_DATA_CLASS.equals(tag.getClass().getName())) {
            return null;
        }

        try {
            ClassLoader loader = tag.getClass().getClassLoader();
            Class<?> converter = Class.forName(DATA_CONVERTER_CLASS, false, loader);
            Class<?> compoundData = Class.forName(COMPOUND_DATA_CLASS, false, loader);
            Object converted = converter.getMethod("toVanillaCompound", compoundData).invoke(null, tag);
            return converted instanceof CompoundTag compoundTag ? compoundTag : null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static @Nullable Object fromVanillaTag(CompoundTag tag, ClassLoader loader) {
        try {
            Class<?> converter = Class.forName(DATA_CONVERTER_CLASS, false, loader);
            return converter.getMethod("fromVanillaCompound", CompoundTag.class).invoke(null, tag);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    public static List<ItemStack> materialItems(BlockState ownerState, @Nullable BlockState[] slots) {
        List<ItemStack> items = new ArrayList<>();
        if (isEmpty(slots)) {
            return items;
        }

        for (BlockState slot : slots) {
            if (slot == null) {
                continue;
            }

            ItemStack stack = itemForSlot(ownerState, slot);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        return items;
    }

    public static @Nullable BlockState targetedSlot(BlockState ownerState, @Nullable BlockState[] slots, BlockPos pos, double hitX, double hitY, double hitZ) {
        if (isEmpty(slots)) {
            return null;
        }

        if (ownerState.getBlock() instanceof MixedSlabBlock) {
            return slot(slots, hitY - pos.getY() >= 0.5D ? 1 : 0);
        }
        return first(slots);
    }

    private static ItemStack itemForSlot(BlockState ownerState, BlockState slot) {
        if (ownerState.getBlock() instanceof MixedSlabBlock) {
            return new ItemStack(slot.getBlock().asItem());
        }
        if (ownerState.getBlock() instanceof VerticalSlabBlock) {
            return VerticalSlabBlock.bitsandbalance$makeHalfDropItem(slot);
        }
        if (ownerState.getBlock() instanceof StepBlock || ownerState.getBlock() instanceof QuadStepBlock) {
            return StepBlock.bitsandbalance$makeStepDropItem(slot);
        }
        if (ownerState.getBlock() instanceof VerticalStepBlock || ownerState.getBlock() instanceof QuadVerticalStepBlock) {
            return VerticalStepBlock.bitsandbalance$makeVerticalStepDropItem(slot);
        }
        return ItemStack.EMPTY;
    }

    private static @Nullable BlockState first(@Nullable BlockState[] slots) {
        if (slots == null) {
            return null;
        }
        for (BlockState slot : slots) {
            if (slot != null) {
                return slot;
            }
        }
        return null;
    }

    private static @Nullable BlockState slot(@Nullable BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
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

    private static boolean isSchematicWorld(Level level) {
        return level != null
                && "fi.dy.masa.litematica.world.WorldSchematic".equals(level.getClass().getName());
    }

    private static String kind(BlockState state) {
        if (state.getBlock() instanceof MixedSlabBlock) return "mixed_slab";
        if (state.getBlock() instanceof VerticalSlabBlock) return "vertical_slab";
        if (state.getBlock() instanceof StepBlock) return "step";
        if (state.getBlock() instanceof VerticalStepBlock) return "vertical_step";
        if (state.getBlock() instanceof QuadStepBlock) return "quad_step";
        if (state.getBlock() instanceof QuadVerticalStepBlock) return "quad_vertical_step";
        return "unknown";
    }

    public static BlockState bottomSlab(BlockState state) {
        return normalizeSlab(state, SlabType.BOTTOM);
    }

    public static BlockState topSlab(BlockState state) {
        return normalizeSlab(state, SlabType.TOP);
    }

    private static BlockState normalizeSlab(BlockState state, SlabType type) {
        if (state != null && state.hasProperty(SlabBlock.TYPE)) {
            return state.setValue(SlabBlock.TYPE, type);
        }
        return state;
    }

    private static @Nullable Vec3iShim createTransformContext(String regionName,
                                                              BlockPos origin,
                                                              Object schematic,
                                                              Object schematicPlacement,
                                                              Object placement) throws ReflectiveOperationException {
        Object regionSizeObj = invoke(schematic, "getAreaSizeAsVec3i", new Class<?>[]{String.class}, regionName);
        if (!(regionSizeObj instanceof net.minecraft.core.Vec3i regionSize)) {
            return null;
        }

        BlockPos regionPos = (BlockPos) invokeNoArgs(placement, "getPos");
        Object positionUtils = litematicaPositionUtils(schematicPlacement.getClass().getClassLoader());
        net.minecraft.core.Vec3i endVec = (net.minecraft.core.Vec3i) invokeStaticByName(positionUtils, "getRelativeEndPositionFromAreaSize", regionSize);
        //? if mc == 26.3
        BlockPos posEndRel = new BlockPos(endVec.getX(), endVec.getY(), endVec.getZ()).offset(regionPos);
        //? else
        BlockPos posEndRel = new BlockPos(endVec).offset(regionPos);
        //? endif
        BlockPos posMinRel = (BlockPos) invokeStaticByName(positionUtils, "getMinCorner", regionPos, posEndRel);
        Object schematicMirror = invokeNoArgs(schematicPlacement, "getMirror");
        Object schematicRotation = invokeNoArgs(schematicPlacement, "getRotation");
        BlockPos regionPosTransformed = (BlockPos) invokeStaticByName(positionUtils, "getTransformedBlockPos", regionPos, schematicMirror, schematicRotation);
        BlockPos totalRegionPosTransformed = regionPosTransformed.offset(origin);
        int offsetX = posMinRel.getX() - regionPos.getX();
        int offsetY = posMinRel.getY() - regionPos.getY();
        int offsetZ = posMinRel.getZ() - regionPos.getZ();
        return new Vec3iShim(positionUtils, schematicPlacement, placement, totalRegionPosTransformed, offsetX, offsetY, offsetZ);
    }

    private static Object invokeNoArgs(Object target, String methodName) throws ReflectiveOperationException {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static Class<?> litematicaPositionUtils(ClassLoader loader) throws ClassNotFoundException {
        try {
            return Class.forName("fi.dy.masa.litematica.util.PositionUtils", false, loader);
        } catch (ClassNotFoundException ignored) {
            return Class.forName("fi.dy.masa.malilib.util.position.PositionUtils", false, loader);
        }
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws ReflectiveOperationException {
        return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
    }

    private static Object invokeStaticByName(Object targetClassObject, String methodName, Object... args) throws ReflectiveOperationException {
        Class<?> targetClass = (Class<?>) targetClassObject;
        for (java.lang.reflect.Method method : targetClass.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length
                    || !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            boolean compatible = true;
            for (int i = 0; i < parameters.length; i++) {
                if (args[i] != null && !parameters[i].isAssignableFrom(args[i].getClass())) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method.invoke(null, args);
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private record Vec3iShim(Object positionUtils,
                             Object schematicPlacement,
                             Object placement,
                             BlockPos totalRegionPosTransformed,
                             int offsetX,
                             int offsetY,
                             int offsetZ) {
        BlockPos toWorld(BlockPos relativePos) throws ReflectiveOperationException {
            BlockPos placementRelative = new BlockPos(
                    offsetX + relativePos.getX(),
                    offsetY + relativePos.getY(),
                    offsetZ + relativePos.getZ()
            );
            BlockPos transformed = (BlockPos) invokeStaticByName(
                    positionUtils,
                    "getTransformedPlacementPosition",
                    placementRelative,
                    schematicPlacement,
                    placement
            );
            return transformed.offset(totalRegionPosTransformed);
        }
    }
}
