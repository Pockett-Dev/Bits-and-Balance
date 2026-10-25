package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricEnhancedSlabLegacyMigration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class LevelChunkEnhancedSlabLegacyBlockEntityMixin {
    @Shadow
    @Final
    private Level level;

    @Inject(method = "setBlockEntity", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$migrateRejectedEnhancedSlabBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel
                && FabricEnhancedSlabLegacyMigration.migrateRejectedBlockEntity(serverLevel, blockEntity)) {
            ci.cancel();
        }
    }
}
