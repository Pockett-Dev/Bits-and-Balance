package org.onenonly.bitsandbalance.mixin.create;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Map;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.render.ClientContraption", remap = false)
public abstract class CreateClientContraptionEnhancedSlabRenderMixin {
    @Inject(method = "readBlockEntity", at = @At("HEAD"), remap = false, require = 0)
    private void bitsandbalance$seedCreateEnhancedSlabRenderSlotFromBlockInfo(Level level,
                                                                              StructureBlockInfo info,
                                                                              boolean legacy,
                                                                              CallbackInfoReturnable<BlockEntity> cir) {
        if (info != null) {
            EnhancedSlabCreateCompat.rememberRenderSlots(level, info.pos(), info.state(), info.nbt());
            CompoundTag updateTag = bitsandbalance$getCreateUpdateTag(info.pos());
            if (updateTag != null && updateTag != info.nbt()) {
                EnhancedSlabCreateCompat.rememberRenderSlots(level, info.pos(), info.state(), updateTag);
            }
        }
    }

    @Inject(method = "setupRenderLevelAndRenderedBlockEntities", at = @At("RETURN"), remap = false, require = 0)
    private void bitsandbalance$seedCreateEnhancedSlabRenderSlots(CallbackInfo ci) {
        bitsandbalance$seedCreateEnhancedSlabRenderSlots(bitsandbalance$getCreateRenderLevel(), bitsandbalance$getCreateContraption());
    }

    @Inject(method = "resetRenderLevel", at = @At("RETURN"), remap = false, require = 0)
    private void bitsandbalance$reseedCreateEnhancedSlabRenderSlots(CallbackInfo ci) {
        bitsandbalance$seedCreateEnhancedSlabRenderSlots(bitsandbalance$getCreateRenderLevel(), bitsandbalance$getCreateContraption());
    }

    @Inject(method = "getRenderLevel", at = @At("RETURN"), remap = false, require = 0)
    private void bitsandbalance$seedReturnedCreateRenderLevel(CallbackInfoReturnable<Object> cir) {
        bitsandbalance$seedCreateEnhancedSlabRenderSlots(cir.getReturnValue(), bitsandbalance$getCreateContraption());
    }

    @Unique
    private void bitsandbalance$seedCreateEnhancedSlabRenderSlots(Object renderWorld, Object contraption) {
        EnhancedSlabCreateCompat.seedRenderSlots(renderWorld, contraption);
    }

    @Unique
    private Object bitsandbalance$getCreateRenderLevel() {
        return bitsandbalance$getCreateField("renderLevel");
    }

    @Unique
    private Object bitsandbalance$getCreateContraption() {
        return bitsandbalance$getCreateField("contraption");
    }

    @Unique
    private CompoundTag bitsandbalance$getCreateUpdateTag(BlockPos pos) {
        if (pos == null) {
            return null;
        }
        Object contraption = bitsandbalance$getCreateContraption();
        Object updateTags = bitsandbalance$getCreateField(contraption, "updateTags");
        if (updateTags instanceof Map<?, ?> tags) {
            Object tag = tags.get(pos);
            return tag instanceof CompoundTag compound ? compound : null;
        }
        return null;
    }

    @Unique
    private Object bitsandbalance$getCreateField(String name) {
        return bitsandbalance$getCreateField(this, name);
    }

    @Unique
    private Object bitsandbalance$getCreateField(Object target, String name) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }
}
