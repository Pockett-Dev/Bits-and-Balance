package org.onenonly.bitsandbalance.fabric.potions;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fabric-side custom brewing rules.
 *
 * This is the minimal subset needed for Bits and Balance potions to be brewable on Fabric.
 */
public final class FabricCustomBrewing {
    public record Rule(ResourceLocation inputPotion, ResourceLocation reagentItem, ResourceLocation resultPotion) {
    }

    private static volatile List<Rule> RULES = List.of();

    private FabricCustomBrewing() {
    }

    public static void init() {
        // Seed defaults immediately so brewing works even before first reload.
        loadDefaults();

        ResourceManagerHelper.get(PackType.SERVER_DATA)
            .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                @Override
                public ResourceLocation getFabricId() {
                    return ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "custom_brewing");
                }

                @Override
                public void onResourceManagerReload(ResourceManager manager) {
                    if (!FabricPotionConfig.enableCustomBrewing) {
                        RULES = List.of();
                        return;
                    }

                    List<Rule> list = loadFromDatapacks(manager);
                    if (list.isEmpty()) {
                        loadDefaults();
                    } else {
                        RULES = List.copyOf(list);
                    }
                }
            });
    }

    public static List<Rule> rules() {
        return RULES;
    }

    public static void loadDefaults() {
        if (!FabricPotionConfig.enableCustomBrewing) {
            RULES = List.of();
            return;
        }

        List<Rule> list = new ArrayList<>();
        String modId = BitsAndBalanceCommon.MOD_ID;

        if (FabricPotionConfig.enableWitheringPotion) {
            // Baseline Withering from Poison/Harming potions + Wither Rose
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "poison"), ResourceLocation.fromNamespaceAndPath("minecraft", "wither_rose"), ResourceLocation.fromNamespaceAndPath(modId, "withering")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "long_poison"), ResourceLocation.fromNamespaceAndPath("minecraft", "wither_rose"), ResourceLocation.fromNamespaceAndPath(modId, "withering")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "harming"), ResourceLocation.fromNamespaceAndPath("minecraft", "wither_rose"), ResourceLocation.fromNamespaceAndPath(modId, "withering")));
            // Strong variants produce strong withering directly
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "strong_poison"), ResourceLocation.fromNamespaceAndPath("minecraft", "wither_rose"), ResourceLocation.fromNamespaceAndPath(modId, "strong_withering")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "strong_harming"), ResourceLocation.fromNamespaceAndPath("minecraft", "wither_rose"), ResourceLocation.fromNamespaceAndPath(modId, "strong_withering")));
            // Extend Withering with Redstone
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "withering"), ResourceLocation.fromNamespaceAndPath("minecraft", "redstone"), ResourceLocation.fromNamespaceAndPath(modId, "long_withering")));
            // Upgrade to Strong Withering with Glowstone (from regular withering)
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "withering"), ResourceLocation.fromNamespaceAndPath("minecraft", "glowstone_dust"), ResourceLocation.fromNamespaceAndPath(modId, "strong_withering")));
        }

        if (FabricPotionConfig.enableLevitationPotion) {
            // Levitation from Slow Falling + Shulker Shell
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "slow_falling"), ResourceLocation.fromNamespaceAndPath("minecraft", "shulker_shell"), ResourceLocation.fromNamespaceAndPath(modId, "levitation")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "long_slow_falling"), ResourceLocation.fromNamespaceAndPath("minecraft", "shulker_shell"), ResourceLocation.fromNamespaceAndPath(modId, "levitation")));
            // Extend Levitation with Redstone
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "levitation"), ResourceLocation.fromNamespaceAndPath("minecraft", "redstone"), ResourceLocation.fromNamespaceAndPath(modId, "long_levitation")));
            // Strengthen Levitation with Glowstone
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "levitation"), ResourceLocation.fromNamespaceAndPath("minecraft", "glowstone_dust"), ResourceLocation.fromNamespaceAndPath(modId, "strong_levitation")));
        }

        if (FabricPotionConfig.enableHastePotion) {
            // Haste from Speed + Quartz
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "swiftness"), ResourceLocation.fromNamespaceAndPath("minecraft", "quartz"), ResourceLocation.fromNamespaceAndPath(modId, "haste")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "long_swiftness"), ResourceLocation.fromNamespaceAndPath("minecraft", "quartz"), ResourceLocation.fromNamespaceAndPath(modId, "haste")));
            // Strong variants produce strong haste directly
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "strong_swiftness"), ResourceLocation.fromNamespaceAndPath("minecraft", "quartz"), ResourceLocation.fromNamespaceAndPath(modId, "strong_haste")));
            // Extend Haste with Redstone
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "haste"), ResourceLocation.fromNamespaceAndPath("minecraft", "redstone"), ResourceLocation.fromNamespaceAndPath(modId, "long_haste")));
            // Upgrade to Strong Haste with Glowstone (from regular haste)
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "haste"), ResourceLocation.fromNamespaceAndPath("minecraft", "glowstone_dust"), ResourceLocation.fromNamespaceAndPath(modId, "strong_haste")));
        }

        if (FabricPotionConfig.enableBlindnessPotion) {
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "awkward"), ResourceLocation.fromNamespaceAndPath("minecraft", "ink_sac"), ResourceLocation.fromNamespaceAndPath(modId, "blindness")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "blindness"), ResourceLocation.fromNamespaceAndPath("minecraft", "redstone"), ResourceLocation.fromNamespaceAndPath(modId, "long_blindness")));
        }

        if (FabricPotionConfig.enableGlowingPotion) {
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "awkward"), ResourceLocation.fromNamespaceAndPath("minecraft", "glow_berries"), ResourceLocation.fromNamespaceAndPath(modId, "glowing")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "glowing"), ResourceLocation.fromNamespaceAndPath("minecraft", "redstone"), ResourceLocation.fromNamespaceAndPath(modId, "long_glowing")));
        }

        if (FabricPotionConfig.enableGlowingPotion && FabricPotionConfig.enableBioluminescencePotion) {
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "glowing"), ResourceLocation.fromNamespaceAndPath(modId, "glow_goo"), ResourceLocation.fromNamespaceAndPath(modId, "bioluminescence")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "bioluminescence"), ResourceLocation.fromNamespaceAndPath("minecraft", "glowstone_dust"), ResourceLocation.fromNamespaceAndPath(modId, "strong_bioluminescence")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "strong_bioluminescence"), ResourceLocation.fromNamespaceAndPath("minecraft", "redstone"), ResourceLocation.fromNamespaceAndPath(modId, "long_strong_bioluminescence")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "long_glowing"), ResourceLocation.fromNamespaceAndPath(modId, "glow_goo"), ResourceLocation.fromNamespaceAndPath(modId, "long_bioluminescence")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "long_bioluminescence"), ResourceLocation.fromNamespaceAndPath("minecraft", "glowstone_dust"), ResourceLocation.fromNamespaceAndPath(modId, "long_strong_bioluminescence")));
        }

        if (FabricPotionConfig.enableDisplacementPotion) {
            // Displacement from Awkward Potion + Ender Eye
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "awkward"), ResourceLocation.fromNamespaceAndPath("minecraft", "ender_eye"), ResourceLocation.fromNamespaceAndPath(modId, "displacement")));
        }

        if (FabricPotionConfig.enableReturningPotion) {
            // Returning from Displacement Potion + Echo Shard
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(modId, "displacement"), ResourceLocation.fromNamespaceAndPath("minecraft", "echo_shard"), ResourceLocation.fromNamespaceAndPath(modId, "returning")));
        }

        RULES = List.copyOf(list);
    }

    private static List<Rule> loadFromDatapacks(ResourceManager resourceManager) {
        Map<ResourceLocation, Rule> loaded = new LinkedHashMap<>();

        try {
            Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "custom_brewing",
                p -> p.getPath().endsWith(".json")
            );

            for (var entry : resources.entrySet()) {
                ResourceLocation resLoc = entry.getKey();
                Resource resource = entry.getValue();

                try (var in = resource.open();
                     var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    String json = sb.toString().trim();

                    Rule rule = parseRule(json);
                    if (rule != null) {
                        loaded.put(resLoc, rule);
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }

        return new ArrayList<>(loaded.values());
    }

    private static Rule parseRule(String json) {
        try {
            String inPot = extract(json, "\"input\"", "\"potion\"");
            String reagent = extract(json, "\"reagent\"", "\"item\"");
            String outPot = extract(json, "\"result\"", "\"potion\"");
            if (inPot == null || reagent == null || outPot == null) return null;
            return new Rule(ResourceLocation.parse(inPot), ResourceLocation.parse(reagent), ResourceLocation.parse(outPot));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String extract(String json, String parentKey, String childKey) {
        int pIdx = json.indexOf(parentKey);
        if (pIdx < 0) return null;
        int cIdx = json.indexOf(childKey, pIdx);
        if (cIdx < 0) return null;
        int q1 = json.indexOf('"', cIdx + childKey.length());
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        String val = json.substring(q1 + 1, q2);
        return val.contains(":") ? val : null;
    }

    public static Rule findMatch(Holder<Potion> basePotion, Item reagentItem) {
        ResourceLocation baseId = BuiltInRegistries.POTION.getKey(basePotion.value());
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(reagentItem);
        if (baseId == null || itemId == null) return null;
        for (Rule r : RULES) {
            if (r.inputPotion.equals(baseId) && r.reagentItem.equals(itemId)) return r;
        }
        return null;
    }

    public static Holder<Potion> resultHolder(Rule rule) {
        Potion potion = BuiltInRegistries.POTION.get(rule.resultPotion);
        return potion != null ? BuiltInRegistries.POTION.wrapAsHolder(potion) : null;
    }
}
