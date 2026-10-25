package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.registry.NeoForgeVerticalSlabCreativeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Set;

@Mixin(value = CreativeModeTab.class, priority = 100)
public abstract class CreativeModeTabEnhancedSlabItemsMixin {

    @Shadow
    private Collection<ItemStack> displayItems;

    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @Inject(method = "buildContents", at = @At("TAIL"))
    private void bitsandbalance$injectEnhancedSlabVariants(CreativeModeTab.ItemDisplayParameters parameters,
                                                           CallbackInfo ci) {
        bitsandbalance$injectEnhancedSlabVariants();
    }

    @Inject(method = { "getDisplayItems", "getSearchTabDisplayItems" }, at = @At("HEAD"))
    private void bitsandbalance$injectEnhancedSlabVariantsBeforeAccess(CallbackInfoReturnable<Collection<ItemStack>> cir) {
        bitsandbalance$injectEnhancedSlabVariants();
    }

    private void bitsandbalance$injectEnhancedSlabVariants() {
        NeoForgeVerticalSlabCreativeTabs.injectEnhancedSlabFamilyItems(
                (CreativeModeTab) (Object) this,
                this.displayItems,
                this.displayItemsSearchTab
        );
    }
}
