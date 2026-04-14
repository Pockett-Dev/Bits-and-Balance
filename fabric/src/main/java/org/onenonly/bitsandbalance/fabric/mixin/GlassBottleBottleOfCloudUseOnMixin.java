package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudItemTransforms;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudPlacedGlass;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.11: BottleItem no longer overrides {@link Item#useOn}, so we hook the base method and
 * guard to only run when the item instance is a {@link BottleItem}.
 */
@Mixin(Item.class)
public abstract class GlassBottleBottleOfCloudUseOnMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$bottleCloudPickupCloudBlock(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!((Object) this instanceof BottleItem)) return;
        if (!BottleOfCloudRuntime.enabled) return;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != ModBottleOfCloud.cloudBlock()) return;

        Player player = context.getPlayer();
        if (player == null) return;

        ItemStack bottle = context.getItemInHand();
        ItemStack transformedStack = BottleOfCloudItemTransforms.fillCloudBottle(bottle, player);
        if (!level.isClientSide()) {
            player.setItemInHand(context.getHand(), transformedStack);
        }
        cir.setReturnValue(InteractionResult.SUCCESS.heldItemTransformedTo(transformedStack));

        if (level.isClientSide()) return;

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            BottleOfCloudPlacedGlass.cleanup(serverLevel, pos);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.9F, 1.0F);
    }
}
