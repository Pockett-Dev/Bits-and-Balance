package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPickBlockHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerPickBlockMixin {
    @Shadow
    public ServerPlayer player;

    @Redirect(
            method = "handlePickItemFromBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getCloneItemStack(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/item/ItemStack;"
            ),
            require = 0
    )
    private ItemStack bitsandbalance$pickTargetedEnhancedSlabPart(BlockState state, LevelReader level, BlockPos pos, boolean includeData) {
        try {
            EnhancedSlabPickBlockHelper.PickTarget target = EnhancedSlabPickBlockHelper.resolve(level, this.player, pos, state);
            if (target != null && !target.stack().isEmpty()) {
                return target.stack().copy();
            }
        } catch (Throwable ignored) {
        }
        return state.getCloneItemStack(level, pos, includeData);
    }
}
