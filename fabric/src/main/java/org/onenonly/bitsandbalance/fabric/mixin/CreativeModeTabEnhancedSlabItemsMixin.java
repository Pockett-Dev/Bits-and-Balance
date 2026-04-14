package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.fabric.registry.FabricCreativeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabEnhancedSlabItemsMixin {

    @Shadow
    private Collection<ItemStack> displayItems;

    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @Inject(method = "buildContents", at = @At("TAIL"))
    private void bitsandbalance$injectEnhancedSlabVariants(CreativeModeTab.ItemDisplayParameters parameters,
                                                           CallbackInfo ci) {
        FabricCreativeTabs.injectEnhancedSlabFamilyItems(
                (CreativeModeTab) (Object) this,
                this.displayItems,
                this.displayItemsSearchTab
        );
    }
}
