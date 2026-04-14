package org.onenonly.bitsandbalance.fabric.loot;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.content.ModDogMusicDisc;
import org.onenonly.bitsandbalance.fabric.config.FabricLootConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricLootConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric replacement for NeoForge global loot modifiers.
 *
 * NeoForge uses data/neoforge/loot_modifiers + a custom LootModifier implementation.
 * Fabric does not have that system, so we inject equivalent pools into the target loot tables.
 *
 * Port status: currently uses NeoForge default weights/counts (no Fabric-side loot config yet).
 */
public final class FabricLootInjector {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-loot-injector");

    private static final Set<Identifier> MISSING_POTIONS_LOGGED = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceKey<Enchantment>> MISSING_ENCHANTMENTS_LOGGED = ConcurrentHashMap.newKeySet();

    private FabricLootInjector() {
    }

    public static void init() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!FabricLootConfig.enableCustomLootTables) return;

            Identifier id = key.identifier();
            String path = id.getPath();

            // Mineshaft
            if (path.equals("chests/abandoned_mineshaft")) {
                if (!FabricLootConfig.enableMineshaftLoot) return;

                if (FabricPotionConfig.enableResurfacingPotion && FabricLootConfig.enableMineshaftResurfacingPotion) {
                    addPotionChancePool(tableBuilder, idForPotion("resurfacing"),
                            FabricLootConfig.mineshaftResurfacingWeight,
                            FabricLootConfig.mineshaftResurfacingMinCount,
                            FabricLootConfig.mineshaftResurfacingMaxCount);
                }
                if (FabricLootConfig.enableMineshaftReturningPotion) {
                    addPotionChancePool(tableBuilder, idForPotion("returning"),
                            FabricLootConfig.mineshaftReturningWeight,
                            FabricLootConfig.mineshaftReturningMinCount,
                            FabricLootConfig.mineshaftReturningMaxCount);
                }
                if (FabricLootConfig.enableMineshaftHastePotion) {
                    addPotionChancePool(tableBuilder, idForPotion("haste"),
                            FabricLootConfig.mineshaftHasteWeight,
                            FabricLootConfig.mineshaftHasteMinCount,
                            FabricLootConfig.mineshaftHasteMaxCount);
                }
                if (FabricLootConfig.enableMineshaftStrongHastePotion) {
                    addPotionChancePool(tableBuilder, idForPotion("strong_haste"),
                            FabricLootConfig.mineshaftStrongHasteWeight,
                            FabricLootConfig.mineshaftStrongHasteMinCount,
                            FabricLootConfig.mineshaftStrongHasteMaxCount);
                }
                return;
            }

            // Stronghold
            if (path.equals("chests/stronghold_corridor") || path.equals("chests/stronghold_crossing")) {
                if (!FabricLootConfig.enableStrongholdLoot) return;

                if (FabricPotionConfig.enableResurfacingPotion && FabricLootConfig.enableStrongholdResurfacingPotion) {
                    addPotionChancePool(tableBuilder, idForPotion("resurfacing"),
                            FabricLootConfig.strongholdResurfacingWeight,
                            FabricLootConfig.strongholdResurfacingMinCount,
                            FabricLootConfig.strongholdResurfacingMaxCount);
                }
                if (FabricLootConfig.enableStrongholdReturningPotion) {
                    addPotionChancePool(tableBuilder, idForPotion("returning"),
                            FabricLootConfig.strongholdReturningWeight,
                            FabricLootConfig.strongholdReturningMinCount,
                            FabricLootConfig.strongholdReturningMaxCount);
                }
                return;
            }

            // Ancient City
            if (path.equals("chests/ancient_city")) {
                if (!FabricLootConfig.enableAncientCityLoot) return;

                if (FabricPotionConfig.enableResurfacingPotion && FabricLootConfig.enableAncientCityResurfacingPotion) {
                    addPotionChancePool(tableBuilder, idForPotion("resurfacing"),
                            FabricLootConfig.ancientCityResurfacingWeight,
                            FabricLootConfig.ancientCityResurfacingMinCount,
                            FabricLootConfig.ancientCityResurfacingMaxCount);
                }
                if (FabricLootConfig.enableAncientCityReturningPotion) {
                    addPotionChancePool(tableBuilder, idForPotion("returning"),
                            FabricLootConfig.ancientCityReturningWeight,
                            FabricLootConfig.ancientCityReturningMinCount,
                            FabricLootConfig.ancientCityReturningMaxCount);
                }
                if (FabricLootConfig.enableDogMusicDisc) {
                    addItemChancePool(tableBuilder, ModDogMusicDisc.musicDiscDog(), ModDogMusicDisc.ANCIENT_CITY_LOOT_CHANCE);
                }
                return;
            }

            // End City Treasure
            if (path.equals("chests/end_city_treasure")) {
                if (!FabricLootConfig.enableEndCityLoot) return;

                if (FabricLootConfig.enableEndCityAerodynamicBook) {
                    addAerodynamicBookChancePool(tableBuilder, (float) FabricLootConfig.endCityAerodynamicBookChance, registries);
                }
                if (FabricLootConfig.enableEndCityReturningPotion) {
                    addPotionChancePool(tableBuilder, idForPotion("returning"),
                            FabricLootConfig.endCityReturningWeight,
                            FabricLootConfig.endCityReturningMinCount,
                            FabricLootConfig.endCityReturningMaxCount);
                }
                return;
            }

            // Simple Dungeon
            if (path.equals("chests/simple_dungeon")) {
                if (!FabricLootConfig.enableSimpleDungeonLoot) return;

                if (FabricPotionConfig.enableResurfacingPotion && FabricLootConfig.enableSimpleDungeonResurfacingPotion) {
                    addPotionChancePool(tableBuilder, idForPotion("resurfacing"),
                            FabricLootConfig.simpleDungeonResurfacingWeight,
                            FabricLootConfig.simpleDungeonResurfacingMinCount,
                            FabricLootConfig.simpleDungeonResurfacingMaxCount);
                }
                if (FabricLootConfig.enableSimpleDungeonReturningPotion) {
                    addPotionChancePool(tableBuilder, idForPotion("returning"),
                            FabricLootConfig.simpleDungeonReturningWeight,
                            FabricLootConfig.simpleDungeonReturningMinCount,
                            FabricLootConfig.simpleDungeonReturningMaxCount);
                }
                if (FabricLootConfig.enableDogMusicDisc) {
                    addItemChancePool(tableBuilder, ModDogMusicDisc.musicDiscDog(), ModDogMusicDisc.SIMPLE_DUNGEON_LOOT_CHANCE);
                }
                return;
            }

            if (path.equals("chests/woodland_mansion")) {
                if (FabricLootConfig.enableDogMusicDisc) {
                    addItemChancePool(tableBuilder, ModDogMusicDisc.musicDiscDog(), ModDogMusicDisc.WOODLAND_MANSION_LOOT_CHANCE);
                }
                return;
            }

            // Trial Chambers
            if (path.equals("chests/trial_chambers/reward_common")) {
                if (!FabricLootConfig.enableTrialChamberLoot) return;
                if (FabricPotionConfig.enableResurfacingPotion && FabricLootConfig.enableTrialCommonResurfacingPotion) {
                    addPotionChancePool(tableBuilder, idForPotion("resurfacing"),
                            FabricLootConfig.trialCommonResurfacingWeight,
                            FabricLootConfig.trialCommonResurfacingMinCount,
                            FabricLootConfig.trialCommonResurfacingMaxCount);
                }
            } else if (path.equals("chests/trial_chambers/reward_rare")) {
                if (!FabricLootConfig.enableTrialChamberLoot) return;
                if (FabricPotionConfig.enableResurfacingPotion && FabricLootConfig.enableTrialRareResurfacingPotion) {
                    addPotionChancePool(tableBuilder, idForPotion("resurfacing"),
                            FabricLootConfig.trialRareResurfacingWeight,
                            FabricLootConfig.trialRareResurfacingMinCount,
                            FabricLootConfig.trialRareResurfacingMaxCount);
                }
                if (FabricLootConfig.enableTrialRareReturningPotion) {
                    addPotionChancePool(tableBuilder, idForPotion("returning"),
                            FabricLootConfig.trialRareReturningWeight,
                            FabricLootConfig.trialRareReturningMinCount,
                            FabricLootConfig.trialRareReturningMaxCount);
                }
            } else if (path.equals("chests/trial_chambers/reward_unique")) {
                if (!FabricLootConfig.enableTrialChamberLoot) return;
                if (FabricLootConfig.enableTrialUniqueReturningPotion) {
                    addPotionChancePool(tableBuilder, idForPotion("returning"),
                            FabricLootConfig.trialUniqueReturningWeight,
                            FabricLootConfig.trialUniqueReturningMinCount,
                            FabricLootConfig.trialUniqueReturningMaxCount);
                }
            }
        });
    }

    private static Identifier idForPotion(String path) {
        return Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, path);
    }

    private static void addAerodynamicBookChancePool(net.minecraft.world.level.storage.loot.LootTable.Builder tableBuilder,
                                                     float chance,
                                 HolderLookup.Provider registries) {
        ResourceKey<Enchantment> aerodynamicKey = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "aerodynamic")
        );

        Holder<Enchantment> aerodynamic = registries.lookupOrThrow(Registries.ENCHANTMENT)
            .get(aerodynamicKey)
                .orElse(null);
        if (aerodynamic == null) {
            warnMissingEnchantmentOnce(aerodynamicKey);
            return;
        }

        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                .apply(new SetEnchantmentsFunction.Builder().withEnchantment(aerodynamic, ConstantValue.exactly(1))));

        tableBuilder.withPool(pool);
    }

    private static void addPotionChancePool(net.minecraft.world.level.storage.loot.LootTable.Builder tableBuilder,
                                           Identifier potionId,
                                           int weight,
                                           int minCount,
                                           int maxCount) {
        double chance = (double) weight / (weight + 100.0D);

        Potion potionValue = BuiltInRegistries.POTION.getOptional(potionId).orElse(null);
        if (potionValue == null) {
            warnMissingPotionOnce(potionId);
            return;
        }
        Holder<Potion> potion = BuiltInRegistries.POTION.wrapAsHolder(potionValue);

        LootPool.Builder pool = LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance((float) chance))
                .add(LootItem.lootTableItem(Items.POTION)
                        .apply(SetPotionFunction.setPotion(potion))
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(minCount, maxCount))));

        tableBuilder.withPool(pool);
    }

    private static void addItemChancePool(net.minecraft.world.level.storage.loot.LootTable.Builder tableBuilder,
                                          Item item,
                                          float chance) {
        if (item == null || item == Items.AIR) {
            return;
        }

        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(LootItem.lootTableItem(item));

        tableBuilder.withPool(pool);
    }

    private static void warnMissingPotionOnce(Identifier potionId) {
        if (!FabricLootConfig.logMissingIds) return;
        if (!MISSING_POTIONS_LOGGED.add(potionId)) return;
        LOGGER.warn("Loot injection skipped: missing potion {} (is it disabled or not registered?)", potionId);
    }

    private static void warnMissingEnchantmentOnce(ResourceKey<Enchantment> enchantmentKey) {
        if (!FabricLootConfig.logMissingIds) return;
        if (!MISSING_ENCHANTMENTS_LOGGED.add(enchantmentKey)) return;
        LOGGER.warn("Loot injection skipped: missing enchantment {}", enchantmentKey.identifier());
    }
}
