package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.registry.ModWoodTypes;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Sheets.class)
public abstract class SheetsMixin {
    @Unique
    private static final Logger BITSANDBALANCE$SPECIAL_ITEM_LOGGER = LoggerFactory.getLogger("bitsandbalance-special-item-debug");

    @Unique
    private static final Set<String> BITSANDBALANCE$LOGGED_TRANSLUCENT_SHEET_KEYS = ConcurrentHashMap.newKeySet();

    @Unique
    private static final ResourceLocation BITSANDBALANCE$SHULKER_SHEET_ATLAS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/shulker_boxes.png");

    @Unique
    private static final ResourceLocation BITSANDBALANCE$BED_SHEET_ATLAS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/beds.png");

    @Unique
    private static final ResourceLocation BITSANDBALANCE$BANNER_SHEET_ATLAS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/banner_patterns.png");

    @Unique
    private static final ResourceLocation BITSANDBALANCE$SHIELD_SHEET_ATLAS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/shield_patterns.png");

    @Unique
    private static Material bitsandbalance$azaleaSignMaterial;

    @Unique
    private static Material bitsandbalance$azaleaHangingSignMaterial;
    
    @Unique
    private static boolean bitsandbalance$isInitializing = false;

    @Unique
    private static boolean bitsandbalance$shouldUseTranslucentSpecialItemSheet() {
        return ItemShareClient.isRenderingWithAlpha() && ItemShareClient.alphaValue < 1.0F;
    }

    @Unique
    private static void bitsandbalance$logTranslucentSheetOverride(String sheetType) {
        String itemKey = ItemShareClient.getTrackedRenderedItemKey();
        String key = sheetType + "|" + itemKey;
        if (itemKey.isEmpty() || !BITSANDBALANCE$LOGGED_TRANSLUCENT_SHEET_KEYS.add(key)) {
            return;
        }

        BITSANDBALANCE$SPECIAL_ITEM_LOGGER.info(
                "Tracked translucent special sheet override: item={} sheet={} alpha={}",
                itemKey,
                sheetType,
                ItemShareClient.alphaValue
        );
    }

    @Inject(method = "bannerSheet", at = @At("HEAD"), cancellable = true, require = 0)
    private static void bitsandbalance$useTranslucentBannerSheet(CallbackInfoReturnable<RenderType> cir) {
        if (bitsandbalance$shouldUseTranslucentSpecialItemSheet()) {
            bitsandbalance$logTranslucentSheetOverride("banner");
            cir.setReturnValue(RenderType.entityTranslucent(BITSANDBALANCE$BANNER_SHEET_ATLAS));
        }
    }

    @Inject(method = "shieldSheet", at = @At("HEAD"), cancellable = true, require = 0)
    private static void bitsandbalance$useTranslucentShieldSheet(CallbackInfoReturnable<RenderType> cir) {
        if (bitsandbalance$shouldUseTranslucentSpecialItemSheet()) {
            bitsandbalance$logTranslucentSheetOverride("shield");
            cir.setReturnValue(RenderType.entityTranslucent(BITSANDBALANCE$SHIELD_SHEET_ATLAS));
        }
    }

    @Inject(method = "bedSheet", at = @At("HEAD"), cancellable = true, require = 0)
    private static void bitsandbalance$useTranslucentBedSheet(CallbackInfoReturnable<RenderType> cir) {
        if (bitsandbalance$shouldUseTranslucentSpecialItemSheet()) {
            bitsandbalance$logTranslucentSheetOverride("bed");
            cir.setReturnValue(RenderType.entityTranslucent(BITSANDBALANCE$BED_SHEET_ATLAS));
        }
    }

    @Inject(method = "shulkerBoxSheet", at = @At("HEAD"), cancellable = true, require = 0)
    private static void bitsandbalance$useTranslucentShulkerSheet(CallbackInfoReturnable<RenderType> cir) {
        if (bitsandbalance$shouldUseTranslucentSpecialItemSheet()) {
            bitsandbalance$logTranslucentSheetOverride("shulker");
            cir.setReturnValue(RenderType.entityTranslucent(BITSANDBALANCE$SHULKER_SHEET_ATLAS));
        }
    }

    @Inject(method = "getSignMaterial", at = @At("HEAD"), cancellable = true)
    private static void bitsandbalance$getSignMaterial(WoodType woodType, CallbackInfoReturnable<Material> cir) {
        // Never override oak's sign materials; only our distinct azalea wood type should use these textures.
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
                    ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "entity/signs/azalea")
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
                    ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "entity/signs/hanging/azalea")
                );
            } finally {
                bitsandbalance$isInitializing = false;
            }
        }

        cir.setReturnValue(bitsandbalance$azaleaHangingSignMaterial);
    }
}
