package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.onenonly.bitsandbalance.mechanics.NeoForgeEnhancedSlabFixedBlockEntityMigration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkEnhancedSlabFixedBlockEntityMigrationMixin {
    @Shadow
    @Final
    private Level level;

    @Inject(method = "setBlockEntity", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$migrateFixedEnhancedSlabBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel
                && NeoForgeEnhancedSlabFixedBlockEntityMigration.migrateRejectedBlockEntity(serverLevel, blockEntity)) {
            ci.cancel();
        }
    }

    @Inject(method = "promotePendingBlockEntity", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$migrateFixedEnhancedSlabBlockEntityTag(BlockPos pos,
                                                                        CompoundTag tag,
                                                                        CallbackInfoReturnable<BlockEntity> cir) {
        if (level instanceof ServerLevel serverLevel
                && NeoForgeEnhancedSlabFixedBlockEntityMigration.migrateRejectedBlockEntityTag(serverLevel, pos, tag)) {
            cir.setReturnValue(null);
        }
    }
}
