package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricEnhancedSlabLegacyMigration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkEnhancedSlabLegacyBlockEntityMixin {
    @Shadow
    @Final
    private Level level;

    @Inject(method = "setBlockEntity", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$migrateRejectedEnhancedSlabBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        BlockState state = blockEntity == null ? null : ((LevelChunk) (Object) this).getBlockState(blockEntity.getBlockPos());
        if (level instanceof ServerLevel serverLevel
                && FabricEnhancedSlabLegacyMigration.migrateRejectedBlockEntity(serverLevel, blockEntity, state)) {
            ci.cancel();
        }
    }

    @Inject(method = "promotePendingBlockEntity", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$migrateRejectedEnhancedSlabBlockEntityTag(BlockPos pos,
                                                                          CompoundTag tag,
                                                                          CallbackInfoReturnable<BlockEntity> cir) {
        BlockState state = ((LevelChunk) (Object) this).getBlockState(pos);
        if (level instanceof ServerLevel serverLevel
                && FabricEnhancedSlabLegacyMigration.migrateRejectedBlockEntityTag(serverLevel, pos, tag, state)) {
            cir.setReturnValue(null);
        }
    }
}
