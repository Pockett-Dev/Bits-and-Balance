package org.onenonly.bitsandbalance.fabric.mixin;

//? if mc != 1.21.1
import net.minecraft.core.component.DataComponents;
//? endif
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
//? if mc == 1.21.1
import net.minecraft.world.item.UseAnim;
//? endif
//? if mc != 1.21.1
import net.minecraft.world.item.component.BlocksAttacks;
//? endif
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Responsive Shields
 * Allows configuring shield raise time by controlling when an item counts as blocking.
 */
@Mixin(LivingEntity.class)
public abstract class ResponsiveShieldsMixin {

    @Inject(method =
//? if mc == 1.21.1
            "isBlocking",
//? else
            "getItemBlockingWith",
//? endif
            at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$getItemBlockingWith(CallbackInfoReturnable<
//? if mc == 1.21.1
            Boolean
//? else
            ItemStack
//? endif
            > cir) {
        if (!FabricTweaksConfig.enableResponsiveShields) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.isUsingItem()) {
            return;
        }

        ItemStack useItem = self.getUseItem();
        if (useItem.isEmpty()) {
            return;
        }

        //? if mc == 1.21.1
        if (useItem.getUseAnimation() != UseAnim.BLOCK) return;
        //? else
        BlocksAttacks blocksAttacks = useItem.get(DataComponents.BLOCKS_ATTACKS);
        if (blocksAttacks == null) return;
        //? endif

        int configuredDelay = Math.max(0, FabricTweaksConfig.shieldRaiseTime);
        int ticksUsing = useItem.getItem().getUseDuration(useItem, self) - self.getUseItemRemainingTicks();
        if (ticksUsing >= configuredDelay) {
            //? if mc == 1.21.1
            cir.setReturnValue(true);
            //? else
            cir.setReturnValue(useItem);
            //? endif
        } else {
            //? if mc == 1.21.1
            cir.setReturnValue(false);
            //? else
            cir.setReturnValue(null);
            //? endif
        }
    }
}
