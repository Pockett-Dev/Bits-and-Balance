package org.onenonly.bitsandbalance.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onenonly.bitsandbalance.worldgen.feature.VeinFeature;
import org.onenonly.bitsandbalance.worldgen.feature.MojangStyleVeinFeature;
import org.onenonly.bitsandbalance.worldgen.placement.AirExposureFilter;
import org.onenonly.bitsandbalance.worldgen.placement.ConfigEnabledFilter;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.content.ModAzalea;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.common.content.ModDogMusicDisc;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;
import org.onenonly.bitsandbalance.common.content.ModKiln;
import org.onenonly.bitsandbalance.common.content.ModOreVariants;
import org.onenonly.bitsandbalance.common.content.ModRawQuartz;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.compat.BlockEntityTypeExt;
import org.onenonly.bitsandbalance.fabric.compat.FabricCookingFuelRegistry;
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricCombatConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricContentConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricEnchantingConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricLootConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricRecipesConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricWorldgenConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricMobsConfig;
import org.onenonly.bitsandbalance.fabric.combat.FabricCombatEvents;
import org.onenonly.bitsandbalance.fabric.loot.FabricLootInjector;
import org.onenonly.bitsandbalance.fabric.potions.FabricCustomBrewing;
import org.onenonly.bitsandbalance.fabric.potions.FabricCustomBrewingResourceCondition;
import org.onenonly.bitsandbalance.fabric.registry.FabricContentRegistrar;
import org.onenonly.bitsandbalance.fabric.registry.FabricCandleBundleContent;
import org.onenonly.bitsandbalance.fabric.registry.FabricBlockEntities;
import org.onenonly.bitsandbalance.fabric.registry.FabricCreativeTabs;
import org.onenonly.bitsandbalance.fabric.registry.FabricEntityTypes;
import org.onenonly.bitsandbalance.fabric.registry.FabricEffects;
import org.onenonly.bitsandbalance.fabric.registry.FabricPotions;
import org.onenonly.bitsandbalance.fabric.worldgen.FabricWorldgen;
import org.onenonly.bitsandbalance.fabric.worldgen.ConfigAwareAzaleaTrunkProvider;
import org.onenonly.bitsandbalance.fabric.network.FabricItemShareNetworking;
import org.onenonly.bitsandbalance.fabric.network.FabricBioluminescenceSync;
import org.onenonly.bitsandbalance.fabric.network.FabricEnhancedSlabHangIntentSync;
import org.onenonly.bitsandbalance.fabric.network.FabricEnhancedSlabMaterialSync;
import org.onenonly.bitsandbalance.fabric.network.FabricPayloadTypes;
import org.onenonly.bitsandbalance.fabric.network.FabricSoulFireBurnSync;
import org.onenonly.bitsandbalance.fabric.network.FabricSittingNetworking;

import org.onenonly.bitsandbalance.fabric.network.FabricNavigatorCompassNetworking;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricCrawlingMechanic;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricLeashedTeleport;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricDoorKnocking;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricEnhancedSlabLegacyMigration;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricQuickHarvesting;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricCozyCampfire;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricNavigatorCompass;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricVillagersFollowEmeralds;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricDismountEntities;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricSpeedyWolves;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricFriendlyFireFriendlies;
import org.onenonly.bitsandbalance.fabric.mobs.FabricDepthScalingEnemies;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricCoyoteTimeJump;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricCreeperSunlightBurn;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricCompostableItems;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricEnderdragonEggAlways;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricFuelTweaks;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricDespawnWithMaster;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricDoubleDoorOpeningFallback;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricImprovedLadders;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricSophisticatedScaffolding;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricImprovedRecoveryCompass;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricTreasureEnchantmentGoldColor;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricMoreMiningXp;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricCascadingLadders;
import org.onenonly.bitsandbalance.fabric.tweaks.FabricUnbreakableTrialSpawners;
import org.onenonly.bitsandbalance.fabric.events.FabricDogMusicDiscDrops;
import org.onenonly.bitsandbalance.fabric.candle.FabricCandleBundleSync;
import org.onenonly.bitsandbalance.fabric.recipe.FabricAlternativeRecipes;
import org.onenonly.bitsandbalance.fabric.recipe.FabricDynamicRecipeDatapackCoordinator;
import org.onenonly.bitsandbalance.fabric.recipe.FabricGeneratedWorldDataPacks;
import org.onenonly.bitsandbalance.fabric.recipe.FabricRecipeConditions;
import org.onenonly.bitsandbalance.fabric.recipe.FabricStepRecipes;
import org.onenonly.bitsandbalance.fabric.recipe.FabricVerticalSlabRecipes;
import org.onenonly.bitsandbalance.fabric.recipe.FabricWoodStonecuttingRecipes;
import org.onenonly.bitsandbalance.fabric.worldgen.FabricOreVariantReplacer;
import net.fabricmc.loader.api.FabricLoader;

