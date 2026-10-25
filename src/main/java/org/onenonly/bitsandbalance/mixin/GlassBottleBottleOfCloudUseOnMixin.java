package org.onenonly.bitsandbalance.mixin;

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
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudPlacedGlass;
//? endif
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Glass-bottle block interaction routes through Item#useOn, so cloud pickup
 * must hook the base item method and guard to BottleItem instances.
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
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        if (state.getBlock() != ModBottleOfCloud.cloudBlock()) return;
        //? else
        if (!state.is(ModBottleOfCloud.cloudBlock())) return;
        //? endif

        Player player = context.getPlayer();
        if (player == null) return;

        ItemStack bottle = context.getItemInHand();
        ItemStack transformedStack = BottleOfCloudItemTransforms.fillCloudBottle(bottle, player);
        if (!level.isClientSide()) {
            player.setItemInHand(context.getHand(), transformedStack);
        }
        //? if mc == 1.21.1
        //? if fabric
        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide()));
        //? else
        cir.setReturnValue(InteractionResult.SUCCESS);
        //? endif
        //? else
        cir.setReturnValue(InteractionResult.SUCCESS.heldItemTransformedTo(transformedStack));
        //? endif

        if (level.isClientSide()) return;

        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            BottleOfCloudPlacedGlass.cleanup(serverLevel, pos);
        }
        //? endif
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.playSound(
            null,
            pos.getX() + 0.5D,
            pos.getY() + 0.5D,
            pos.getZ() + 0.5D,
            SoundEvents.BOTTLE_FILL,
            SoundSource.PLAYERS,
            0.9F,
            1.0F
        );
    }
}
