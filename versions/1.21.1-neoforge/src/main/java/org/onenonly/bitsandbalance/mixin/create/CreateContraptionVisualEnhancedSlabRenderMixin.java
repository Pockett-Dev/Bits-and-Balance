package org.onenonly.bitsandbalance.mixin.create;

import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.render.ContraptionVisual", remap = false)
public abstract class CreateContraptionVisualEnhancedSlabRenderMixin {
    @Inject(method = "setupStructure", at = @At("HEAD"), remap = false, require = 0)
    private void bitsandbalance$seedEnhancedSlabRenderSlots(@Coerce Object clientContraption, CallbackInfo ci) {
        if (clientContraption == null) {
            return;
        }
        Object contraption = bitsandbalance$getCreateField(clientContraption, "contraption");
        Object renderLevel = bitsandbalance$invokeNoArg(clientContraption, "getRenderLevel");
        EnhancedSlabCreateCompat.seedRenderSlots(renderLevel, contraption);
    }

    @Unique
    private static Object bitsandbalance$invokeNoArg(Object target, String methodName) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    @Unique
    private static Object bitsandbalance$getCreateField(Object target, String fieldName) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
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
