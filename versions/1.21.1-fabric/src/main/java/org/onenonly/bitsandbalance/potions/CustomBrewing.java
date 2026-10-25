package org.onenonly.bitsandbalance.potions;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CustomBrewing {
    public record Rule(ResourceLocation inputPotion, ResourceLocation reagentItem, ResourceLocation resultPotion) {
    }

    private static volatile List<Rule> rules = List.of();

    private CustomBrewing() {
    }

    public static List<Rule> rules() {
        return rules;
    }

    public static void setRules(List<Rule> newRules) {
        rules = List.copyOf(newRules);
    }

    public static void loadDefaults() {
        setRules(List.of());
    }

    public static @Nullable Rule findMatch(Holder<Potion> basePotion, Item reagentItem) {
        ResourceLocation baseId = BuiltInRegistries.POTION.getKey(basePotion.value());
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(reagentItem);
        if (baseId == null || itemId == null) {
            return null;
        }

        for (Rule rule : rules) {
            if (rule.inputPotion.equals(baseId) && rule.reagentItem.equals(itemId)) {
                return rule;
            }
        }
        return null;
    }

    public static @Nullable Holder<Potion> resultHolder(Rule rule) {
        Potion potion = BuiltInRegistries.POTION.get(rule.resultPotion);
        return potion != null ? BuiltInRegistries.POTION.wrapAsHolder(potion) : null;
    }

    public static @Nullable Holder<Potion> potionHolder(ResourceLocation id) {
        Potion potion = BuiltInRegistries.POTION.get(id);
        return potion != null ? BuiltInRegistries.POTION.wrapAsHolder(potion) : null;
    }
}