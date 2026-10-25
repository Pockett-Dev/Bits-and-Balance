package org.onenonly.bitsandbalance.fabric.mixin.create;

import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.render.ClientContraption", remap = false)
public abstract class CreateClientContraptionEnhancedSlabRenderMixin {
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
    private Object bitsandbalance$getCreateField(String name) {
        Class<?> type = getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(this);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }
}
