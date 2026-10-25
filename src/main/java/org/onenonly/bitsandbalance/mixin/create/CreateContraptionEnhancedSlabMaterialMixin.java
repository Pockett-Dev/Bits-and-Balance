package org.onenonly.bitsandbalance.mixin.create;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.Map;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.Contraption", remap = false)
public abstract class CreateContraptionEnhancedSlabMaterialMixin {
    @Shadow public boolean disassembled;
    @Unique private boolean bitsandbalance$restoreCreateEnhancedSlabMaterials;

    @Inject(method = "capture", at = @At("RETURN"), cancellable = true, remap = false)
    private void bitsandbalance$captureEnhancedSlabMaterials(Level world, BlockPos pos, CallbackInfoReturnable<Object> cir) {
        Object pair = cir.getReturnValue();
        if (pair == null) return;

        try {
            Object key = pair.getClass().getMethod("getKey").invoke(pair);
            if (!(key instanceof StructureBlockInfo info)) return;

            CompoundTag tag = EnhancedSlabCreateCompat.withCapturedMaterials(world, pos, info.state(), info.nbt());
            if (tag == null || tag == info.nbt()) return;

            Object value = pair.getClass().getMethod("getValue").invoke(pair);
            StructureBlockInfo replacementInfo = new StructureBlockInfo(info.pos(), info.state(), tag);
            Class<?> pairClass = Class.forName("org.apache.commons.lang3.tuple.Pair", false, Thread.currentThread().getContextClassLoader());
            Object replacementPair = pairClass.getMethod("of", Object.class, Object.class).invoke(null, replacementInfo, value);
            cir.setReturnValue(replacementPair);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Inject(method = "addBlocksToWorld", at = @At("HEAD"), remap = false)
    private void bitsandbalance$markCreateEnhancedSlabRestore(Level world, @Coerce Object transform, CallbackInfo ci) {
        bitsandbalance$restoreCreateEnhancedSlabMaterials = !disassembled;
    }

    @Inject(method = "addBlocksToWorld", at = @At("RETURN"), remap = false)
    private void bitsandbalance$restoreCreateEnhancedSlabMaterials(Level world, @Coerce Object transform, CallbackInfo ci) {
        if (!bitsandbalance$restoreCreateEnhancedSlabMaterials) return;
        bitsandbalance$restoreCreateEnhancedSlabMaterials = false;

        try {
            Object blocksObject = getClass().getMethod("getBlocks").invoke(this);
            if (!(blocksObject instanceof Map<?, ?> blocks)) return;

            Method apply = transform.getClass().getMethod("apply", BlockPos.class);
            for (Object value : blocks.values()) {
                if (!(value instanceof StructureBlockInfo info)) continue;
                Object targetObject = apply.invoke(transform, info.pos());
                if (!(targetObject instanceof BlockPos targetPos)) continue;

                BlockState state = world.getBlockState(targetPos);
                EnhancedSlabCreateCompat.restoreMovedSlots(world, targetPos, state, info.nbt());
                if (EnhancedSlabCreateCompat.supports(state)) {
                    world.sendBlockUpdated(targetPos, state, state, Block.UPDATE_ALL);
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
