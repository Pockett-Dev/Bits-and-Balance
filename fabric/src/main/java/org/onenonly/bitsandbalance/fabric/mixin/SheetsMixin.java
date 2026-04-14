package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.registry.ModWoodTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sheets.class)
public abstract class SheetsMixin {
    @Unique
    private static Material bitsandbalance$azaleaSignMaterial;

    @Unique
    private static Material bitsandbalance$azaleaHangingSignMaterial;
    
    @Unique
    private static boolean bitsandbalance$isInitializing = false;

    @Inject(method = "getSignMaterial", at = @At("HEAD"), cancellable = true)
    private static void bitsandbalance$getSignMaterial(WoodType woodType, CallbackInfoReturnable<Material> cir) {
        // If our azalea wood type couldn't be registered and fell back to OAK, never override
        // oak's sign materials (that would make vanilla oak signs render with azalea textures).
        if (ModWoodTypes.AZALEA_WOOD_TYPE == WoodType.OAK || woodType == WoodType.OAK) {
            return;
        }

        if (woodType != ModWoodTypes.AZALEA_WOOD_TYPE || bitsandbalance$isInitializing) {
            return;
        }

        if (bitsandbalance$azaleaSignMaterial == null) {
            bitsandbalance$isInitializing = true;
            try {
                Material oak = Sheets.getSignMaterial(WoodType.OAK);
                bitsandbalance$azaleaSignMaterial = new Material(
                        oak.atlasLocation(),
                    Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "entity/signs/azalea")
                );
            } finally {
                bitsandbalance$isInitializing = false;
            }
        }

        cir.setReturnValue(bitsandbalance$azaleaSignMaterial);
    }

    @Inject(method = "getHangingSignMaterial", at = @At("HEAD"), cancellable = true)
    private static void bitsandbalance$getHangingSignMaterial(WoodType woodType, CallbackInfoReturnable<Material> cir) {
        // See comment in bitsandbalance$getSignMaterial.
        if (ModWoodTypes.AZALEA_WOOD_TYPE == WoodType.OAK || woodType == WoodType.OAK) {
            return;
        }

        if (woodType != ModWoodTypes.AZALEA_WOOD_TYPE || bitsandbalance$isInitializing) {
            return;
        }

        if (bitsandbalance$azaleaHangingSignMaterial == null) {
            bitsandbalance$isInitializing = true;
            try {
                Material oak = Sheets.getHangingSignMaterial(WoodType.OAK);
                bitsandbalance$azaleaHangingSignMaterial = new Material(
                        oak.atlasLocation(),
                    Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "entity/signs/hanging/azalea")
                );
            } finally {
                bitsandbalance$isInitializing = false;
            }
        }

        cir.setReturnValue(bitsandbalance$azaleaHangingSignMaterial);
    }
}
