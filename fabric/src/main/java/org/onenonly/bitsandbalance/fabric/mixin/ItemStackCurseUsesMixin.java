package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.onenonly.bitsandbalance.common.client.UsesForCursesClient;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackCurseUsesMixin {
    @Inject(
            method = "get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private void bitsandbalance$hidePumpkinCameraOverlayComponent(
            DataComponentType<?> componentType,
            CallbackInfoReturnable<Object> cir
    ) {
        if (componentType != DataComponents.EQUIPPABLE) return;

        ItemStack self = (ItemStack) (Object) this;
        if (!UsesForCursesClient.shouldSuppressEquippableComponent(self, componentType, FabricTweaksConfig.curseHidePumpkinOverlayOnVanishing)) {
            return;
        }

        Object current = cir.getReturnValue();
        if (current instanceof Equippable equippable) {
            cir.setReturnValue(UsesForCursesClient.withoutCameraOverlay(equippable));
        }
    }
}