public class BitsAndBalanceFabric implements ModInitializer {
        public static final String MOD_ID = BitsAndBalanceCommon.MOD_ID;
        private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Minimal set of registries needed for existing JSON-driven content.
    public static final Attribute AERODYNAMIC_DRAG = new RangedAttribute(
            "attribute.name.bitsandbalance.aerodynamic_drag",
            0.0D,
            0.0D,
            1024.0D
    ).setSyncable(true);

    @Override
    public void onInitialize() {
                BitsAndBalanceCommon.init();
                org.onenonly.bitsandbalance.fabric.config.FabricGroupedConfigMigration.migrateIfNeeded();
                FabricWorldgenConfig.init();
        FabricBalanceConfig.init();
        FabricCombatConfig.init();
                FabricContentConfig.init();
        FabricEnchantingConfig.init();
        FabricMechanicsConfig.init();
        FabricMobsConfig.init();
        FabricPotionConfig.init();
        FabricCustomBrewingResourceCondition.register();
        FabricRecipesConfig.init();
        FabricBuildingConfig.init();
        FabricTweaksConfig.init();
        FabricLootConfig.init();
        FabricRecipeConditions.init();
        FabricGeneratedWorldDataPacks.initializeDirectories();
        FabricVerticalSlabRecipes.init();
        FabricStepRecipes.init();
        FabricDynamicRecipeDatapackCoordinator.init();
                if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
                        FabricAlternativeRecipes.init();
                        FabricWoodStonecuttingRecipes.init();
                }
        FabricCombatEvents.init();
        // Ported NeoForge gameplay systems
        FabricDepthScalingEnemies.init();
        FabricDismountEntities.init();
        FabricSpeedyWolves.init();
        FabricFriendlyFireFriendlies.init();

        // Networking payload types must be registered before any receivers are registered.
        FabricPayloadTypes.register();
        org.onenonly.bitsandbalance.fabric.network.FabricBiomeTitlesStructureSync.initServer();
        FabricSittingNetworking.initServer();
        FabricBioluminescenceSync.initServer();
        FabricSoulFireBurnSync.initServer();
        FabricItemShareNetworking.initServer();
        org.onenonly.bitsandbalance.fabric.network.FabricArrowSelectionNetworking.initServer();
        FabricEnhancedSlabMaterialSync.initServer();
        FabricCrawlingMechanic.initServer();
        FabricDoorKnocking.initServer();
        FabricNavigatorCompassNetworking.initServer();
        FabricQuickHarvesting.init();
        FabricCozyCampfire.init();
        FabricNavigatorCompass.initServer();
        FabricVillagersFollowEmeralds.init();

