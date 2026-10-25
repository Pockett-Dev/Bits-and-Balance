package org.onenonly.bitsandbalance.mixin.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper", remap = false)
public abstract class SableSubLevelAssemblyEnhancedSlabMaterialMixin {
    @Unique
    private static final ThreadLocal<Map<Long, CapturedSlots>> bitsandbalance$sableCapturedSlots =
            ThreadLocal.withInitial(HashMap::new);

    @Inject(method = "moveBlocks", at = @At("HEAD"), remap = false, require = 0)
    private static void bitsandbalance$captureSableEnhancedSlabMaterials(ServerLevel level,
                                                                         @Coerce Object transform,
                                                                         Iterable<BlockPos> blocks,
                                                                         CallbackInfo ci) {
        Map<Long, CapturedSlots> captured = bitsandbalance$sableCapturedSlots.get();
        captured.clear();
        if (level == null || blocks == null) return;

        for (BlockPos sourcePos : blocks) {
            if (sourcePos == null) continue;

            BlockState sourceState = SableCompat.plotBlockState(level, sourcePos);
            if (sourceState == null) {
                sourceState = level.getBlockState(sourcePos);
            }
            BlockState[] slots = EnhancedSlabCreateCompat.captureMaterialSlots(level, sourcePos, sourceState);
            if (EnhancedSlabCreateCompat.hasMaterialSlots(slots)) {
                captured.put(sourcePos.asLong(), new CapturedSlots(sourcePos.immutable(), sourceState, slots));
            }
        }
    }

    @Inject(method = "moveBlocks", at = @At("RETURN"), remap = false, require = 0)
    private static void bitsandbalance$restoreSableEnhancedSlabMaterials(ServerLevel level,
                                                                         @Coerce Object transform,
                                                                         Iterable<BlockPos> blocks,
                                                                         CallbackInfo ci) {
        Map<Long, CapturedSlots> captured = bitsandbalance$sableCapturedSlots.get();
        if (captured.isEmpty()) return;

        try {
            ServerLevel targetLevel = bitsandbalance$resultingLevel(transform);
            if (targetLevel == null) {
                targetLevel = level;
            }
            BlockPos massRefreshTarget = null;
            Set<Long> physicsRefreshSections = new HashSet<>();

            for (CapturedSlots capturedSlots : captured.values()) {
                BlockPos targetPos = bitsandbalance$applyTransform(transform, capturedSlots.sourcePos());
                if (targetPos == null) continue;

                BlockState targetState = SableCompat.plotBlockState(targetLevel, targetPos);
                if (targetState == null) {
                    targetState = targetLevel.getBlockState(targetPos);
                }
                BlockState[] movedSlots = EnhancedSlabCreateCompat.transformMovedSlots(
                        capturedSlots.sourceState(),
                        targetState,
                        capturedSlots.slots(),
                        transform
                );
                EnhancedSlabCreateCompat.restoreMaterialSlots(targetLevel, targetPos, targetState, movedSlots);
                if (EnhancedSlabCreateCompat.supports(targetState)) {
                    massRefreshTarget = targetPos;
                    physicsRefreshSections.add(SectionPos.of(targetPos).asLong());
                    // Sable already recorded the moved block. A synthetic AIR -> state change here
                    // would increment its heat-map solid count twice and force endless split/reassembly.
                    SableCompat.refreshPlotBlockState(
                            targetLevel,
                            targetPos,
                            targetState,
                            targetState,
                            Block.UPDATE_ALL
                    );
                    targetLevel.sendBlockUpdated(targetPos, targetState, targetState, Block.UPDATE_ALL);
                }

                if (bitsandbalance$sourceWasMoved(level, targetLevel, capturedSlots.sourcePos(), targetPos)) {
                    EnhancedSlabMaterialSavedData.remember(level, capturedSlots.sourcePos().asLong());
                }
            }

            // Sable can register the new body while moveBlocks is still emitting its per-block
            // notifications. Its initial MassTracker then contains only the blocks moved before
            // registration, and its heat map sees storage-backed shapes before their slots exist.
            // All enhanced-slab slots and blocks are complete here, before assembleBlocks reads
            // the center of mass, so rebuild connectivity and mass once from the finished plot.
            if (massRefreshTarget != null) {
                SableCompat.rebuildEnhancedSlabHeatMap(targetLevel, massRefreshTarget);
                bitsandbalance$refreshSablePhysics(
                        targetLevel,
                        massRefreshTarget,
                        physicsRefreshSections
                );
            }
        } finally {
            captured.clear();
        }
    }

