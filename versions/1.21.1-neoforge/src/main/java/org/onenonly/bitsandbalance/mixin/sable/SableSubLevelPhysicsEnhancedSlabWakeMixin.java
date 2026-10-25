package org.onenonly.bitsandbalance.mixin.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * Directly wakes an enhanced-slab physics body after a plot-storage block change.
 *
 * <p>Sable's normal tail wake uses the changed block's plot-grid coordinates for a world-space
 * intersection query. That query can miss the body represented by the plot, particularly when its
 * assembler is removed. Resolving the owning plot gives us the exact body without waking unrelated
 * physics objects.</p>
 */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem", remap = false)
public abstract class SableSubLevelPhysicsEnhancedSlabWakeMixin {
    @Shadow(remap = false)
    @Final
    private ServerLevel level;

    @Inject(
            method = "handleBlockChange(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/chunk/LevelChunkSection;IIILnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("TAIL"),
            require = 0,
            remap = false
    )
    private void bitsandbalance$wakeEnhancedSlabBody(SectionPos sectionPos,
                                                      LevelChunkSection section,
                                                      int localX,
                                                      int localY,
                                                      int localZ,
                                                      BlockState oldState,
                                                      BlockState newState,
                                                      CallbackInfo ci) {
        if (this.level == null || sectionPos == null) {
            return;
        }

        BlockPos changedPos = sectionPos.origin().offset(localX, localY, localZ);
        if (!bitsandbalance$affectsEnhancedSlab(changedPos, oldState, newState)) {
            return;
        }

        SableCompat.recordEnhancedSlabDirectWakeAttempt();
        if (bitsandbalance$wakeOwningSubLevel(sectionPos)) {
            SableCompat.recordEnhancedSlabDirectWakeSuccess();
        }
    }

    @Unique
    private boolean bitsandbalance$affectsEnhancedSlab(BlockPos changedPos,
                                                        BlockState oldState,
                                                        BlockState newState) {
        if (EnhancedSlabCreateCompat.supports(oldState) || EnhancedSlabCreateCompat.supports(newState)) {
            return true;
        }

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = changedPos.relative(direction);
            BlockState neighborState = SableCompat.plotBlockState(this.level, neighborPos);
            if (neighborState == null) {
                neighborState = this.level.getBlockState(neighborPos);
            }
            if (EnhancedSlabCreateCompat.supports(neighborState)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean bitsandbalance$wakeOwningSubLevel(SectionPos sectionPos) {
        try {
            ClassLoader loader = SableSubLevelPhysicsEnhancedSlabWakeMixin.class.getClassLoader();
            Class<?> containerClass = Class.forName(
                    "dev.ryanhcode.sable.api.sublevel.SubLevelContainer",
                    false,
                    loader
            );
            Object container = containerClass.getMethod("getContainer", ServerLevel.class).invoke(null, this.level);
            if (container == null) {
                return false;
            }

            Object plot = containerClass.getMethod("getPlot", int.class, int.class).invoke(
                    container,
                    sectionPos.x(),
                    sectionPos.z()
            );
            if (plot == null) {
                return false;
            }

            Class<?> levelPlotClass = Class.forName(
                    "dev.ryanhcode.sable.sublevel.plot.LevelPlot",
                    false,
                    loader
            );
            Object subLevel = levelPlotClass.getMethod("getSubLevel").invoke(plot);
            if (subLevel == null) {
                return false;
            }

            Class<?> physicsSystemClass = Class.forName(
                    "dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem",
                    false,
                    loader
            );
            Object pipeline = physicsSystemClass.getMethod("getPipeline").invoke(this);
            if (pipeline == null) {
                return false;
            }

            Class<?> pipelineClass = Class.forName(
                    "dev.ryanhcode.sable.api.physics.PhysicsPipeline",
                    false,
                    loader
            );
            Class<?> pipelineBodyClass = Class.forName(
                    "dev.ryanhcode.sable.api.physics.PhysicsPipelineBody",
                    false,
                    loader
            );
            Method wakeUp = pipelineClass.getMethod("wakeUp", pipelineBodyClass);
            wakeUp.invoke(pipeline, subLevel);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }
}
