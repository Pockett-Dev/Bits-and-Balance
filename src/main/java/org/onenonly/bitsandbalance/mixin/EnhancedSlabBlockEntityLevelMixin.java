package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Defers server material restoration until the chunk is published and safe to access. */
@Mixin(LevelChunk.class)
public abstract class EnhancedSlabBlockEntityLevelMixin {
    @Shadow
    @Final
    private Level level;

    @Inject(method = "setBlockEntity", at = @At("TAIL"))
    private void bitsandbalance$restorePortableEnhancedSlabMaterials(BlockEntity blockEntity, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            EnhancedSlabMaterialSavedData.queuePortableBlockEntities(serverLevel, (LevelChunk) (Object) this);
        } else {
            // Lazy client creation (including outline/hover queries) has no loaded NBT yet.
            // The constructor's empty/oak defaults must never replace authoritative materials.
            EnhancedSlabMaterialSavedData.hydrateClientBlockEntity(level, blockEntity);
        }
    }
}