        // Candle bundle container (block + block entity) used to render mixed candle types.
        FabricCandleBundleContent.register();
        // Mixed double slab block (two different slab types in one position).
        org.onenonly.bitsandbalance.fabric.registry.FabricMixedSlabContent.register();
        // Vertical slab block (stores one or two slab halves oriented horizontally).
        org.onenonly.bitsandbalance.fabric.registry.FabricVerticalSlabContent.register();
        // Step blocks (??-block steps in 4 quadrant positions, one per slab type).
        org.onenonly.bitsandbalance.fabric.registry.FabricStepContent.register();
        // Vertical step blocks (??-block steps oriented vertically, one per vertical-slab type).
        org.onenonly.bitsandbalance.fabric.registry.FabricVerticalStepContent.register();
        registerRegistries();
        FabricBlockEntities.register();
        FabricEnhancedSlabLegacyMigration.init();
        FabricCreativeTabs.register();
        FabricEffects.register();
        FabricPotions.register();
        FabricCustomBrewing.init();
        FabricLootInjector.init();
                FabricLeashedTeleport.init();
                FabricWorldgen.init();
        FabricOreVariantReplacer.init();
                registerAzaleaWoodsetBehaviors();
        registerAzaleaBoatDispenserBehavior();
        org.onenonly.bitsandbalance.common.mechanics.GlowGooDispenserBehavior.register();
        registerAzaleaWoodsetFuelValues();
        registerDynamicVariantFuelValues();
        registerVanillaBlockEntitySupport();

        // Initialize tweak features
        FabricCoyoteTimeJump.init();
        FabricFuelTweaks.init();
        FabricDespawnWithMaster.init();
        FabricDoubleDoorOpeningFallback.init();
        FabricImprovedLadders.init();
        FabricSophisticatedScaffolding.init();
        FabricImprovedRecoveryCompass.init();
        FabricDogMusicDiscDrops.init();
        FabricTreasureEnchantmentGoldColor.init();
                FabricMoreMiningXp.init();
        FabricCascadingLadders.init();
        org.onenonly.bitsandbalance.fabric.tweaks.FabricCascadingOreMining.init();
        FabricCompostableItems.init();
        FabricCreeperSunlightBurn.init();
        FabricEnderdragonEggAlways.init();
                FabricCandleBundleSync.init();
        FabricEnhancedSlabHangIntentSync.init();
        FabricUnbreakableTrialSpawners.init();

        // Inject tool-related block tags into dynamic vertical slab blocks after tags load,
        // so that Jade (and any other tag-based system) can display the correct tool icon.
        org.onenonly.bitsandbalance.fabric.registry.FabricTagInjection.init();