    @Unique
    private static @Nullable ServerLevel bitsandbalance$resultingLevel(@Nullable Object transform) {
        if (transform == null) return null;

        try {
            Method method = transform.getClass().getMethod("getLevel");
            Object result = method.invoke(transform);
            return result instanceof ServerLevel serverLevel ? serverLevel : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    @Unique
    private static @Nullable BlockPos bitsandbalance$applyTransform(@Nullable Object transform, BlockPos pos) {
        if (transform == null || pos == null) return null;

        try {
            Method method = transform.getClass().getMethod("apply", BlockPos.class);
            Object result = method.invoke(transform, pos);
            return result instanceof BlockPos blockPos ? blockPos : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    @Unique
    private static boolean bitsandbalance$sourceWasMoved(@Nullable ServerLevel sourceLevel,
                                                         @Nullable ServerLevel targetLevel,
                                                         BlockPos sourcePos,
                                                         @Nullable BlockPos targetPos) {
        if (sourceLevel == null || targetLevel == null || sourcePos == null || targetPos == null) {
            return false;
        }
        if (sourceLevel.equals(targetLevel) && sourcePos.equals(targetPos)) {
            return false;
        }

        BlockState remainingSourceState = sourceLevel.getBlockState(sourcePos);
        return remainingSourceState.isAir() || !EnhancedSlabCreateCompat.supports(remainingSourceState);
    }

    @Unique
    private static void bitsandbalance$refreshSablePhysics(ServerLevel level,
                                                           BlockPos plotPos,
                                                           Set<Long> affectedSections) {
        if (level == null || plotPos == null) {
            return;
        }

        try {
            ClassLoader loader = SableSubLevelAssemblyEnhancedSlabMaterialMixin.class.getClassLoader();
            Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable", false, loader);
            Object helper = sableClass.getField("HELPER").get(null);
            if (helper == null) {
                return;
            }

            Method getContaining = helper.getClass().getMethod(
                    "getContaining",
                    Level.class,
                    net.minecraft.core.Vec3i.class
            );
            Object subLevel = getContaining.invoke(helper, level, plotPos);
            if (subLevel == null) {
                return;
            }

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

            Class<?> serverSubLevelClass = Class.forName(
                    "dev.ryanhcode.sable.sublevel.ServerSubLevel",
                    false,
                    loader
            );
            Class<?> physicsPipelineBodyClass = Class.forName(
                    "dev.ryanhcode.sable.api.physics.PhysicsPipelineBody",
                    false,
                    loader
            );
            Class<?> pipelineClass = Class.forName(
                    "dev.ryanhcode.sable.api.physics.PhysicsPipeline",
                    false,
                    loader
            );

            // moveBlocks initially uploads sections before position-backed enhanced-slab material
            // data has been restored. Re-upload each affected section once so Rapier receives the
            // finished neighborhood classification and position-aware collider for the owning body.
            if (affectedSections != null && !affectedSections.isEmpty()) {
                try {
                    Method handleChunkSectionAddition = pipelineClass.getMethod(
                            "handleChunkSectionAddition",
                            LevelChunkSection.class,
                            int.class,
                            int.class,
                            int.class,
                            boolean.class
                    );
                    for (long packedSection : affectedSections) {
                        SectionPos sectionPos = SectionPos.of(packedSection);
                        LevelChunk chunk = level.getChunk(sectionPos.x(), sectionPos.z());
                        int sectionIndex = chunk.getSectionIndex(sectionPos.minBlockY());
                        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
                            continue;
                        }

                        try {
                            handleChunkSectionAddition.invoke(
                                    pipeline,
                                    chunk.getSection(sectionIndex),
                                    sectionPos.x(),
                                    sectionPos.y(),
                                    sectionPos.z(),
                                    true
                            );
                        } catch (ReflectiveOperationException | RuntimeException ignored) {
                        }
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }

            try {
                subLevel.getClass().getMethod("buildMassTracker").invoke(subLevel);
                subLevel.getClass().getMethod("updateMergedMassData", float.class).invoke(subLevel, 1.0F);
                pipelineClass.getMethod("onStatsChanged", serverSubLevelClass).invoke(pipeline, subLevel);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }

            // Plot coordinates are storage coordinates, not the body's world-space pose. Wake the
            // known body directly instead of relying on Sable's coordinate intersection query.
            try {
                pipelineClass.getMethod("wakeUp", physicsPipelineBodyClass).invoke(pipeline, subLevel);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    private record CapturedSlots(BlockPos sourcePos, BlockState sourceState, BlockState[] slots) {
    }
}
