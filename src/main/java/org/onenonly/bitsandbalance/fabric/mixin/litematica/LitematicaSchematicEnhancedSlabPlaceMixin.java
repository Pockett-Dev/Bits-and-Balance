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

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.util.SchematicPlacingUtils", remap = false)
public abstract class LitematicaSchematicEnhancedSlabPlaceMixin {
    @Inject(method = "placeToWorldWithinChunk", at = @At("RETURN"), remap = false, require = 1)
    private static void bitsandbalance$restoreStoredSlabMaterials(Level world,
                                                                  ChunkPos chunkPos,
                                                                  @Coerce Object schematicPlacement,
                                                                  @Coerce Object replace,
                                                                  @Coerce Object layerBehavior,
                                                                  boolean notifyNeighbors,
                                                                  CallbackInfoReturnable<Boolean> cir) {
        if (world == null || chunkPos == null || schematicPlacement == null || !Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        LitematicaEnhancedSlabCompat.restorePlacementChunk(world, chunkPos.getWorldPosition(), schematicPlacement);
    }
}
