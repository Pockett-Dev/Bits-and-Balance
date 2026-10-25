package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHangIntentAccess;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHangIntentClientCache;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHangIntentHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.network.EnhancedSlabHangIntentSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class EnhancedSlabHangIntentPlacementMixin {
    @Unique
    private static final ThreadLocal<BlockPos> BITSANDBALANCE$PENDING_TARGET = new ThreadLocal<>();
    @Unique
    private static final ThreadLocal<Boolean> BITSANDBALANCE$PREAPPLIED_CLIENT_EXPLICIT = new ThreadLocal<>();

    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"))
    private void bitsandbalance$capturePlacementTarget(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!Config.enableEnhancedSlabs || context == null) {
            BITSANDBALANCE$PENDING_TARGET.remove();
            BITSANDBALANCE$PREAPPLIED_CLIENT_EXPLICIT.remove();
            return;
        }

        BlockPos targetPos = EnhancedSlabHangIntentHelper.getPlacementTarget(context);
        BITSANDBALANCE$PENDING_TARGET.set(targetPos);
        BITSANDBALANCE$PREAPPLIED_CLIENT_EXPLICIT.remove();

        Level level = context.getLevel();
        if (!level.isClientSide()) return;

        BlockItem self = (BlockItem) (Object) this;
        boolean explicit = EnhancedSlabHangIntentHelper.isIntentionalHangPlacement(context, targetPos, self.getBlock().defaultBlockState());
        if (!explicit) return;

        EnhancedSlabHangIntentClientCache.apply(targetPos.asLong(), true);
        EnhancedSlabHelper.invalidateVisualYOffsetCacheAround(level, targetPos);
        BITSANDBALANCE$PREAPPLIED_CLIENT_EXPLICIT.set(Boolean.TRUE);
    }

    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;", at = @At("RETURN"))
    private void bitsandbalance$syncExplicitHangIntent(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPos targetPos = BITSANDBALANCE$PENDING_TARGET.get();
        BITSANDBALANCE$PENDING_TARGET.remove();
        boolean preappliedClientExplicit = Boolean.TRUE.equals(BITSANDBALANCE$PREAPPLIED_CLIENT_EXPLICIT.get());
        BITSANDBALANCE$PREAPPLIED_CLIENT_EXPLICIT.remove();

        if (!Config.enableEnhancedSlabs || targetPos == null || context == null) return;

        InteractionResult result = cir.getReturnValue();
        Level rawLevel = context.getLevel();
        if (result == null || !result.consumesAction()) {
            if (rawLevel.isClientSide() && preappliedClientExplicit) {
                EnhancedSlabHangIntentClientCache.apply(targetPos.asLong(), false);
                EnhancedSlabHelper.invalidateVisualYOffsetCacheAround(rawLevel, targetPos);
            }
            return;
        }

        BlockState placedState = rawLevel.getBlockState(targetPos);
        if (!EnhancedSlabHelper.isVisualOffsetEligibleNonSlab(placedState)) {
            if (rawLevel.isClientSide() && preappliedClientExplicit) {
                EnhancedSlabHangIntentClientCache.apply(targetPos.asLong(), false);
                EnhancedSlabHelper.invalidateVisualYOffsetCacheAround(rawLevel, targetPos);
            }
            return;
        }

        boolean explicit = EnhancedSlabHangIntentHelper.isIntentionalHangPlacement(context, targetPos, placedState);
        if (rawLevel.isClientSide()) {
            EnhancedSlabHangIntentClientCache.apply(targetPos.asLong(), explicit);
            EnhancedSlabHelper.invalidateVisualYOffsetCacheAround(rawLevel, targetPos);
            rawLevel.sendBlockUpdated(targetPos, placedState, placedState, 3);
            return;
        }

        if (!(rawLevel instanceof ServerLevel level)) return;

        long posLong = targetPos.asLong();
        boolean changed = EnhancedSlabHangIntentAccess.get(level).set(posLong, explicit);
        if (changed) {
            EnhancedSlabHangIntentSync.sendUpdateToAll(level, posLong, explicit);
        }
    }
}
