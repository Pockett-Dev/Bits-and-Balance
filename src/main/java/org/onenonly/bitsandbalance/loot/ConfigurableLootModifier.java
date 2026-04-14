package org.onenonly.bitsandbalance.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.ModPotions;
import org.onenonly.bitsandbalance.common.content.ModDogMusicDisc;

/**
 * Configurable loot modifier that adds Resurfacing and Returning Potions to various loot tables
 * based on configuration values.
 */
public class ConfigurableLootModifier extends LootModifier {
    public static final MapCodec<ConfigurableLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst)
                    .and(Identifier.CODEC.fieldOf("target_table").forGetter(m -> m.targetTable))
                    .apply(inst, ConfigurableLootModifier::new)
    );

    private final Identifier targetTable;

    public ConfigurableLootModifier(LootItemCondition[] conditionsIn, Identifier targetTable) {
        super(conditionsIn);
        this.targetTable = targetTable;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // Check if custom loot tables are enabled
        if (!Config.enableCustomLootTables) {
            return generatedLoot;
        }

        // Get the current loot table ID
        Identifier currentTable = context.getQueriedLootTableId();
        if (!targetTable.equals(currentTable)) {
            return generatedLoot;
        }

        String tablePath = targetTable.getPath();

        // Handle mineshaft loot
        if (tablePath.equals("chests/abandoned_mineshaft")) {
            handleMineshaftLoot(generatedLoot, context);
        }
        // Handle stronghold loot
        else if (isStrongholdTable(targetTable)) {
            handleStrongholdLoot(generatedLoot, context);
        }
        // Handle ancient city loot
        else if (tablePath.equals("chests/ancient_city")) {
            handleAncientCityLoot(generatedLoot, context);
        }
        // Handle end city loot
        else if (tablePath.equals("chests/end_city_treasure")) {
            handleEndCityLoot(generatedLoot, context);
        }
        // Handle simple dungeon loot
        else if (tablePath.equals("chests/simple_dungeon")) {
            handleSimpleDungeonLoot(generatedLoot, context);
        }
        else if (tablePath.equals("chests/woodland_mansion")) {
            handleWoodlandMansionLoot(generatedLoot, context);
        }
        // Handle trial chamber loot
        else if (isTrialChamberTable(targetTable)) {
            handleTrialChamberLoot(generatedLoot, context, tablePath);
        }

        return generatedLoot;
    }

    private void handleMineshaftLoot(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!Config.enableMineshaftLoot) return;

        // Add Resurfacing Potion
        if (Config.enableMineshaftResurfacingPotion) {
            addPotionToLoot(generatedLoot, context, ModPotions.RESURFACING,
                    Config.mineshaftResurfacingWeight,
                    Config.mineshaftResurfacingMinCount,
                    Config.mineshaftResurfacingMaxCount);
        }

        // Add Returning Potion
        if (Config.enableMineshaftReturningPotion) {
            addPotionToLoot(generatedLoot, context, ModPotions.RETURNING,
                    Config.mineshaftReturningWeight,
                    Config.mineshaftReturningMinCount,
                    Config.mineshaftReturningMaxCount);
        }

        // Add Haste Potion
        if (Config.enableMineshaftHastePotion) {
            addPotionToLoot(generatedLoot, context, ModPotions.HASTE,
                    Config.mineshaftHasteWeight,
                    Config.mineshaftHasteMinCount,
                    Config.mineshaftHasteMaxCount);
        }

        // Add Strong Haste Potion
        if (Config.enableMineshaftStrongHastePotion) {
            addPotionToLoot(generatedLoot, context, ModPotions.STRONG_HASTE,
                    Config.mineshaftStrongHasteWeight,
                    Config.mineshaftStrongHasteMinCount,
                    Config.mineshaftStrongHasteMaxCount);
        }
    }

    private void handleStrongholdLoot(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!Config.enableStrongholdLoot) return;

        // Add Resurfacing Potion
        if (Config.enableStrongholdResurfacingPotion) {
            addPotionToLoot(generatedLoot, context, ModPotions.RESURFACING,
                    Config.strongholdResurfacingWeight,
                    Config.strongholdResurfacingMinCount,
                    Config.strongholdResurfacingMaxCount);
        }

        // Add Returning Potion
        if (Config.enableStrongholdReturningPotion) {
            addPotionToLoot(generatedLoot, context, ModPotions.RETURNING,
                    Config.strongholdReturningWeight,
                    Config.strongholdReturningMinCount,
                    Config.strongholdReturningMaxCount);
        }
    }

    private void handleAncientCityLoot(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!Config.enableAncientCityLoot) return;

        // Add Resurfacing Potion
        if (Config.enableAncientCityResurfacingPotion) {
            addPotionToLoot(generatedLoot, context, ModPotions.RESURFACING,
                    Config.ancientCityResurfacingWeight,
                    Config.ancientCityResurfacingMinCount,
                    Config.ancientCityResurfacingMaxCount);
        }

        // Add Returning Potion
        if (Config.enableAncientCityReturningPotion) {
            addPotionToLoot(generatedLoot, context, ModPotions.RETURNING,
                    Config.ancientCityReturningWeight,
                    Config.ancientCityReturningMinCount,
                    Config.ancientCityReturningMaxCount);
        }

        if (Config.enableDogMusicDisc) {
            addItemToLoot(generatedLoot, context, ModDogMusicDisc.musicDiscDog(), ModDogMusicDisc.ANCIENT_CITY_LOOT_CHANCE);
        }
    }

    private void handleEndCityLoot(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!Config.enableEndCityLoot) return;

        // Rare Aerodynamic book
        // (Hardcoded on purpose; can be made configurable later if desired.)
        // 2% chance (1 in 50) per End City Treasure chest roll.
        if (context.getRandom().nextDouble() < 0.02D) {
            var enchantmentLookup = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var aerodynamic = enchantmentLookup.get(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "aerodynamic"));
            aerodynamic.ifPresent(holder -> {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(
                        book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)
                );
                stored.set(holder, 1);
                book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
                generatedLoot.add(book);
            });
        }

        // Add Returning Potion
        if (Config.enableEndCityReturningPotion) {
            addPotionToLoot(generatedLoot, context, ModPotions.RETURNING,
                    Config.endCityReturningWeight,
                    Config.endCityReturningMinCount,
                    Config.endCityReturningMaxCount);
        }
    }

    private void handleSimpleDungeonLoot(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!Config.enableSimpleDungeonLoot) return;

        // Add Resurfacing Potion
        if (Config.enableSimpleDungeonResurfacingPotion) {
            addPotionToLoot(generatedLoot, context, ModPotions.RESURFACING,
                    Config.simpleDungeonResurfacingWeight,
                    Config.simpleDungeonResurfacingMinCount,
                    Config.simpleDungeonResurfacingMaxCount);
        }

        // Add Returning Potion
        if (Config.enableSimpleDungeonReturningPotion) {
            addPotionToLoot(generatedLoot, context, ModPotions.RETURNING,
                    Config.simpleDungeonReturningWeight,
                    Config.simpleDungeonReturningMinCount,
                    Config.simpleDungeonReturningMaxCount);
        }

        if (Config.enableDogMusicDisc) {
            addItemToLoot(generatedLoot, context, ModDogMusicDisc.musicDiscDog(), ModDogMusicDisc.SIMPLE_DUNGEON_LOOT_CHANCE);
        }
    }

    private void handleWoodlandMansionLoot(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (Config.enableDogMusicDisc) {
            addItemToLoot(generatedLoot, context, ModDogMusicDisc.musicDiscDog(), ModDogMusicDisc.WOODLAND_MANSION_LOOT_CHANCE);
        }
    }

    private void handleTrialChamberLoot(ObjectArrayList<ItemStack> generatedLoot, LootContext context, String tablePath) {
        if (!Config.enableTrialChamberLoot) return;

        if (tablePath.equals("chests/trial_chambers/reward_common")) {
            // Add Resurfacing Potion to common rewards
            if (Config.enableTrialCommonResurfacingPotion) {
                addPotionToLoot(generatedLoot, context, ModPotions.RESURFACING,
                        Config.trialCommonResurfacingWeight,
                        Config.trialCommonResurfacingMinCount,
                        Config.trialCommonResurfacingMaxCount);
            }
        } else if (tablePath.equals("chests/trial_chambers/reward_rare")) {
            // Add Resurfacing Potion to rare rewards
            if (Config.enableTrialRareResurfacingPotion) {
                addPotionToLoot(generatedLoot, context, ModPotions.RESURFACING,
                        Config.trialRareResurfacingWeight,
                        Config.trialRareResurfacingMinCount,
                        Config.trialRareResurfacingMaxCount);
            }
            // Add Returning Potion to rare rewards
            if (Config.enableTrialRareReturningPotion) {
                addPotionToLoot(generatedLoot, context, ModPotions.RETURNING,
                        Config.trialRareReturningWeight,
                        Config.trialRareReturningMinCount,
                        Config.trialRareReturningMaxCount);
            }
        } else if (tablePath.equals("chests/trial_chambers/reward_unique")) {
            // Add Returning Potion to unique rewards
            if (Config.enableTrialUniqueReturningPotion) {
                addPotionToLoot(generatedLoot, context, ModPotions.RETURNING,
                        Config.trialUniqueReturningWeight,
                        Config.trialUniqueReturningMinCount,
                        Config.trialUniqueReturningMaxCount);
            }
        }
    }

    private void addPotionToLoot(ObjectArrayList<ItemStack> generatedLoot, LootContext context, 
                                 net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.alchemy.Potion, net.minecraft.world.item.alchemy.Potion> potionHolder,
                                 int weight, int minCount, int maxCount) {
        // Calculate chance based on weight (convert weight to probability)
        // Using a simple formula: chance = weight / (weight + 100)
        // This gives reasonable probabilities: weight 1 = ~1%, weight 10 = ~9%, weight 50 = ~33%
        double chance = (double) weight / (weight + 100.0);
        
        if (context.getRandom().nextDouble() < chance) {
            // Determine count
            int count = minCount;
            if (maxCount > minCount) {
                count = context.getRandom().nextInt(maxCount - minCount + 1) + minCount;
            }
            
            // Create the potion
            ItemStack potionStack = PotionContents.createItemStack(Items.POTION, potionHolder);
            potionStack.setCount(count);
            generatedLoot.add(potionStack);
        }
    }

    private void addItemToLoot(ObjectArrayList<ItemStack> generatedLoot, LootContext context, Item item, float chance) {
        if (item == null || item == Items.AIR) {
            return;
        }
        if (context.getRandom().nextFloat() < chance) {
            generatedLoot.add(new ItemStack(item));
        }
    }

    private boolean isStrongholdTable(Identifier table) {
        String path = table.getPath();
        return path.equals("chests/stronghold_corridor") ||
               path.equals("chests/stronghold_crossing");
        // Note: Stronghold libraries are excluded from loot injection
    }

    private boolean isTrialChamberTable(Identifier table) {
        String path = table.getPath();
        return path.equals("chests/trial_chambers/reward_common") ||
               path.equals("chests/trial_chambers/reward_rare") ||
               path.equals("chests/trial_chambers/reward_unique");
    }

    @Override
    public MapCodec<? extends LootModifier> codec() {
        return CODEC;
    }
}