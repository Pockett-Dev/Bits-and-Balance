package org.onenonly.bitsandbalance.fabric.mixin.litematica;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.common.compat.litematica.LitematicaEnhancedSlabCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.util.WorldPlacingUtils", remap = false)
public abstract class LitematicaSchematicWorldEnhancedSlabPlaceMixin {
    @Inject(method = "placeToProtoChunk", at = @At("RETURN"), remap = false, require = 1)
    private static void bitsandbalance$restoreSchematicWorldStoredSlabMaterials(@Coerce Object chunk,
                                                                                ChunkPos chunkPos,
                                                                                @Coerce Object schematicPlacement,
                                                                                CallbackInfoReturnable<Object> cir) {
        if (chunk == null || chunkPos == null || schematicPlacement == null || cir.getReturnValue() == null) {
            return;
        }

        try {
            Object wrapped = chunk.getClass().getMethod("getWrapped").invoke(chunk);
            Method getLevel = wrapped.getClass().getMethod("getLevel");
            Level level = (Level) getLevel.invoke(wrapped);
            LitematicaEnhancedSlabCompat.restorePlacementChunk(level, chunkPos.getWorldPosition(), schematicPlacement);
        } catch (Throwable ignored) {
        }
    }
}
