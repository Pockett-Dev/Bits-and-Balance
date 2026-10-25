package org.onenonly.bitsandbalance.common.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class SableCompat {
    private static final boolean FIXTURE_DIAGNOSTICS =
            System.getProperty("bitsandbalance.fixtureRunner") != null;
    private static final ThreadLocal<Integer> ORE_VARIANT_REPLACEMENT_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> PLOT_BLOCK_WRITE_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final int MAX_HEATMAP_REBUILD_VOLUME = 131_072;
    private static final int MAX_PLOT_MATERIAL_CLEAR_VOLUME = 131_072;
    private static final Map<Object, Map<Long, Integer>> HEATMAP_COMPONENTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile boolean initialized;
    private static volatile boolean available;
    private static Object helper;
    private static Method isInPlotGrid;
    private static Method getContainingByPos;
    private static volatile boolean setUnsavedInitialized;
    private static volatile Method setUnsaved;
    private static volatile Field heatMapSubLevel;
    private static volatile boolean heatMapSubLevelInitialized;
    private static volatile Method subLevelGetLevel;
    private static volatile Method subLevelGetPlot;
    private static volatile Method plotGetBoundingBox;
    private static volatile Method subLevelGetHeatMapManager;
    private static volatile Method heatMapOnSolidRemoved;
    private static volatile Method heatMapRemoveCell;
    private static volatile Method heatMapRebuildFrom;
    private static volatile int physicsColliderRefreshCount;
    private static volatile int positionAwareRapierColliderCount;
    private static volatile int lastPositionAwareRapierColliderHandle = -1;
    private static volatile int lastPositionAwareRapierColliderBoxCount;
    private static volatile int enhancedSlabDirectWakeAttemptCount;
    private static volatile int enhancedSlabDirectWakeSuccessCount;
    private static volatile int enhancedSlabMassTrackerRebuildCount;
    private static volatile int enhancedSlabMergedHistoryReuseCount;
    private static volatile int enhancedSlabSuccessorReconciliationAttemptCount;
    private static volatile int enhancedSlabSuccessorSupportedStateCount;
    private static volatile int enhancedSlabSuccessorRestoredSlotCount;
    private static volatile int enhancedSlabSuccessorPositiveMassCount;
    private static volatile int enhancedSlabSuccessorReconciliationFailureCount;
    private static volatile double enhancedSlabSuccessorLastMass;

    private SableCompat() {
    }

    public record LocalRay(Vec3 start, Vec3 end) {
    }

    private record PlotChunk(Object plot, @Nullable Object holder, LevelChunk chunk) {
    }

    public static void beginOreVariantReplacement() {
        ORE_VARIANT_REPLACEMENT_DEPTH.set(ORE_VARIANT_REPLACEMENT_DEPTH.get() + 1);
    }

    public static void endOreVariantReplacement() {
        int depth = ORE_VARIANT_REPLACEMENT_DEPTH.get() - 1;
        if (depth <= 0) {
            ORE_VARIANT_REPLACEMENT_DEPTH.remove();
        } else {
            ORE_VARIANT_REPLACEMENT_DEPTH.set(depth);
        }
    }

    public static boolean isOreVariantReplacementActive() {
        return ORE_VARIANT_REPLACEMENT_DEPTH.get() > 0;
    }

    public static boolean isPlotBlockWriteActive() {
        return PLOT_BLOCK_WRITE_DEPTH.get() > 0;
    }

    public static int physicsColliderRefreshCount() {
        return physicsColliderRefreshCount;
    }

    public static void recordPositionAwareRapierCollider(int handle, int boxCount) {
        if (!FIXTURE_DIAGNOSTICS || handle < 0 || boxCount <= 0) {
            return;
        }
        positionAwareRapierColliderCount++;
        lastPositionAwareRapierColliderHandle = handle;
        lastPositionAwareRapierColliderBoxCount = boxCount;
    }

    public static int positionAwareRapierColliderCount() {
        return positionAwareRapierColliderCount;
    }

    public static int lastPositionAwareRapierColliderHandle() {
        return lastPositionAwareRapierColliderHandle;
    }

    public static int lastPositionAwareRapierColliderBoxCount() {
        return lastPositionAwareRapierColliderBoxCount;
    }

    public static void recordEnhancedSlabDirectWakeAttempt() {
        if (FIXTURE_DIAGNOSTICS) {
            enhancedSlabDirectWakeAttemptCount++;
        }
    }

    public static void recordEnhancedSlabDirectWakeSuccess() {
        if (FIXTURE_DIAGNOSTICS) {
            enhancedSlabDirectWakeSuccessCount++;
        }
    }

    public static int enhancedSlabDirectWakeAttemptCount() {
        return enhancedSlabDirectWakeAttemptCount;
    }

    public static int enhancedSlabDirectWakeSuccessCount() {
        return enhancedSlabDirectWakeSuccessCount;
    }

    public static int enhancedSlabMassTrackerRebuildCount() {
        return enhancedSlabMassTrackerRebuildCount;
    }

    public static int enhancedSlabMergedHistoryReuseCount() {
        return enhancedSlabMergedHistoryReuseCount;
    }

    public static String enhancedSlabSuccessorReconciliationDiagnostics() {
        return "attempts:" + enhancedSlabSuccessorReconciliationAttemptCount
                + ",supportedStates:" + enhancedSlabSuccessorSupportedStateCount
                + ",restoredSlots:" + enhancedSlabSuccessorRestoredSlotCount
                + ",positiveMasses:" + enhancedSlabSuccessorPositiveMassCount
                + ",failures:" + enhancedSlabSuccessorReconciliationFailureCount
                + ",lastMass:" + enhancedSlabSuccessorLastMass;
    }

    public static void markChunkUnsaved(LevelChunk chunk) {
        Method method = getSetUnsavedMethod();
        if (method == null) {
            return;
        }

        try {
            method.invoke(chunk, true);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            setUnsaved = null;
            setUnsavedInitialized = true;
        }
    }

    public static boolean isInPlotGrid(Level level, int chunkX, int chunkZ) {
        if (level == null || !initialize()) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(isInPlotGrid.invoke(helper, level, chunkX, chunkZ));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            available = false;
            return false;
        }
    }

    public static LocalRay localInteractionRay(Player player, BlockPos targetPos, double range) {
        if (player == null || targetPos == null || !initialize()) {
            return null;
        }

        Level level = player.level();
        Object subLevel;
        try {
            subLevel = getContainingByPos.invoke(helper, level, targetPos);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            available = false;
            return null;
        }

        if (subLevel == null) {
            return null;
        }

        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(Math.max(0.0D, range)));
        Object pose;
        try {
            pose = subLevel.getClass().getMethod("logicalPose").invoke(subLevel);
            Method transformPositionInverse = pose.getClass().getMethod("transformPositionInverse", Vec3.class);
            Object localStart = transformPositionInverse.invoke(pose, start);
            Object localEnd = transformPositionInverse.invoke(pose, end);
            if (localStart instanceof Vec3 startVec && localEnd instanceof Vec3 endVec) {
                return new LocalRay(startVec, endVec);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }

        return null;
    }

    public static @Nullable BlockState plotBlockState(Level level, BlockPos pos) {
        if (level == null || pos == null || !initialize()) {
            return null;
        }

        try {
            PlotChunk plotChunk = plotChunk(level, pos);
            return plotChunk == null ? null : plotChunk.chunk().getBlockState(pos);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        return null;
    }

    public static @Nullable BlockEntity plotBlockEntity(Level level, BlockPos pos) {
        if (level == null || pos == null || !initialize()) {
            return null;
        }

        try {
            PlotChunk plotChunk = plotChunk(level, pos);
            return plotChunk == null ? null : plotChunk.chunk().getBlockEntity(pos);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        return null;
    }

    public static boolean setPlotBlockState(Level level, BlockPos pos, BlockState state, int flags) {
        if (level == null || pos == null || state == null || !initialize()) {
            return false;
        }

        try {
            PlotChunk plotChunk = plotChunk(level, pos);
            if (plotChunk == null) {
                return false;
            }

            BlockState oldState = plotChunk.chunk().getBlockState(pos);
            if (oldState == state) {
                return true;
            }

            boolean suppressSableCommonEvent = oldState.isAir() == state.isAir();
            if (suppressSableCommonEvent) {
                PLOT_BLOCK_WRITE_DEPTH.set(PLOT_BLOCK_WRITE_DEPTH.get() + 1);
            }
            try {
                if (!setChunkBlockState(plotChunk.chunk(), pos, state)) {
                    return false;
                }
            } finally {
                if (suppressSableCommonEvent) {
                    int depth = PLOT_BLOCK_WRITE_DEPTH.get() - 1;
                    if (depth <= 0) {
                        PLOT_BLOCK_WRITE_DEPTH.remove();
                    } else {
                        PLOT_BLOCK_WRITE_DEPTH.set(depth);
                    }
                }
            }

            markChunkUnsaved(plotChunk.chunk());
            notifyPlotBlockChange(plotChunk.plot(), plotChunk.holder(), plotChunk.chunk(), level, pos, oldState, state, flags);
            // Direct plot writes bypass Sable's normal world-space wake lookup. Always wake the
            // exact owner after a successful mutation; this is idempotent and keeps this helper's
            // vanilla-control behavior aligned with a normal in-world block change.
            wakeOwningPlotSubLevel(level, plotChunk.plot());
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    public static boolean refreshPlotBlockState(Level level,
                                                BlockPos pos,
                                                BlockState oldState,
                                                BlockState newState,
                                                int flags) {
        if (level == null || pos == null || oldState == null || newState == null || !initialize()) {
            return false;
        }

        try {
            PlotChunk plotChunk = plotChunk(level, pos);
            if (plotChunk == null) {
                return false;
            }
            notifyPlotBlockChange(plotChunk.plot(), plotChunk.holder(), plotChunk.chunk(), level, pos, oldState, newState, flags);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    public static boolean rememberPlotEnhancedSlabMaterials(Level level,
                                                            BlockPos pos,
                                                            BlockState state,
                                                            @Nullable BlockState[] slots,
                                                            int flags) {
        if (level == null || level.isClientSide() || pos == null || !EnhancedSlabCreateCompat.supports(state)
                || !EnhancedSlabCreateCompat.hasMaterialSlots(slots)) {
            return false;
        }

        PlotChunk plotChunk;
        try {
            plotChunk = initialize() ? plotChunk(level, pos) : null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            plotChunk = null;
        }
        if (plotChunk == null) {
            return false;
        }

        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), slots);
        hydrateEnhancedSlabBlockEntity(plotChunk.chunk().getBlockEntity(pos), slots);
        notifyPlotBlockChange(plotChunk.plot(), plotChunk.holder(), plotChunk.chunk(), level, pos, state, state, flags);
        refreshPhysicsCollider(level, plotChunk.chunk(), pos, state);
        rebuildOwningPlotMass(level, plotChunk.plot());
        return true;
    }

    /**
     * Resolves the parent Minecraft level used by a Sable plot accessor and
     * reads the material slots stored at the plot-grid position. Sable passes
     * its EmbeddedPlotLevelAccessor to shape and model lookups rather than the
     * parent Level, so Level-only callers otherwise lose both textures and
     * material-aware collision shapes while a block is in a physics object.
     */
    public static @Nullable BlockState[] enhancedSlabMaterials(Object level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }

        Level materialLevel = level instanceof Level actualLevel
                ? actualLevel
                : sableParentLevel(level);
        BlockPos materialPos = sableMaterialPos(level, pos);
        return materialLevel == null
                ? null
                : EnhancedSlabMaterialSavedData.get(materialLevel, materialPos.asLong());
    }

    /** Returns whether the player currently tracks the Sable sublevel containing a plot-grid position. */
    public static boolean isPlayerTrackingPlotPosition(Level level, BlockPos pos, UUID playerId) {
        if (level == null || pos == null || playerId == null || !initialize()) {
            return false;
        }

        try {
            Object subLevel = getContainingByPos.invoke(helper, level, pos);
            if (subLevel == null) {
                return false;
            }
            Method getTrackingPlayers = subLevel.getClass().getMethod("getTrackingPlayers");
            Object trackingPlayers = getTrackingPlayers.invoke(subLevel);
            return trackingPlayers instanceof java.util.Collection<?> collection && collection.contains(playerId);
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    private static @Nullable Level sableParentLevel(Object candidate) {
        String className = candidate.getClass().getName();
        if (!className.startsWith("dev.ryanhcode.sable.")) {
            return null;
        }

        Object current = candidate;
        for (int depth = 0; depth < 3 && current != null; depth++) {
            if (current instanceof Level level) {
                return level;
            }
            Object parent = invokeNoArg(current, "getLevel");
            if (parent instanceof Level level) {
                return level;
            }
            if (parent == null) {
                parent = sableWrappedLevel(current);
                if (parent instanceof Level level) {
                    return level;
                }
            }
            if (parent != null && parent != current) {
                current = parent;
                continue;
            }
            Object subLevel = invokeNoArg(current, "getSubLevel");
            if (subLevel == null || subLevel == current) {
                return null;
            }
            current = subLevel;
        }
        return null;
    }

    /**
     * Sable's {@code LevelAccelerator} exposes its wrapped level only through a private field.
     * Mass rebuilding uses that accessor, so storage-backed enhanced slabs need this fallback to
     * resolve their position-specific material slots while Sable calculates mass and solidity.
     */
    private static @Nullable Object sableWrappedLevel(Object candidate) {
        Class<?> type = candidate.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("level");
                field.setAccessible(true);
                return field.get(candidate);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static BlockPos sableMaterialPos(Object candidate, BlockPos pos) {
        String className = candidate.getClass().getName();
        if (!className.endsWith(".EmbeddedPlotLevelAccessor")) {
            return pos;
        }

        try {
            Field centerField = candidate.getClass().getDeclaredField("center");
            centerField.setAccessible(true);
            Object center = centerField.get(candidate);
            if (center instanceof BlockPos centerPos) {
                return pos.offset(centerPos);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        return pos;
    }

    private static @Nullable Object invokeNoArg(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    public static int physicsBodyId(Object body) {
        if (body == null) {
            return -1;
        }

        try {
            Class<?> rapierClass = Class.forName("dev.ryanhcode.sable.physics.impl.rapier.Rapier3D");
            for (Method method : rapierClass.getMethods()) {
                if (!"getID".equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                if (!method.getParameterTypes()[0].isInstance(body)) {
                    continue;
                }
                Object value = method.invoke(null, body);
                return value instanceof Number number ? number.intValue() : -1;
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        return -1;
    }

    /**
     * Captures the pose basis which Sable's merged-mass tracker normally retains between updates.
     * Replacing that tracker discards its previous center of mass, so the replacement tracker can
     * only report a zero center delta after a position-backed Step mutation.
     */
    private static @Nullable SubLevelPoseBasisSnapshot bitsandbalance$captureSubLevelPoseBasis(Object subLevel) {
        if (subLevel == null) {
            return null;
        }

        try {
            PoseBasisSnapshot logical = bitsandbalance$poseBasisSnapshot(
                    subLevel.getClass().getMethod("logicalPose").invoke(subLevel)
            );
            PoseBasisSnapshot last = bitsandbalance$poseBasisSnapshot(
                    subLevel.getClass().getMethod("lastPose").invoke(subLevel)
            );
            return logical == null || last == null ? null : new SubLevelPoseBasisSnapshot(logical, last);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static @Nullable PoseBasisSnapshot bitsandbalance$poseBasisSnapshot(Object pose)
            throws ReflectiveOperationException {
        if (pose == null) {
            return null;
        }

        Object positionObject = pose.getClass().getMethod("position").invoke(pose);
        Object orientationObject = pose.getClass().getMethod("orientation").invoke(pose);
        Object rotationPointObject = pose.getClass().getMethod("rotationPoint").invoke(pose);
        Object scaleObject = pose.getClass().getMethod("scale").invoke(pose);
        if (!(positionObject instanceof Vector3dc position)
                || !(orientationObject instanceof Quaterniondc orientation)
                || !(rotationPointObject instanceof Vector3dc rotationPoint)
                || !(scaleObject instanceof Vector3dc scale)) {
            return null;
        }
        return new PoseBasisSnapshot(
                new Vector3d(position),
                new Quaterniond(orientation),
                new Vector3d(rotationPoint),
                new Vector3d(scale)
        );
    }

    /**
     * Restores the center-of-mass basis transition lost when {@code buildMassTracker()} replaces
     * Sable's {@code MergedMassTracker}. The replacement tracker sets its previous center equal to
     * its newly rebuilt center before publishing, so Sable has already assigned the new render
     * pivot but has teleported the native body by a zero delta. Use the basis captured before the
     * replacement to perform exactly that omitted native/logical transition and to keep the
     * previous pose interpolation-equivalent.
     */
    private static void bitsandbalance$preservePoseAfterMassTrackerRebuild(Object pipeline,
                                                                           Object subLevel,
                                                                           @Nullable SubLevelPoseBasisSnapshot before) {
        if (pipeline == null || subLevel == null || before == null) {
            return;
        }

        try {
            Object massData = subLevel.getClass().getMethod("getMassTracker").invoke(subLevel);
            Object centerObject = massData == null
                    ? null
                    : massData.getClass().getMethod("getCenterOfMass").invoke(massData);
            if (!(centerObject instanceof Vector3dc centerOfMass)) {
                return;
            }

            Vector3d newCenter = new Vector3d(centerOfMass);
            Object logicalPose = subLevel.getClass().getMethod("logicalPose").invoke(subLevel);
            Object logicalPositionObject = logicalPose.getClass().getMethod("position").invoke(logicalPose);
            Object logicalOrientationObject = logicalPose.getClass().getMethod("orientation").invoke(logicalPose);
            Object logicalRotationPointObject = logicalPose.getClass().getMethod("rotationPoint").invoke(logicalPose);
            Object logicalScaleObject = logicalPose.getClass().getMethod("scale").invoke(logicalPose);
            if (!(logicalPositionObject instanceof Vector3d logicalPosition)
                    || !(logicalOrientationObject instanceof Quaterniondc logicalOrientation)
                    || !(logicalRotationPointObject instanceof Vector3d logicalRotationPoint)
                    || !(logicalScaleObject instanceof Vector3dc logicalScale)) {
                return;
            }

            Vector3d localDelta = new Vector3d(newCenter).sub(before.logical().rotationPoint());
            localDelta.mul(logicalScale);
            logicalOrientation.transform(localDelta);
            Vector3d originalPosition = new Vector3d(logicalPosition);
            Vector3d targetPosition = new Vector3d(originalPosition).add(localDelta);

            Method teleport = bitsandbalance$findPoseTeleport(
                    pipeline,
                    subLevel,
                    targetPosition,
                    logicalOrientation
            );
            Vector3d angularVelocity = new Vector3d();
            Method getAngularVelocity = bitsandbalance$findPoseVectorMethod(
                    pipeline,
                    "getAngularVelocity",
                    subLevel,
                    angularVelocity
            );
            if (teleport == null || getAngularVelocity == null) {
                return;
            }

            Object angularResult = getAngularVelocity.invoke(pipeline, subLevel, angularVelocity);
            if (angularResult instanceof Vector3dc returnedAngularVelocity) {
                angularVelocity.set(returnedAngularVelocity);
            }
            Vector3d linearVelocityCorrection = angularVelocity.cross(localDelta, new Vector3d());
            Vector3d zeroAngularVelocity = new Vector3d();
            Method addVelocity = linearVelocityCorrection.lengthSquared() == 0.0D
                    ? null
                    : bitsandbalance$findPoseVelocityAdder(
                            pipeline,
                            subLevel,
                            linearVelocityCorrection,
                            zeroAngularVelocity
                    );
            if (linearVelocityCorrection.lengthSquared() != 0.0D && addVelocity == null) {
                return;
            }

            teleport.invoke(pipeline, subLevel, targetPosition, logicalOrientation);
            if (addVelocity != null) {
                try {
                    addVelocity.invoke(
                            pipeline,
                            subLevel,
                            linearVelocityCorrection,
                            zeroAngularVelocity
                    );
                } catch (ReflectiveOperationException | LinkageError | RuntimeException velocityFailure) {
                    try {
                        teleport.invoke(pipeline, subLevel, originalPosition, logicalOrientation);
                    } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                    }
                    throw velocityFailure;
                }
            }

            logicalPosition.set(targetPosition);
            logicalRotationPoint.set(newCenter);

            Object lastPose = subLevel.getClass().getMethod("lastPose").invoke(subLevel);
            Object lastPositionObject = lastPose.getClass().getMethod("position").invoke(lastPose);
            Object lastRotationPointObject = lastPose.getClass().getMethod("rotationPoint").invoke(lastPose);
            if (lastPositionObject instanceof Vector3d lastPosition
                    && lastRotationPointObject instanceof Vector3d lastRotationPoint) {
                Vector3d lastDelta = new Vector3d(newCenter).sub(before.last().rotationPoint());
                lastDelta.mul(before.last().scale());
                before.last().orientation().transform(lastDelta);
                lastPosition.set(before.last().position()).add(lastDelta);
                lastRotationPoint.set(newCenter);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    private static @Nullable Method bitsandbalance$findPoseTeleport(Object pipeline,
                                                                    Object subLevel,
                                                                    Vector3dc position,
                                                                    Quaterniondc orientation) {
        for (Method method : pipeline.getClass().getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getName().equals("teleport")
                    && parameterTypes.length == 3
                    && parameterTypes[0].isInstance(subLevel)
                    && parameterTypes[1].isInstance(position)
                    && parameterTypes[2].isInstance(orientation)) {
                return method;
            }
        }
        return null;
    }

    private static @Nullable Method bitsandbalance$findPoseVectorMethod(Object pipeline,
                                                                        String methodName,
                                                                        Object subLevel,
                                                                        Vector3d destination) {
        for (Method method : pipeline.getClass().getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getName().equals(methodName)
                    && parameterTypes.length == 2
                    && parameterTypes[0].isInstance(subLevel)
                    && parameterTypes[1].isInstance(destination)) {
                return method;
            }
        }
        return null;
    }

    private static @Nullable Method bitsandbalance$findPoseVelocityAdder(Object pipeline,
                                                                         Object subLevel,
                                                                         Vector3dc linearVelocity,
                                                                         Vector3dc angularVelocity) {
        for (Method method : pipeline.getClass().getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getName().equals("addLinearAndAngularVelocity")
                    && parameterTypes.length == 3
                    && parameterTypes[0].isInstance(subLevel)
                    && parameterTypes[1].isInstance(linearVelocity)
                    && parameterTypes[2].isInstance(angularVelocity)) {
                return method;
            }
        }
        return null;
    }

    private record PoseBasisSnapshot(Vector3d position,
                                     Quaterniond orientation,
                                     Vector3d rotationPoint,
                                     Vector3d scale) {
    }

    private record SubLevelPoseBasisSnapshot(PoseBasisSnapshot logical, PoseBasisSnapshot last) {
    }

    private static @Nullable PlotChunk plotChunk(Level level, BlockPos pos) throws ReflectiveOperationException {
        Object subLevel = getContainingByPos.invoke(helper, level, pos);
        if (subLevel == null) {
            return null;
        }

        Method plotMethod = getSubLevelGetPlotMethod(subLevel.getClass());
        if (plotMethod == null) {
            return null;
        }
        Object plot = plotMethod.invoke(subLevel);
        if (plot == null) {
            return null;
        }

        ChunkPos globalChunkPos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        Method toLocal = plot.getClass().getMethod("toLocal", ChunkPos.class);
        Object localChunkPos = toLocal.invoke(plot, globalChunkPos);
        ChunkPos lookupChunkPos = localChunkPos instanceof ChunkPos chunkPos ? chunkPos : globalChunkPos;
        Object holder = null;
        try {
            Method getChunkHolder = plot.getClass().getMethod("getChunkHolder", ChunkPos.class);
            holder = getChunkHolder.invoke(plot, lookupChunkPos);
        } catch (NoSuchMethodException ignored) {
        }

        Object chunkObject;
        if (holder == null) {
            Method getChunk = plot.getClass().getMethod("getChunk", ChunkPos.class);
            chunkObject = getChunk.invoke(plot, lookupChunkPos);
        } else {
            Method getChunk = holder.getClass().getMethod("getChunk");
            chunkObject = getChunk.invoke(holder);
        }
        return chunkObject instanceof LevelChunk chunk ? new PlotChunk(plot, holder, chunk) : null;
    }

    private static boolean setChunkBlockState(LevelChunk chunk, BlockPos pos, BlockState state) throws ReflectiveOperationException {
        try {
            Method method = LevelChunk.class.getMethod("setBlockState", BlockPos.class, BlockState.class, boolean.class);
            Object previous = method.invoke(chunk, pos, state, true);
            return previous instanceof BlockState || chunk.getBlockState(pos) == state;
        } catch (NoSuchMethodException ignored) {
            Method method = LevelChunk.class.getMethod("setBlockState", BlockPos.class, BlockState.class, int.class);
            Object previous = method.invoke(chunk, pos, state, 0);
            return previous instanceof BlockState || chunk.getBlockState(pos) == state;
        }
    }

    private static void notifyPlotBlockChange(Object plot,
                                              @Nullable Object holder,
                                              LevelChunk chunk,
                                              Level level,
                                              BlockPos pos,
                                              BlockState oldState,
                                              BlockState newState,
                                              int flags) {
        boolean occupancyChanged = oldState.isAir() != newState.isAir();
        if (occupancyChanged) {
            try {
                if (holder != null) {
                    Method handleBlockChange = holder.getClass().getMethod(
                            "handleBlockChange",
                            int.class,
                            int.class,
                            int.class,
                            BlockState.class,
                            BlockState.class
                    );
                    handleBlockChange.invoke(holder, pos.getX() & 15, pos.getY(), pos.getZ() & 15, oldState, newState);
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            }

            try {
                Method updateBoundingBox = plot.getClass().getMethod("updateBoundingBox");
                updateBoundingBox.invoke(plot);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            }
        }

        try {
            if (holder != null) {
                Method blockChanged = holder.getClass().getMethod("blockChanged", BlockPos.class);
                blockChanged.invoke(holder, pos);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }

        markChunkUnsaved(chunk);

        try {
            level.sendBlockUpdated(pos, oldState, newState, flags);
        } catch (RuntimeException ignored) {
        }
    }

    /**
     * Wakes the exact Sable body which owns a successful plot write.
     * This intentionally does not replay the block change: Sable has already updated mass,
     * connectivity, and Rapier voxel data, while waking the body is idempotent.
     */
    private static void wakeOwningPlotSubLevel(Level level, Object plot) {
        recordEnhancedSlabDirectWakeAttempt();
        if (!(level instanceof ServerLevel) || plot == null) {
            return;
        }

        try {
            Object subLevel = plot.getClass().getMethod("getSubLevel").invoke(plot);
            if (subLevel == null) {
                return;
            }

            ClassLoader loader = SableCompat.class.getClassLoader();
            Class<?> physicsSystemClass = Class.forName(
                    "dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem",
                    false,
                    loader
            );
            Object physicsSystem = physicsSystemClass.getMethod("get", Level.class).invoke(null, level);
            Object pipeline = physicsSystem == null
                    ? null
                    : physicsSystemClass.getMethod("getPipeline").invoke(physicsSystem);
            if (pipeline == null) {
                return;
            }

            Method wakeUp = null;
            for (Method method : pipeline.getClass().getMethods()) {
                if (method.getName().equals("wakeUp") && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isInstance(subLevel)) {
                    wakeUp = method;
                    break;
                }
            }
            if (wakeUp == null) {
                return;
            }
            wakeUp.invoke(pipeline, subLevel);
            recordEnhancedSlabDirectWakeSuccess();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    /**
     * Rebuilds Sable's complete mass tracker after position-backed materials change without a
     * corresponding {@link BlockState} change.
     *
     * <p>Sable's ordinary incremental block-change path resolves both the old and new mass after
     * the saved material slots have already changed. For a Quad Step whose owner state remains the
     * same, those two lookups are therefore identical and Sable returns without updating mass,
     * center of mass, or inertia. Rebuilding from the live plot makes the stored occupied quarters
     * authoritative, then publishes the rebuilt merged properties to the active physics body.
     * All normal Sable mass and inertia property overrides remain in force because this delegates
     * to {@code ServerSubLevel.buildMassTracker()} and the normal pipeline stats update.</p>
     */
    private static void rebuildOwningPlotMass(Level level, Object plot) {
        if (!(level instanceof ServerLevel) || plot == null) {
            return;
        }

        try {
            Object subLevel = plot.getClass().getMethod("getSubLevel").invoke(plot);
            if (subLevel == null) {
                return;
            }

            SubLevelPoseBasisSnapshot poseBeforeRebuild = bitsandbalance$captureSubLevelPoseBasis(subLevel);
            boolean preservedMergedHistory = bitsandbalance$rebuildSelfMassPreservingMergedHistory(subLevel);

            Class<?> physicsSystemClass = Class.forName(
                    "dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem",
                    false,
                    SableCompat.class.getClassLoader()
            );
            Object physicsSystem = physicsSystemClass.getMethod("get", Level.class).invoke(null, level);
            if (physicsSystem == null) {
                return;
            }

            Object partialTickValue = physicsSystemClass.getMethod("getPartialPhysicsTick").invoke(physicsSystem);
            float partialTick = partialTickValue instanceof Number number ? number.floatValue() : 0.0F;
            subLevel.getClass().getMethod("updateMergedMassData", float.class).invoke(subLevel, partialTick);

            Object pipeline = physicsSystemClass.getMethod("getPipeline").invoke(physicsSystem);
            if (pipeline == null) {
                return;
            }
            // The in-place rebuild leaves MergedMassTracker's previous mass, COM, and inertia
            // history intact, so its normal upload performs the complete pose-basis transition.
            // Retain the old compensation only as a compatibility fallback for an unsupported
            // Sable layout where that history could not be preserved.
            if (!preservedMergedHistory) {
                bitsandbalance$preservePoseAfterMassTrackerRebuild(
                        pipeline,
                        subLevel,
                        poseBeforeRebuild
                );
            }
            if (FIXTURE_DIAGNOSTICS) {
                enhancedSlabMassTrackerRebuildCount++;
                if (preservedMergedHistory) {
                    enhancedSlabMergedHistoryReuseCount++;
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    /**
     * Rebuilds Sable's self mass while retaining the live {@code MergedMassTracker} instance.
     *
     * <p>{@code ServerSubLevel.buildMassTracker()} constructs both a fresh self tracker and a fresh
     * merged tracker. The latter has no previous COM/inertia history, so its first update cannot
     * perform Sable's normal pose-basis transition. Build the fresh pair, transfer the complete
     * self-tracker state into the existing self tracker, and reinstall the existing merged tracker
     * before it is updated. The temporary merged tracker is never published to the pipeline.</p>
     */
    private static boolean bitsandbalance$rebuildSelfMassPreservingMergedHistory(Object subLevel)
            throws ReflectiveOperationException {
        Method getMassTracker = subLevel.getClass().getMethod("getMassTracker");
        Method buildMassTracker = subLevel.getClass().getMethod("buildMassTracker");
        Object previousMerged = getMassTracker.invoke(subLevel);
        Object previousSelf = null;
        try {
            previousSelf = bitsandbalance$selfMassTracker(previousMerged);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Still build a complete fresh tracker below so the caller can use its pose fallback.
        }
        boolean previousHistoryInitialized = bitsandbalance$hasInitializedMergedMassHistory(previousMerged);

        buildMassTracker.invoke(subLevel);
        Object rebuiltMerged = getMassTracker.invoke(subLevel);
        Object rebuiltSelf;
        try {
            rebuiltSelf = bitsandbalance$selfMassTracker(rebuiltMerged);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
        if (!previousHistoryInitialized || previousMerged == null || previousSelf == null || rebuiltSelf == null
                || previousSelf.getClass() != rebuiltSelf.getClass()) {
            return false;
        }

        try {
            bitsandbalance$copyMassTrackerState(rebuiltSelf, previousSelf);
            Field mergedField = bitsandbalance$findField(subLevel.getClass(), "massTracker");
            if (mergedField == null || !mergedField.getType().isInstance(previousMerged)) {
                return false;
            }
            // Install the history-bearing tracker only after its self mass is complete. Until this
            // write, the newly built tracker remains the valid fallback on the sublevel.
            mergedField.set(subLevel, previousMerged);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static @Nullable Object bitsandbalance$selfMassTracker(@Nullable Object merged)
            throws ReflectiveOperationException {
        return merged == null ? null : merged.getClass().getMethod("getSelfMassTracker").invoke(merged);
    }

    private static boolean bitsandbalance$hasInitializedMergedMassHistory(@Nullable Object merged) {
        if (merged == null) {
            return false;
        }

        try {
            Field centerField = bitsandbalance$findField(merged.getClass(), "lastCenterOfMass");
            Field inertiaField = bitsandbalance$findField(merged.getClass(), "lastInertiaTensor");
            return centerField != null
                    && inertiaField != null
                    && centerField.get(merged) != null
                    && inertiaField.get(merged) != null;
        } catch (IllegalAccessException | RuntimeException ignored) {
            return false;
        }
    }

    private static void bitsandbalance$copyMassTrackerState(Object source, Object target)
            throws ReflectiveOperationException {
        for (String fieldName : new String[]{
                "mass",
                "inertiaTensor",
                "inverseMass",
                "inverseInertiaTensor",
                "centerOfMass"
        }) {
            Field field = bitsandbalance$findField(source.getClass(), fieldName);
            if (field == null || !field.getDeclaringClass().isInstance(target)) {
                throw new NoSuchFieldException(fieldName);
            }
            field.set(target, field.get(source));
        }
    }

    private static @Nullable Field bitsandbalance$findField(Class<?> owner, String name) {
        Class<?> type = owner;
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Finalizes an enhanced-slab split successor before Sable performs its zero-mass cleanup.
     *
     * <p>{@code SubLevelAssemblyHelper.moveBlocks} copies block entities before it returns, but
     * position-backed material data and the mass scan can still lag behind the new plot. Sable
     * immediately destroys the entire successor when its self tracker has no center or mass. Work
     * from the returned successor itself, recover every copied Step payload, then rebuild its heat
     * map and mass once while the plot is still guaranteed to exist.</p>
     */
    public static void reconcileAssembledEnhancedSlabSubLevelBeforeValidation(Object successor) {
        if (successor == null) {
            return;
        }
        if (FIXTURE_DIAGNOSTICS) {
            enhancedSlabSuccessorReconciliationAttemptCount++;
        }

        try {
            Method levelMethod = getSubLevelGetLevelMethod(successor.getClass());
            Method plotMethod = getSubLevelGetPlotMethod(successor.getClass());
            Object levelObject = levelMethod == null ? null : levelMethod.invoke(successor);
            Object plot = plotMethod == null ? null : plotMethod.invoke(successor);
            if (!(levelObject instanceof Level level) || plot == null) {
                if (FIXTURE_DIAGNOSTICS) enhancedSlabSuccessorReconciliationFailureCount++;
                return;
            }

            Method boundsMethod = getPlotGetBoundingBoxMethod(plot.getClass());
            Object bounds = boundsMethod == null ? null : boundsMethod.invoke(plot);
            if (bounds == null) {
                if (FIXTURE_DIAGNOSTICS) enhancedSlabSuccessorReconciliationFailureCount++;
                return;
            }

            int minX = invokeInt(bounds, "minX");
            int minY = invokeInt(bounds, "minY");
            int minZ = invokeInt(bounds, "minZ");
            int maxX = invokeInt(bounds, "maxX");
            int maxY = invokeInt(bounds, "maxY");
            int maxZ = invokeInt(bounds, "maxZ");
            long volume = (long) (maxX - minX + 1)
                    * (long) (maxY - minY + 1)
                    * (long) (maxZ - minZ + 1);
            if (maxX < minX || maxY < minY || maxZ < minZ
                    || volume <= 0L || volume > MAX_HEATMAP_REBUILD_VOLUME) {
                if (FIXTURE_DIAGNOSTICS) enhancedSlabSuccessorReconciliationFailureCount++;
                return;
            }

            boolean restoredEnhancedSlab = false;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (!EnhancedSlabCreateCompat.supports(state)) {
                            continue;
                        }
                        if (FIXTURE_DIAGNOSTICS) enhancedSlabSuccessorSupportedStateCount++;

                        PlotChunk plotChunk = plotChunk(level, pos);
                        if (plotChunk == null || plotChunk.plot() != plot) {
                            continue;
                        }

                        BlockState[] slots = EnhancedSlabCreateCompat.captureMaterialSlots(level, pos, state);
                        if (!EnhancedSlabCreateCompat.hasMaterialSlots(slots)) {
                            continue;
                        }

                        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), slots);
                        hydrateEnhancedSlabBlockEntity(plotChunk.chunk().getBlockEntity(pos), slots);
                        refreshPhysicsCollider(level, plotChunk.chunk(), pos, state);
                        restoredEnhancedSlab = true;
                        if (FIXTURE_DIAGNOSTICS) enhancedSlabSuccessorRestoredSlotCount++;
                    }
                }
            }

            if (!restoredEnhancedSlab) {
                if (FIXTURE_DIAGNOSTICS) enhancedSlabSuccessorReconciliationFailureCount++;
                return;
            }

            Method heatMapMethod = getHeatMapManagerMethod(successor.getClass());
            Object heatMap = heatMapMethod == null ? null : heatMapMethod.invoke(successor);
            if (heatMap != null) {
                rebuildHeatMap(successor, heatMap);
            }
            rebuildOwningPlotMass(level, plot);
            if (FIXTURE_DIAGNOSTICS) {
                Object selfTracker = successor.getClass().getMethod("getSelfMassTracker").invoke(successor);
                Object massValue = selfTracker == null
                        ? null
                        : selfTracker.getClass().getMethod("getMass").invoke(selfTracker);
                enhancedSlabSuccessorLastMass = massValue instanceof Number number
                        ? number.doubleValue()
                        : 0.0D;
                if (enhancedSlabSuccessorLastMass > 0.0D) {
                    enhancedSlabSuccessorPositiveMassCount++;
                } else {
                    enhancedSlabSuccessorReconciliationFailureCount++;
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            if (FIXTURE_DIAGNOSTICS) enhancedSlabSuccessorReconciliationFailureCount++;
        }
    }

    /**
     * Reconciles the original Sable body retained after an asynchronous heat-map split when that
     * body still contains an enhanced slab. Detached components pass through {@code moveBlocks}
     * and receive a finished-plot collider and mass rebuild from our assembly hook. The retained
     * component does not, so a large fracture can otherwise leave its aggregate mass properties
     * at an intermediate removal state even though each Step's own mass data is correct.
     */
    public static void reconcileRetainedEnhancedSlabSubLevelAfterSplit(Object heatMapManager) {
        if (heatMapManager == null) {
            return;
        }

        try {
            Field field = getHeatMapSubLevelField(heatMapManager.getClass());
            Object subLevel = field == null ? null : field.get(heatMapManager);
            if (subLevel == null) {
                return;
            }

            Method levelMethod = getSubLevelGetLevelMethod(subLevel.getClass());
            Method plotMethod = getSubLevelGetPlotMethod(subLevel.getClass());
            Object levelObject = levelMethod == null ? null : levelMethod.invoke(subLevel);
            Object plot = plotMethod == null ? null : plotMethod.invoke(subLevel);
            if (!(levelObject instanceof Level level) || plot == null) {
                return;
            }

            Method boundsMethod = getPlotGetBoundingBoxMethod(plot.getClass());
            Object bounds = boundsMethod == null ? null : boundsMethod.invoke(plot);
            if (bounds == null) {
                return;
            }

            int minX = invokeInt(bounds, "minX");
            int minY = invokeInt(bounds, "minY");
            int minZ = invokeInt(bounds, "minZ");
            int maxX = invokeInt(bounds, "maxX");
            int maxY = invokeInt(bounds, "maxY");
            int maxZ = invokeInt(bounds, "maxZ");
            long volume = (long) (maxX - minX + 1)
                    * (long) (maxY - minY + 1)
                    * (long) (maxZ - minZ + 1);
            if (maxX < minX || maxY < minY || maxZ < minZ
                    || volume <= 0L || volume > MAX_HEATMAP_REBUILD_VOLUME) {
                return;
            }

            boolean containsEnhancedSlab = false;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (!EnhancedSlabCreateCompat.supports(state)) {
                            continue;
                        }

                        containsEnhancedSlab = true;
                        try {
                            PlotChunk plotChunk = plotChunk(level, pos);
                            if (plotChunk != null && plotChunk.plot() == plot) {
                                refreshPhysicsCollider(level, plotChunk.chunk(), pos, state);
                            }
                        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                        }
                    }
                }
            }

            if (containsEnhancedSlab) {
                rebuildOwningPlotMass(level, plot);
                wakeOwningPlotSubLevel(level, plot);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    /**
     * Material restoration leaves the owner BlockState unchanged, so Sable's LevelChunk hook does
     * not notify Rapier. Explicitly replay the same-state change after SavedData is available so
     * the position-aware enhanced-slab collider is baked and the body is awakened.
     */
    private static void refreshPhysicsCollider(Level level,
                                               LevelChunk chunk,
                                               BlockPos pos,
                                               BlockState state) {
        if (!(level instanceof ServerLevel) || chunk == null || pos == null || state == null) {
            return;
        }

        try {
            Class<?> physicsSystemClass = Class.forName(
                    "dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem",
                    false,
                    SableCompat.class.getClassLoader()
            );
            Method get = physicsSystemClass.getMethod("get", Level.class);
            Object physicsSystem = get.invoke(null, level);
            if (physicsSystem == null) {
                return;
            }

            int sectionIndex = chunk.getSectionIndex(pos.getY());
            LevelChunkSection section = chunk.getSection(sectionIndex);
            Method handleBlockChange = physicsSystemClass.getMethod(
                    "handleBlockChange",
                    SectionPos.class,
                    LevelChunkSection.class,
                    int.class,
                    int.class,
                    int.class,
                    BlockState.class,
                    BlockState.class
            );
            handleBlockChange.invoke(
                    physicsSystem,
                    SectionPos.of(pos),
                    section,
                    pos.getX() & 15,
                    pos.getY() & 15,
                    pos.getZ() & 15,
                    state,
                    state
            );
            if (FIXTURE_DIAGNOSTICS) {
                physicsColliderRefreshCount++;
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    private static void hydrateEnhancedSlabBlockEntity(@Nullable BlockEntity blockEntity, BlockState[] slots) {
        if (blockEntity == null || slots == null || slots.length == 0) {
            return;
        }

        try {
            if (blockEntity instanceof StepBlockEntity step && slots[0] != null) {
                step.setSlabState(slots[0]);
                return;
            }
            if (blockEntity instanceof VerticalStepBlockEntity verticalStep && slots[0] != null) {
                verticalStep.setSlabState(slots[0]);
                return;
            }
            if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab && slots[0] != null) {
                if (slots.length > 1 && slots[1] != null) {
                    verticalSlab.setSlabs(slots[0], slots[1]);
                } else {
                    verticalSlab.setSingleFacingSlab(slots[0]);
                }
                return;
            }
            if (blockEntity instanceof QuadStepBlockEntity quadStep) {
                quadStep.bitsandbalance$setAllSlabsSilent(slots);
                quadStep.bitsandbalance$syncToClients();
                return;
            }
            if (blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep) {
                quadVerticalStep.bitsandbalance$setAllSlabsSilent(slots);
                quadVerticalStep.bitsandbalance$syncToClients();
            }
        } catch (RuntimeException ignored) {
        }
    }

    public static void clearPlotEnhancedSlabMaterials(Object plot) {
        if (plot == null) {
            return;
        }

        try {
            Method getSubLevel = plot.getClass().getMethod("getSubLevel");
            Object subLevel = getSubLevel.invoke(plot);
            if (subLevel == null) {
                return;
            }

            Method getLevel = subLevel.getClass().getMethod("getLevel");
            Object levelObject = getLevel.invoke(subLevel);
            if (!(levelObject instanceof ServerLevel level)) {
                return;
            }

            Method getBoundingBox = plot.getClass().getMethod("getBoundingBox");
            Object bounds = getBoundingBox.invoke(plot);
            if (bounds == null || "EMPTY".equals(String.valueOf(bounds))) {
                return;
            }

            int minX = invokeInt(bounds, "minX");
            int minY = invokeInt(bounds, "minY");
            int minZ = invokeInt(bounds, "minZ");
            int maxX = invokeInt(bounds, "maxX");
            int maxY = invokeInt(bounds, "maxY");
            int maxZ = invokeInt(bounds, "maxZ");
            long volume = (long) (maxX - minX + 1) * (long) (maxY - minY + 1) * (long) (maxZ - minZ + 1);
            if (volume <= 0 || volume > MAX_PLOT_MATERIAL_CLEAR_VOLUME) {
                return;
            }

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        long posLong = BlockPos.asLong(x, y, z);
                        EnhancedSlabMaterialSavedData.remember(level, posLong);
                        EnhancedSlabMaterialSavedData.removeClient(level, posLong);
                    }
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    public static void notifyEnhancedSlabPartialRemoval(Level level, BlockPos pos) {
        if (level == null || pos == null || !initialize()) {
            return;
        }

        try {
            Object subLevel = getContainingByPos.invoke(helper, level, pos);
            if (subLevel == null) {
                return;
            }

            Method method = getHeatMapManagerMethod(subLevel.getClass());
            if (method == null) {
                return;
            }

            Object heatMap = method.invoke(subLevel);
            if (heatMap == null) {
                return;
            }

            Method removed = getHeatMapOnSolidRemovedMethod(heatMap.getClass());
            if (removed != null && !rebuildHeatMap(subLevel, heatMap, pos)) {
                forgetHeatMapComponents(heatMap, pos);
                removed.invoke(heatMap, pos);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    /**
     * Rebuilds Sable's connectivity heat map after position-backed enhanced-slab material data
     * has been restored to a plot. Sable records moved block states before that data is available,
     * so its first shape-aware connectivity pass can otherwise retain a false split candidate.
     */
    public static boolean rebuildEnhancedSlabHeatMap(Level level, BlockPos plotPos) {
        if (level == null || plotPos == null || !initialize()) {
            return false;
        }

        try {
            Object subLevel = getContainingByPos.invoke(helper, level, plotPos);
            if (subLevel == null) {
                return false;
            }

            Method method = getHeatMapManagerMethod(subLevel.getClass());
            if (method == null) {
                return false;
            }

            Object heatMap = method.invoke(subLevel);
            return heatMap != null && rebuildHeatMap(subLevel, heatMap);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    public static boolean isEnhancedSlabHeatMapConnected(Object heatMapManager, BlockPos sourcePos, BlockPos targetPos, boolean originalSolid) {
        if (heatMapManager == null || sourcePos == null || targetPos == null) {
            return originalSolid;
        }

        Level level = getHeatMapLevel(heatMapManager);
        if (level == null) {
            return originalSolid;
        }

        BlockState sourceState = level.getBlockState(sourcePos);
        BlockState targetState = level.getBlockState(targetPos);
        boolean sourceEnhanced = supportsEnhancedSlabShape(sourceState);
        boolean targetEnhanced = supportsEnhancedSlabShape(targetState);
        if (!originalSolid) {
            return false;
        }
        if (!sourceEnhanced && !targetEnhanced) {
            return originalSolid;
        }

        Vec3i offset = connectionOffset(sourcePos, targetPos);
        if (offset == null) {
            return false;
        }

        return hasSharedOccupiedConnection(
                level,
                heatMapManager,
                sourcePos,
                sourceState,
                sourceEnhanced,
                targetPos,
                targetState,
                targetEnhanced,
                offset
        );
    }

    public static Object gatherConnectedBlocksShapeAware(BlockPos gatherOrigin, ServerLevel level, int maximumBlocksToAssemble, Object frontierPredicate) {
        if (gatherOrigin == null || level == null) {
            return null;
        }

        try {
            BlockState gatherOriginState = level.getBlockState(gatherOrigin);
            if (gatherOriginState.isAir()) {
                return newSableGatherResult(null, 0, null, "NO_BLOCKS");
            }

            ArrayDeque<GatherEntry> frontier = new ArrayDeque<>(1 << 12);
            Set<BlockPos> queued = new LinkedHashSet<>(1 << 12);
            Set<BlockPos> blocks = new LinkedHashSet<>(1 << 10);

            BlockPos origin = gatherOrigin.immutable();
            frontier.add(new GatherEntry(origin, gatherOriginState));
            queued.add(origin);

            int minX = gatherOrigin.getX();
            int minY = gatherOrigin.getY();
            int minZ = gatherOrigin.getZ();
            int maxX = gatherOrigin.getX();
            int maxY = gatherOrigin.getY();
            int maxZ = gatherOrigin.getZ();
            int blockCount = 0;

            while (!frontier.isEmpty()) {
                GatherEntry entry = frontier.removeFirst();
                BlockPos pos = entry.pos();

                blockCount++;
                if (blockCount > maximumBlocksToAssemble) {
                    return newSableGatherResult(null, blockCount, null, "TOO_MANY_BLOCKS");
                }

                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());

                blocks.add(pos);

                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) {
                                continue;
                            }

                            int axes = Math.abs(x) + Math.abs(y) + Math.abs(z);
                            if (axes == 3) {
                                continue;
                            }

                            BlockPos candidate = pos.offset(x, y, z).immutable();
                            if (queued.contains(candidate) || blocks.contains(candidate)) {
                                continue;
                            }

                            Direction direction = axes == 1 ? directionFromOffset(new Vec3i(x, y, z)) : null;
                            BlockState candidateState = level.getBlockState(candidate);
                            if (candidateState.isAir()) {
                                continue;
                            }

                            if (!isEnhancedSlabAssemblyConnected(level, pos, entry.state(), candidate, candidateState)) {
                                continue;
                            }

                            if (!isValidSableFrontierConnection(frontierPredicate, pos, entry.state(), candidate, candidateState, direction)) {
                                continue;
                            }

                            queued.add(candidate);
                            frontier.add(new GatherEntry(candidate, candidateState));
                        }
                    }
                }
            }

            Object bounds = newSableBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
            if (blocks.isEmpty()) {
                return newSableGatherResult(null, blockCount, null, "NO_BLOCKS");
            }
            return newSableGatherResult(blocks, blockCount, bounds, "SUCCESS");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    public static boolean isEnhancedSlabAssemblyConnected(Level level,
                                                          BlockPos sourcePos,
                                                          BlockState sourceState,
                                                          BlockPos targetPos,
                                                          BlockState targetState) {
        if (level == null || sourcePos == null || sourceState == null || targetPos == null || targetState == null) {
            return true;
        }

        boolean sourceEnhanced = supportsEnhancedSlabShape(sourceState);
        boolean targetEnhanced = supportsEnhancedSlabShape(targetState);
        if (!sourceEnhanced && !targetEnhanced) {
            return true;
        }
        // Shape-aware slab support is not glue; terrain touching a step should stay put.
        if (sourceEnhanced != targetEnhanced) {
            if (!isSimulatedPhysicsAssembler(sourceState) && !isSimulatedPhysicsAssembler(targetState)) {
                return false;
            }
        }

        Vec3i offset = connectionOffset(sourcePos, targetPos);
        if (offset == null) {
            return false;
        }

        return hasSharedOccupiedConnection(
                level,
                null,
                sourcePos,
                sourceState,
                sourceEnhanced,
                targetPos,
                targetState,
                targetEnhanced,
                offset
        );
    }

    private static boolean isSimulatedPhysicsAssembler(BlockState state) {
        if (state == null) {
            return false;
        }
        return "dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlock"
                .equals(state.getBlock().getClass().getName());
    }

    private static boolean isValidSableFrontierConnection(Object frontierPredicate,
                                                          BlockPos originPos,
                                                          BlockState originState,
                                                          BlockPos pos,
                                                          BlockState state,
                                                          Direction directionFrom) throws ReflectiveOperationException {
        if (frontierPredicate == null) {
            return true;
        }

        Method method = frontierPredicate.getClass().getMethod(
                "isValidConnection",
                BlockPos.class,
                BlockState.class,
                BlockPos.class,
                BlockState.class,
                Direction.class
        );
        method.setAccessible(true);
        Object value = method.invoke(frontierPredicate, originPos, originState, pos, state, directionFrom);
        if (value instanceof Boolean valid) {
            return valid;
        }
        throw new ReflectiveOperationException("Sable FrontierPredicate returned a non-boolean value");
    }

    private static Object newSableBoundingBox(int minX,
                                              int minY,
                                              int minZ,
                                              int maxX,
                                              int maxY,
                                              int maxZ) throws ReflectiveOperationException {
        Class<?> boundsClass = Class.forName("dev.ryanhcode.sable.companion.math.BoundingBox3i");
        Constructor<?> constructor = boundsClass.getConstructor(
                int.class,
                int.class,
                int.class,
                int.class,
                int.class,
                int.class
        );
        return constructor.newInstance(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static Object newSableGatherResult(Set<BlockPos> blocks,
                                               int checkedBlocks,
                                               Object boundingBox,
                                               String stateName) throws ReflectiveOperationException {
        Class<?> boundsClass = Class.forName("dev.ryanhcode.sable.companion.math.BoundingBox3i");
        Class<?> stateClass = Class.forName("dev.ryanhcode.sable.api.SubLevelAssemblyHelper$GatherResult$State");
        Class<?> resultClass = Class.forName("dev.ryanhcode.sable.api.SubLevelAssemblyHelper$GatherResult");
        Object state = Enum.valueOf(stateClass.asSubclass(Enum.class), stateName);
        Constructor<?> constructor = resultClass.getConstructor(Set.class, int.class, boundsClass, stateClass);
        return constructor.newInstance(blocks, checkedBlocks, boundingBox, state);
    }

    private static boolean initialize() {
        if (initialized) {
            return available;
        }

        synchronized (SableCompat.class) {
            if (initialized) {
                return available;
            }

            try {
                Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable");
                Field helperField = sableClass.getField("HELPER");
                Object helperInstance = helperField.get(null);
                Method plotGridMethod = helperInstance.getClass().getMethod("isInPlotGrid", Level.class, int.class, int.class);
                Method containingMethod = helperInstance.getClass().getMethod("getContaining", Level.class, Vec3i.class);

                helper = helperInstance;
                isInPlotGrid = plotGridMethod;
                getContainingByPos = containingMethod;
                available = true;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                available = false;
            }

            initialized = true;
            return available;
        }
    }

    private static Level getHeatMapLevel(Object heatMapManager) {
        try {
            Field field = getHeatMapSubLevelField(heatMapManager.getClass());
            if (field == null) {
                return null;
            }

            Object subLevel = field.get(heatMapManager);
            if (subLevel == null) {
                return null;
            }

            Method method = getSubLevelGetLevelMethod(subLevel.getClass());
            Object level = method == null ? null : method.invoke(subLevel);
            return level instanceof Level actualLevel ? actualLevel : null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static Field getHeatMapSubLevelField(Class<?> owner) {
        if (heatMapSubLevelInitialized) {
            return heatMapSubLevel;
        }

        synchronized (SableCompat.class) {
            if (heatMapSubLevelInitialized) {
                return heatMapSubLevel;
            }

            try {
                Field field = owner.getDeclaredField("subLevel");
                field.setAccessible(true);
                heatMapSubLevel = field;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                heatMapSubLevel = null;
            }

            heatMapSubLevelInitialized = true;
            return heatMapSubLevel;
        }
    }

    private static Method getSubLevelGetLevelMethod(Class<?> owner) {
        Method method = subLevelGetLevel;
        if (method != null) {
            return method;
        }

        synchronized (SableCompat.class) {
            if (subLevelGetLevel != null) {
                return subLevelGetLevel;
            }
            try {
                subLevelGetLevel = owner.getMethod("getLevel");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                subLevelGetLevel = null;
            }
            return subLevelGetLevel;
        }
    }

    private static Method getHeatMapManagerMethod(Class<?> owner) {
        Method method = subLevelGetHeatMapManager;
        if (method != null) {
            return method;
        }

        synchronized (SableCompat.class) {
            if (subLevelGetHeatMapManager != null) {
                return subLevelGetHeatMapManager;
            }
            try {
                subLevelGetHeatMapManager = owner.getMethod("getHeatMapManager");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                subLevelGetHeatMapManager = null;
            }
            return subLevelGetHeatMapManager;
        }
    }

    private static Method getHeatMapOnSolidRemovedMethod(Class<?> owner) {
        Method method = heatMapOnSolidRemoved;
        if (method != null) {
            return method;
        }

        synchronized (SableCompat.class) {
            if (heatMapOnSolidRemoved != null) {
                return heatMapOnSolidRemoved;
            }
            try {
                heatMapOnSolidRemoved = owner.getMethod("onSolidRemoved", BlockPos.class);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                heatMapOnSolidRemoved = null;
            }
            return heatMapOnSolidRemoved;
        }
    }

    private static Method getHeatMapRebuildFromMethod(Class<?> owner) {
        Method method = heatMapRebuildFrom;
        if (method != null) {
            return method;
        }

        synchronized (SableCompat.class) {
            if (heatMapRebuildFrom != null) {
                return heatMapRebuildFrom;
            }
            try {
                heatMapRebuildFrom = owner.getDeclaredMethod("rebuildHeatmapFrom", List.class);
                heatMapRebuildFrom.setAccessible(true);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                heatMapRebuildFrom = null;
            }
            return heatMapRebuildFrom;
        }
    }

    private static Method getHeatMapRemoveCellMethod(Class<?> owner) {
        Method method = heatMapRemoveCell;
        if (method != null) {
            return method;
        }

        synchronized (SableCompat.class) {
            if (heatMapRemoveCell != null) {
                return heatMapRemoveCell;
            }
            try {
                heatMapRemoveCell = owner.getDeclaredMethod("heatMapRemove", BlockPos.class);
                heatMapRemoveCell.setAccessible(true);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                heatMapRemoveCell = null;
            }
            return heatMapRemoveCell;
        }
    }

    private static Method getSubLevelGetPlotMethod(Class<?> owner) {
        Method method = subLevelGetPlot;
        if (method != null) {
            return method;
        }

        synchronized (SableCompat.class) {
            if (subLevelGetPlot != null) {
                return subLevelGetPlot;
            }
            try {
                subLevelGetPlot = owner.getMethod("getPlot");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                subLevelGetPlot = null;
            }
            return subLevelGetPlot;
        }
    }

    private static Method getPlotGetBoundingBoxMethod(Class<?> owner) {
        Method method = plotGetBoundingBox;
        if (method != null) {
            return method;
        }

        synchronized (SableCompat.class) {
            if (plotGetBoundingBox != null) {
                return plotGetBoundingBox;
            }
            try {
                plotGetBoundingBox = owner.getMethod("getBoundingBox");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                plotGetBoundingBox = null;
            }
            return plotGetBoundingBox;
        }
    }

    private static boolean rebuildHeatMap(Object subLevel, Object heatMap) {
        return rebuildHeatMap(subLevel, heatMap, null);
    }

    private static boolean rebuildHeatMap(Object subLevel, Object heatMap, @Nullable BlockPos preferredStart) {
        try {
            Method levelMethod = getSubLevelGetLevelMethod(subLevel.getClass());
            Method plotMethod = getSubLevelGetPlotMethod(subLevel.getClass());
            Method rebuildMethod = getHeatMapRebuildFromMethod(heatMap.getClass());
            if (levelMethod == null || plotMethod == null || rebuildMethod == null) {
                return false;
            }

            Object levelObject = levelMethod.invoke(subLevel);
            if (!(levelObject instanceof Level level)) {
                return false;
            }

            Object plot = plotMethod.invoke(subLevel);
            if (plot == null) {
                return false;
            }

            Method boundsMethod = getPlotGetBoundingBoxMethod(plot.getClass());
            Object bounds = boundsMethod == null ? null : boundsMethod.invoke(plot);
            if (bounds == null) {
                return false;
            }

            int minX = invokeInt(bounds, "minX");
            int minY = invokeInt(bounds, "minY");
            int minZ = invokeInt(bounds, "minZ");
            int maxX = invokeInt(bounds, "maxX");
            int maxY = invokeInt(bounds, "maxY");
            int maxZ = invokeInt(bounds, "maxZ");
            long volume = (long) (maxX - minX + 1) * (long) (maxY - minY + 1) * (long) (maxZ - minZ + 1);
            if (maxX < minX || maxY < minY || maxZ < minZ || volume <= 0L || volume > MAX_HEATMAP_REBUILD_VOLUME) {
                return false;
            }

            List<BlockPos> blocks = new ArrayList<>();
            List<BlockPos> disconnectedEnhancedBlocks = new ArrayList<>();
            BlockPos resolvedPreferredStart = null;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(blockPos);
                        if (!state.isAir()) {
                            if (preferredStart != null && preferredStart.equals(blockPos)) {
                                resolvedPreferredStart = blockPos;
                            } else if (hasDisconnectedEnhancedShape(level, blockPos, state)) {
                                disconnectedEnhancedBlocks.add(blockPos);
                            } else {
                                blocks.add(blockPos);
                            }
                        }
                    }
                }
            }
            // A same-position Step mutation has no native Sable air/non-air event. Seed the fresh
            // fill from that changed owner so its new occupied shape defines the live graph before
            // either formerly connected side can claim a different component of the same node.
            if (resolvedPreferredStart != null) {
                blocks.add(0, resolvedPreferredStart);
            }
            blocks.addAll(disconnectedEnhancedBlocks);

            if (blocks.isEmpty()) {
                return false;
            }

            // Sable's onSolidRemoved queues a position and clears its heat cell later. A full
            // rebuild clears that queue first, and rebuildHeatmapFrom only clears positions still
            // present in its non-air input list. Clear the changed owner explicitly so a final
            // quarter removal cannot leave an invisible heat-map bridge until the next login.
            if (preferredStart != null) {
                Method removeCell = getHeatMapRemoveCellMethod(heatMap.getClass());
                if (removeCell == null) {
                    return false;
                }
                removeCell.invoke(heatMap, preferredStart);
            }

            clearHeatMapComponents(heatMap);
            if (!clearSableHeatMapSplitState(heatMap)) {
                return false;
            }
            rebuildMethod.invoke(heatMap, blocks);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    private static int invokeInt(Object target, String methodName) throws ReflectiveOperationException {
        Object value = target.getClass().getMethod(methodName).invoke(target);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new NoSuchMethodException(methodName);
    }

    private static Vec3i connectionOffset(BlockPos sourcePos, BlockPos targetPos) {
        int dx = Integer.compare(targetPos.getX() - sourcePos.getX(), 0);
        int dy = Integer.compare(targetPos.getY() - sourcePos.getY(), 0);
        int dz = Integer.compare(targetPos.getZ() - sourcePos.getZ(), 0);
        int axes = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
        if (axes != 1 && axes != 2) {
            return null;
        }
        return new Vec3i(dx, dy, dz);
    }

    private static boolean hasSharedOccupiedConnection(Level level,
                                                       Object heatMapManager,
                                                       BlockPos sourcePos,
                                                       BlockState sourceState,
                                                       boolean sourceEnhanced,
                                                       BlockPos targetPos,
                                                       BlockState targetState,
                                                       boolean targetEnhanced,
                                                       Vec3i offset) {
        ShapeComponents sourceComponents = shapeComponents(sourceEnhanced ? occupiedShape(level, sourcePos, sourceState) : Shapes.block());
        ShapeComponents targetComponents = shapeComponents(targetEnhanced ? occupiedShape(level, targetPos, targetState) : Shapes.block());
        if (sourceComponents.isEmpty() || targetComponents.isEmpty()) {
            return false;
        }

        int sourceMask = sourceEnhanced ? knownComponentMask(heatMapManager, sourcePos, sourceComponents) : sourceComponents.allMask();
        int targetMask = 0;
        for (int sourceIndex = 0; sourceIndex < sourceComponents.boxes().size(); sourceIndex++) {
            if ((sourceMask & componentBit(sourceComponents, sourceIndex)) == 0) {
                continue;
            }
            AABB sourceBox = sourceComponents.boxes().get(sourceIndex);
            for (int targetIndex = 0; targetIndex < targetComponents.boxes().size(); targetIndex++) {
                AABB targetBox = targetComponents.boxes().get(targetIndex);
                if (touchesConnection(sourceBox, targetBox, offset)) {
                    targetMask |= componentBit(targetComponents, targetIndex);
                }
            }
        }
        if (targetMask == 0) {
            return false;
        }

        if (targetEnhanced) {
            rememberHeatMapComponents(heatMapManager, targetPos, targetMask);
        }
        return true;
    }

    private static boolean supportsEnhancedSlabShape(BlockState state) {
        return EnhancedSlabCreateCompat.supports(state)
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock;
    }

    private static boolean hasDisconnectedEnhancedShape(Level level, BlockPos pos, BlockState state) {
        return supportsEnhancedSlabShape(state) && shapeComponents(occupiedShape(level, pos, state)).componentCount() > 1;
    }

    private static int knownComponentMask(Object heatMapManager, BlockPos pos, ShapeComponents components) {
        if (heatMapManager == null) {
            return components.allMask();
        }
        synchronized (HEATMAP_COMPONENTS) {
            Map<Long, Integer> componentsByPos = HEATMAP_COMPONENTS.get(heatMapManager);
            if (componentsByPos != null) {
                Integer mask = componentsByPos.get(pos.asLong());
                if (mask != null) {
                    return mask;
                }
            }
        }
        return components.allMask();
    }

    private static void rememberHeatMapComponents(Object heatMapManager, BlockPos pos, int mask) {
        if (heatMapManager == null) {
            return;
        }
        synchronized (HEATMAP_COMPONENTS) {
            Map<Long, Integer> componentsByPos = HEATMAP_COMPONENTS.computeIfAbsent(heatMapManager, ignored -> new HashMap<>());
            componentsByPos.merge(pos.asLong(), mask, (oldMask, newMask) -> oldMask | newMask);
        }
    }

    private static void forgetHeatMapComponents(Object heatMapManager, BlockPos pos) {
        synchronized (HEATMAP_COMPONENTS) {
            Map<Long, Integer> componentsByPos = HEATMAP_COMPONENTS.get(heatMapManager);
            if (componentsByPos != null) {
                componentsByPos.remove(pos.asLong());
            }
        }
    }

    private static void clearHeatMapComponents(Object heatMapManager) {
        synchronized (HEATMAP_COMPONENTS) {
            HEATMAP_COMPONENTS.remove(heatMapManager);
        }
    }

    /**
     * Sable's rebuildHeatmapFrom resets its propagation queues but intentionally preserves the
     * pending split maps. That is correct for Sable's internal split reuse, but not for our full
     * post-material-restoration rebuild: those entries were computed before position-backed step
     * slots existed and would split an otherwise connected assembler/step pair afterward.
     */
    private static boolean clearSableHeatMapSplitState(Object heatMapManager) {
        return clearReflectiveContainer(heatMapManager, "subLevelSplits")
                && clearReflectiveContainer(heatMapManager, "splitIndexMap");
    }

    private static boolean clearReflectiveContainer(Object owner, String fieldName) {
        if (owner == null || fieldName == null) {
            return false;
        }

        try {
            Field field = owner.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(owner);
            if (value == null) {
                return false;
            }
            value.getClass().getMethod("clear").invoke(value);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    private static int componentBit(ShapeComponents components, int boxIndex) {
        return 1 << Math.min(30, components.componentByBox()[boxIndex]);
    }

    private static ShapeComponents shapeComponents(VoxelShape shape) {
        List<AABB> boxes = shape.toAabbs();
        if (boxes.isEmpty()) {
            return ShapeComponents.EMPTY;
        }

        int[] components = new int[boxes.size()];
        int componentCount = 0;
        for (int i = 0; i < boxes.size(); i++) {
            components[i] = i;
            componentCount++;
        }

        boolean changed;
        do {
            changed = false;
            for (int first = 0; first < boxes.size(); first++) {
                for (int second = first + 1; second < boxes.size(); second++) {
                    if (components[first] != components[second] && boxesConnected(boxes.get(first), boxes.get(second))) {
                        int from = components[second];
                        int to = components[first];
                        for (int index = 0; index < components.length; index++) {
                            if (components[index] == from) {
                                components[index] = to;
                            }
                        }
                        componentCount--;
                        changed = true;
                    }
                }
            }
        } while (changed);

        Map<Integer, Integer> compact = new HashMap<>();
        int next = 0;
        int allMask = 0;
        for (int i = 0; i < components.length; i++) {
            Integer compacted = compact.get(components[i]);
            if (compacted == null) {
                compacted = next++;
                compact.put(components[i], compacted);
            }
            components[i] = compacted;
            allMask |= 1 << Math.min(30, compacted);
        }
        return new ShapeComponents(boxes, components, next, allMask);
    }

    private static boolean boxesConnected(AABB first, AABB second) {
        double epsilon = 1.0E-5D;
        return volumeOverlaps(first, second, epsilon)
                || (touchesPlane(first.maxX, second.minX, epsilon) || touchesPlane(second.maxX, first.minX, epsilon))
                && overlaps(first.minY, first.maxY, second.minY, second.maxY, epsilon)
                && overlaps(first.minZ, first.maxZ, second.minZ, second.maxZ, epsilon)
                || (touchesPlane(first.maxY, second.minY, epsilon) || touchesPlane(second.maxY, first.minY, epsilon))
                && overlaps(first.minX, first.maxX, second.minX, second.maxX, epsilon)
                && overlaps(first.minZ, first.maxZ, second.minZ, second.maxZ, epsilon)
                || (touchesPlane(first.maxZ, second.minZ, epsilon) || touchesPlane(second.maxZ, first.minZ, epsilon))
                && overlaps(first.minX, first.maxX, second.minX, second.maxX, epsilon)
                && overlaps(first.minY, first.maxY, second.minY, second.maxY, epsilon);
    }

    private static boolean volumeOverlaps(AABB first, AABB second, double epsilon) {
        return overlaps(first.minX, first.maxX, second.minX, second.maxX, epsilon)
                && overlaps(first.minY, first.maxY, second.minY, second.maxY, epsilon)
                && overlaps(first.minZ, first.maxZ, second.minZ, second.maxZ, epsilon);
    }

    private static boolean touchesPlane(double first, double second, double epsilon) {
        return Math.abs(first - second) <= epsilon;
    }

    private static VoxelShape occupiedShape(Level level, BlockPos pos, BlockState state) {
        VoxelShape shape = quadOccupiedShape(level, pos, state);
        if (shape.isEmpty()) {
            shape = state.getCollisionShape(level, pos, CollisionContext.empty());
        }
        if (shape.isEmpty()) {
            shape = state.getBlockSupportShape(level, pos);
        }
        return shape;
    }

    private static VoxelShape quadOccupiedShape(Level level, BlockPos pos, BlockState state) {
        BlockState[] slots = quadSlots(level, pos);
        if (slots == null) {
            return Shapes.empty();
        }

        VoxelShape shape = Shapes.empty();
        if (state.getBlock() instanceof QuadStepBlock && state.hasProperty(QuadStepBlock.AXIS)) {
            Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
            for (int i = 0; i < Math.min(4, slots.length); i++) {
                if (slots[i] != null) {
                    shape = Shapes.or(shape, QuadStepBlock.shapeForIndex(axis, i));
                }
            }
            return shape;
        }
        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            for (int i = 0; i < Math.min(4, slots.length); i++) {
                if (slots[i] != null) {
                    shape = Shapes.or(shape, QuadVerticalStepBlock.shapeForIndex(i));
                }
            }
        }
        return shape;
    }

    private static BlockState[] quadSlots(Level level, BlockPos pos) {
        BlockEntity blockEntity = plotBlockEntity(level, pos);
        if (blockEntity == null) {
            blockEntity = level.getBlockEntity(pos);
        }
        if (blockEntity instanceof QuadStepBlockEntity quadStep) {
            return quadStep.getAllSlabs();
        }
        if (blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep) {
            return quadVerticalStep.getAllSlabs();
        }
        return EnhancedSlabMaterialSavedData.get(level, pos.asLong());
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

    private static Direction directionFromOffset(Vec3i offset) {
        if (offset.getX() > 0) return Direction.EAST;
        if (offset.getX() < 0) return Direction.WEST;
        if (offset.getY() > 0) return Direction.UP;
        if (offset.getY() < 0) return Direction.DOWN;
        if (offset.getZ() > 0) return Direction.SOUTH;
        if (offset.getZ() < 0) return Direction.NORTH;
        return null;
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

    private record GatherEntry(BlockPos pos, BlockState state) {
    }

    private record ShapeComponents(List<AABB> boxes, int[] componentByBox, int componentCount, int allMask) {
        private static final ShapeComponents EMPTY = new ShapeComponents(List.of(), new int[0], 0, 0);

        private boolean isEmpty() {
            return boxes.isEmpty();
        }
    }

    private static Method getSetUnsavedMethod() {
        if (setUnsavedInitialized) {
            return setUnsaved;
        }

        synchronized (SableCompat.class) {
            if (setUnsavedInitialized) {
                return setUnsaved;
            }

            try {
                Method method = LevelChunk.class.getMethod("setUnsaved", boolean.class);
                setUnsaved = method;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                setUnsaved = null;
            }

            setUnsavedInitialized = true;
            return setUnsaved;
        }
    }
}
