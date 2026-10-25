package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.client.EnhancedSlabTerrainModelOffset;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.fabric.model.FabricModelAccess", remap = false)
public abstract class SodiumFabricModelAccessEnhancedSlabMixin {
    @Unique
    private static final Logger bitsandbalance$logger = LoggerFactory.getLogger("bitsandbalance-sodium-enhanced-slab-render");

    @Unique
    private static final AtomicBoolean bitsandbalance$logged = new AtomicBoolean(false);

    @Inject(method = "collectPartsOf", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$collectEnhancedSlabParts(BlockStateModel model,
                                                         BlockAndTintGetter level,
                                                         BlockPos pos,
                                                         BlockState state,
                                                         RandomSource random,
                                                         @Coerce Object listStorage,
                                                         CallbackInfoReturnable<List<BlockStateModelPart>> cir) {
        if (!EnhancedSlabTerrainRenderHelper.handles(state)) {
            bitsandbalance$collectOffsetParts(model, level, pos, state, random, listStorage, cir);
            return;
        }

        List<Object> parts = bitsandbalance$clearAndGet(listStorage);
        if (!EnhancedSlabTerrainRenderHelper.collectModelParts(level, pos, state, parts)) {
            return;
        }

        if (bitsandbalance$logged.compareAndSet(false, true)) {
            bitsandbalance$logger.info("Sodium FabricModelAccess supplied enhanced slab terrain parts for block={} pos={} parts={}", state.getBlock(), pos, parts.size());
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        List<BlockStateModelPart> typedParts = (List) parts;
        cir.setReturnValue(typedParts);
    }

    @Unique
    private void bitsandbalance$collectOffsetParts(BlockStateModel model,
                                                   BlockAndTintGetter level,
                                                   BlockPos pos,
                                                   BlockState state,
                                                   RandomSource random,
                                                   Object listStorage,
                                                   CallbackInfoReturnable<List<BlockStateModelPart>> cir) {
        if (model == null || level == null || pos == null || state == null) {
            return;
        }

        double yOff = EnhancedSlabHelper.getVisualYOffset(level, pos, state);
        if (yOff == 0.0D) {
            return;
        }

        List<Object> rawParts = bitsandbalance$clearAndGet(listStorage);
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<BlockStateModelPart> typedParts = (List) rawParts;
        model.collectParts(random, typedParts);
        if (typedParts.isEmpty()) {
            return;
        }

        EnhancedSlabTerrainModelOffset.wrapPartsInPlace(typedParts, (float) yOff, true);
        cir.setReturnValue(typedParts);
    }

    @Unique
    private static List<Object> bitsandbalance$clearAndGet(Object listStorage) {
        if (listStorage != null) {
            try {
                Method clearAndGet = listStorage.getClass().getMethod("clearAndGet");
                Object value = clearAndGet.invoke(listStorage);
                if (value instanceof List<?> list) {
                    @SuppressWarnings("unchecked")
                    List<Object> typed = (List<Object>) list;
                    return typed;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return new ArrayList<>();
    }
}