        LOGGER.info("BitsAndBalance Fabric initialized");
    }


        private static void registerAzaleaWoodsetBehaviors() {
                if (!FabricWorldgenConfig.isAzaleaWoodsetEnabled()) {
                        return;
                }
                // 26.3 moved axe transformations into data components. Extend stripping through
                // Fabric's interaction event so the custom blocks retain their vanilla behavior.
                UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
                        var stack = player.getItemInHand(hand);
                        if (!stack.is(ItemTags.AXES)) {
                                return InteractionResult.PASS;
                        }
                        if (hand == InteractionHand.MAIN_HAND
                                && player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)
                                && !player.isSecondaryUseActive()) {
                                return InteractionResult.PASS;
                        }
                        var pos = hitResult.getBlockPos();
                        var state = level.getBlockState(pos);
                        Block replacement = state.is(ModAzalea.AZALEA_LOG)
                                ? ModAzalea.STRIPPED_AZALEA_LOG
                                : state.is(ModAzalea.AZALEA_WOOD) ? ModAzalea.STRIPPED_AZALEA_WOOD : null;
                        if (replacement == null) {
                                return InteractionResult.PASS;
                        }
                        BlockState replacementState = replacement.withPropertiesOf(state);
                        if (player instanceof ServerPlayer serverPlayer) {
                                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
                        }
                        stack.hurtAndBreak(1, player, hand);
                        level.setBlock(pos, replacementState, Block.UPDATE_ALL_IMMEDIATE);
                        level.playSound(player, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, replacementState));
                        return InteractionResult.SUCCESS;
                });

                // Flammability (match common vanilla wood defaults)
                FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();

                flammable.add(ModAzalea.AZALEA_LOG, 5, 5);
                flammable.add(ModAzalea.AZALEA_WOOD, 5, 5);
                flammable.add(ModAzalea.STRIPPED_AZALEA_LOG, 5, 5);
                flammable.add(ModAzalea.STRIPPED_AZALEA_WOOD, 5, 5);

                flammable.add(ModAzalea.AZALEA_PLANKS, 5, 20);
                flammable.add(ModAzalea.AZALEA_SLAB, 5, 20);
                flammable.add(ModAzalea.AZALEA_STAIRS, 5, 20);
                flammable.add(ModAzalea.AZALEA_SHELF, 5, 20);
                flammable.add(ModAzalea.AZALEA_BUTTON, 5, 20);
                flammable.add(ModAzalea.AZALEA_PRESSURE_PLATE, 5, 20);
                flammable.add(ModAzalea.AZALEA_FENCE, 5, 20);
                flammable.add(ModAzalea.AZALEA_FENCE_GATE, 5, 20);
                flammable.add(ModAzalea.AZALEA_DOOR, 5, 20);
                flammable.add(ModAzalea.AZALEA_TRAPDOOR, 5, 20);

                flammable.add(ModAzalea.AZALEA_SIGN, 5, 20);
                flammable.add(ModAzalea.AZALEA_WALL_SIGN, 5, 20);
                flammable.add(ModAzalea.AZALEA_HANGING_SIGN, 5, 20);
                flammable.add(ModAzalea.AZALEA_WALL_HANGING_SIGN, 5, 20);
        }

        private static void registerAzaleaBoatDispenserBehavior() {
                if (!FabricWorldgenConfig.isAzaleaWoodsetEnabled()) {
                        return;
                }

                var oakBoatBehavior = DispenserBlock.DISPENSER_REGISTRY.get(Items.OAK_BOAT);
                var oakChestBoatBehavior = DispenserBlock.DISPENSER_REGISTRY.get(Items.OAK_CHEST_BOAT);

                                BuiltInRegistries.ITEM.get(ModAzalea.AZALEA_BOAT_ID)
                                                .ifPresent(holder -> DispenserBlock.registerBehavior(holder.value(), oakBoatBehavior));
                                BuiltInRegistries.ITEM.get(ModAzalea.AZALEA_CHEST_BOAT_ID)
                                                .ifPresent(holder -> DispenserBlock.registerBehavior(holder.value(), oakChestBoatBehavior));
        }

        private static void registerAzaleaWoodsetFuelValues() {
                if (!FabricWorldgenConfig.isAzaleaWoodsetEnabled()) {
                        return;
                }

                // Match NeoForge values in CommonSetup.
                registerFuel(ModAzalea.AZALEA_LOG_ID, 300);
                registerFuel(ModAzalea.AZALEA_WOOD_ID, 300);
                registerFuel(ModAzalea.STRIPPED_AZALEA_LOG_ID, 300);
                registerFuel(ModAzalea.STRIPPED_AZALEA_WOOD_ID, 300);
                registerFuel(ModAzalea.AZALEA_PLANKS_ID, 300);
                registerFuel(ModAzalea.AZALEA_SLAB_ID, 150);
                registerFuel(ModAzalea.AZALEA_STAIRS_ID, 300);
                registerFuel(ModAzalea.AZALEA_SHELF_ID, 300);
                registerFuel(ModAzalea.AZALEA_FENCE_ID, 300);
                registerFuel(ModAzalea.AZALEA_FENCE_GATE_ID, 300);
                registerFuel(ModAzalea.AZALEA_DOOR_ID, 200);
                registerFuel(ModAzalea.AZALEA_TRAPDOOR_ID, 200);
                registerFuel(ModAzalea.AZALEA_SIGN_ID, 200);
                registerFuel(ModAzalea.AZALEA_HANGING_SIGN_ID, 200);
                registerFuel(ModAzalea.AZALEA_BUTTON_ID, 100);
                registerFuel(ModAzalea.AZALEA_PRESSURE_PLATE_ID, 100);
                registerFuel(ModAzalea.AZALEA_BOAT_ID, 1200);
                registerFuel(ModAzalea.AZALEA_CHEST_BOAT_ID, 1200);
        }

        private static void registerDynamicVariantFuelValues() {
                for (Block verticalBlock : VerticalSlabDynamicRegistry.getAllVerticalBlocksSnapshot()) {
                        if (verticalBlock instanceof FixedVerticalSlabBlock) {
                                registerDerivedFuel(verticalBlock, DynamicVariantMaterialHelper.VERTICAL_SLAB_BURN_TIME);
                        }
                }

                for (Block stepBlock : StepDynamicRegistry.getAllStepBlocksSnapshot()) {
                        if (stepBlock instanceof FixedStepBlock) {
                                registerDerivedFuel(stepBlock, DynamicVariantMaterialHelper.STEP_BURN_TIME);
                        }
                }

                for (Block verticalStepBlock : VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot()) {
                        if (verticalStepBlock instanceof FixedVerticalStepBlock) {
                                registerDerivedFuel(verticalStepBlock, DynamicVariantMaterialHelper.STEP_BURN_TIME);
                        }
                }
        }

        private static void registerDerivedFuel(Block block, int burnTime) {
                Block sourceSlab = DynamicVariantMaterialHelper.getFuelSourceSlab(block);
                if (sourceSlab != null && block.asItem() != Items.AIR) {
                        FabricCookingFuelRegistry.registerDerived(block.asItem(), sourceSlab, burnTime);
                }
        }

        private static void registerFuel(Identifier itemId, int burnTime) {
                BuiltInRegistries.ITEM.get(itemId).ifPresent(holder -> FabricCookingFuelRegistry.register(holder.value(), burnTime));
        }

        private static void registerFuel(Block block, int burnTime) {
                if (burnTime > 0 && block.asItem() != Items.AIR) {
                        FabricCookingFuelRegistry.register(block.asItem(), burnTime);
                }
        }

    private static void registerVanillaBlockEntitySupport() {
        // Vanilla sign block entities validate the block state against a fixed set of supported blocks.
        // Add our custom sign blocks so placing them doesn't crash.
        ((BlockEntityTypeExt) (Object) BlockEntityType.SIGN).bitsandbalance$addValidBlocks(
                ModAzalea.AZALEA_SIGN,
                ModAzalea.AZALEA_WALL_SIGN
        );
        ((BlockEntityTypeExt) (Object) BlockEntityType.HANGING_SIGN).bitsandbalance$addValidBlocks(
                ModAzalea.AZALEA_HANGING_SIGN,
                ModAzalea.AZALEA_WALL_HANGING_SIGN
        );
        ((BlockEntityTypeExt) (Object) BlockEntityType.SHELF).bitsandbalance$addValidBlocks(
                ModAzalea.AZALEA_SHELF
        );

        LOGGER.info("Registered azalea sign and shelf blocks with vanilla block entity types");
    }

    private static void registerRegistries() {
        Registry.register(
                BuiltInRegistries.ATTRIBUTE,
                Identifier.fromNamespaceAndPath(MOD_ID, "aerodynamic_drag"),
                AERODYNAMIC_DRAG
        );

        Registry.register(
                BuiltInRegistries.FEATURE_TYPE,
                Identifier.fromNamespaceAndPath(MOD_ID, "noodle_vein"),
                VeinFeature.CODEC
        );

        Registry.register(
                BuiltInRegistries.FEATURE_TYPE,
                Identifier.fromNamespaceAndPath(MOD_ID, "mojang_style_vein"),
                MojangStyleVeinFeature.CODEC
        );

        Registry.register(
                BuiltInRegistries.PLACEMENT_MODIFIER_TYPE,
                Identifier.fromNamespaceAndPath(MOD_ID, "air_exposure_filter"),
                AirExposureFilter.CODEC
        );

        Registry.register(
                BuiltInRegistries.PLACEMENT_MODIFIER_TYPE,
                Identifier.fromNamespaceAndPath(MOD_ID, "config_enabled"),
                ConfigEnabledFilter.CODEC
        );

        Registry.register(
                BuiltInRegistries.BLOCK_STATE_PROVIDER_TYPE,
                Identifier.fromNamespaceAndPath(MOD_ID, "config_aware_azalea_trunk"),
                ConfigAwareAzaleaTrunkProvider.CODEC
        );

        FabricContentRegistrar registrar = new FabricContentRegistrar();
        ModAzalea.register(registrar);
        ModOreVariants.register(registrar);
        ModRawQuartz.register(registrar);
        ModKiln.register(registrar);
        ModBottleOfCloud.register(registrar);
        ModGlowGoo.register(registrar);
        ModDogMusicDisc.register(registrar);
                FabricEntityTypes.register();
    }
}
