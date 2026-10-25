package org.onenonly.bitsandbalance.mixin.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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

            BlockState sourceState = level.getBlockState(sourcePos);
            BlockState[] slots = EnhancedSlabCreateCompat.captureMaterialSlots(level, sourcePos, sourceState);
            if (EnhancedSlabCreateCompat.hasMaterialSlots(slots)) {
                captured.put(sourcePos.asLong(), new CapturedSlots(sourcePos.immutable(), slots));
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

            for (CapturedSlots capturedSlots : captured.values()) {
                BlockPos targetPos = bitsandbalance$applyTransform(transform, capturedSlots.sourcePos());
                if (targetPos == null) continue;

                BlockState targetState = targetLevel.getBlockState(targetPos);
                EnhancedSlabCreateCompat.restoreMaterialSlots(targetLevel, targetPos, targetState, capturedSlots.slots());
                if (EnhancedSlabCreateCompat.supports(targetState)) {
                    targetLevel.sendBlockUpdated(targetPos, targetState, targetState, Block.UPDATE_ALL);
                }

                if (level != null && (!level.equals(targetLevel) || !capturedSlots.sourcePos().equals(targetPos))) {
                    EnhancedSlabMaterialSavedData.remember(level, capturedSlots.sourcePos().asLong());
                }
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

    private record CapturedSlots(BlockPos sourcePos, BlockState[] slots) {
    }
}
