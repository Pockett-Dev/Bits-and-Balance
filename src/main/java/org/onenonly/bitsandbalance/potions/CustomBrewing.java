package org.onenonly.bitsandbalance.potions;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.potions.PotionFeatureFlags;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Custom brewing rules manager. Provides a small, data-reloadable registry of (input potion, reagent item) -> result potion.
 * Rules can be defined via datapack JSON files under data/<namespace>/custom_brewing/*.json and are also seeded by defaults in code.
 */
public final class CustomBrewing {
    private static final Logger LOG = LogUtils.getLogger();

    public record Rule(Identifier inputPotion, Identifier reagentItem, Identifier resultPotion) {}

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
        if (!PotionFeatureFlags.isCustomBrewingEnabled()) {
            setRules(List.of());
            LOG.debug("[{}][brew] Custom brewing disabled; loaded 0 default custom brewing rule(s)", BitsAndBalanceCommon.MOD_ID);
            return;
        }

        List<Rule> list = new ArrayList<>();
        if (PotionFeatureFlags.isWitheringPotionEnabled()) {
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "poison"), Identifier.fromNamespaceAndPath("minecraft", "wither_rose"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "withering")));
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "long_poison"), Identifier.fromNamespaceAndPath("minecraft", "wither_rose"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "withering")));
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "harming"), Identifier.fromNamespaceAndPath("minecraft", "wither_rose"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "withering")));
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "strong_poison"), Identifier.fromNamespaceAndPath("minecraft", "wither_rose"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "strong_withering")));
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "strong_harming"), Identifier.fromNamespaceAndPath("minecraft", "wither_rose"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "strong_withering")));
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "withering"), Identifier.fromNamespaceAndPath("minecraft", "redstone"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "long_withering")));
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "withering"), Identifier.fromNamespaceAndPath("minecraft", "glowstone_dust"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "strong_withering")));
        }

        if (PotionFeatureFlags.isLevitationPotionEnabled()) {
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "slow_falling"), Identifier.fromNamespaceAndPath("minecraft", "shulker_shell"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "levitation")));
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "long_slow_falling"), Identifier.fromNamespaceAndPath("minecraft", "shulker_shell"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "levitation")));
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "levitation"), Identifier.fromNamespaceAndPath("minecraft", "redstone"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "long_levitation")));
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "levitation"), Identifier.fromNamespaceAndPath("minecraft", "glowstone_dust"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "strong_levitation")));
        }

        if (PotionFeatureFlags.isHastePotionEnabled()) {
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "swiftness"), Identifier.fromNamespaceAndPath("minecraft", "quartz"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "haste")));
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "long_swiftness"), Identifier.fromNamespaceAndPath("minecraft", "quartz"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "haste")));
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "strong_swiftness"), Identifier.fromNamespaceAndPath("minecraft", "quartz"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "strong_haste")));
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "haste"), Identifier.fromNamespaceAndPath("minecraft", "redstone"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "long_haste")));
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "haste"), Identifier.fromNamespaceAndPath("minecraft", "glowstone_dust"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "strong_haste")));
        }

        if (PotionFeatureFlags.isBlindnessPotionEnabled()) {
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "awkward"), Identifier.fromNamespaceAndPath("minecraft", "ink_sac"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "blindness")));
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "blindness"), Identifier.fromNamespaceAndPath("minecraft", "redstone"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "long_blindness")));
        }

        if (PotionFeatureFlags.isGlowingPotionEnabled()) {
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "awkward"), Identifier.fromNamespaceAndPath("minecraft", "glow_berries"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "glowing")));
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "glowing"), Identifier.fromNamespaceAndPath("minecraft", "redstone"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "long_glowing")));
        }

        if (PotionFeatureFlags.isGlowingPotionEnabled() && PotionFeatureFlags.isBioluminescencePotionEnabled()) {
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "glowing"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "glow_goo"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "bioluminescence")));
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "bioluminescence"), Identifier.fromNamespaceAndPath("minecraft", "glowstone_dust"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "strong_bioluminescence")));
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "strong_bioluminescence"), Identifier.fromNamespaceAndPath("minecraft", "redstone"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "long_strong_bioluminescence")));
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "long_glowing"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "glow_goo"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "long_bioluminescence")));
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "long_bioluminescence"), Identifier.fromNamespaceAndPath("minecraft", "glowstone_dust"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "long_strong_bioluminescence")));
        }

        if (PotionFeatureFlags.isDisplacementPotionEnabled()) {
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "awkward"), Identifier.fromNamespaceAndPath("minecraft", "ender_eye"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "displacement")));
        }

        if (PotionFeatureFlags.isReturningPotionEnabled()) {
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "displacement"), Identifier.fromNamespaceAndPath("minecraft", "echo_shard"), Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "returning")));
        }

        setRules(list);
        LOG.debug("[{}][brew] Loaded {} default custom brewing rule(s)", BitsAndBalanceCommon.MOD_ID, list.size());
    }

    public static @Nullable Rule findMatch(Holder<Potion> basePotion, Item reagentItem) {
        Identifier baseId = BuiltInRegistries.POTION.getKey(basePotion.value());
        Identifier itemId = BuiltInRegistries.ITEM.getKey(reagentItem);
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
        return BuiltInRegistries.POTION.get(rule.resultPotion).orElse(null);
    }

    public static @Nullable Holder<Potion> potionHolder(Identifier id) {
        return BuiltInRegistries.POTION.get(id).orElse(null);
    }

    public static class ReloadListener implements PreparableReloadListener {
        @Override
        public CompletableFuture<Void> reload(
            SharedState sharedState,
            Executor backgroundExecutor,
            PreparationBarrier barrier,
            Executor gameExecutor
        ) {
            ResourceManager resourceManager = sharedState.resourceManager();
            return CompletableFuture.supplyAsync(() -> {
                Map<Identifier, Rule> loaded = new LinkedHashMap<>();
                try {
                    for (String ignored : resourceManager.getNamespaces()) {
                        var resources = resourceManager.listResources("custom_brewing", path -> path.getPath().endsWith(".json"));
                        for (var entry : resources.entrySet()) {
                            var resLoc = entry.getKey();
                            try {
                                var opt = resourceManager.getResource(resLoc);
                                if (opt.isEmpty()) {
                                    continue;
                                }
                                try (var in = opt.get().open();
                                     var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                                    StringBuilder sb = new StringBuilder();
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        sb.append(line);
                                    }
                                    Rule rule = parseRule(sb.toString().trim(), resLoc);
                                    if (rule != null) {
                                        loaded.put(resLoc, rule);
                                    }
                                }
                            } catch (Throwable e) {
                                LOG.debug("[{}][brew] Failed loading custom_brewing {}: {}", BitsAndBalanceCommon.MOD_ID, resLoc, e.toString());
                            }
                        }
                    }
                } catch (Throwable t) {
                    LOG.debug("[{}][brew] custom_brewing scan failed: {}", BitsAndBalanceCommon.MOD_ID, t.toString());
                }
                return new ArrayList<>(loaded.values());
            }, backgroundExecutor).thenCompose(barrier::wait).thenAcceptAsync(list -> {
                if (list == null || list.isEmpty()) {
                    loadDefaults();
                } else {
                    setRules(list);
                    LOG.info("[{}][brew] Loaded {} custom brewing rule(s) from datapacks", BitsAndBalanceCommon.MOD_ID, list.size());
                }
            }, gameExecutor);
        }

        private @Nullable Rule parseRule(String json, Identifier resLoc) {
            try {
                String inPot = extract(json, "\"input\"", "\"potion\"");
                String reagent = extract(json, "\"reagent\"", "\"item\"");
                String outPot = extract(json, "\"result\"", "\"potion\"");
                if (inPot == null || reagent == null || outPot == null) {
                    return null;
                }
                return new Rule(Identifier.parse(inPot), Identifier.parse(reagent), Identifier.parse(outPot));
            } catch (Throwable e) {
                LOG.debug("[{}][brew] Could not parse rule {}: {}", BitsAndBalanceCommon.MOD_ID, resLoc, e.toString());
                return null;
            }
        }

        private @Nullable String extract(String json, String parentKey, String childKey) {
            int pIdx = json.indexOf(parentKey);
            if (pIdx < 0) {
                return null;
            }
            int cIdx = json.indexOf(childKey, pIdx);
            if (cIdx < 0) {
                return null;
            }
            int q1 = json.indexOf('"', cIdx + childKey.length());
            if (q1 < 0) {
                return null;
            }
            int q2 = json.indexOf('"', q1 + 1);
            if (q2 < 0) {
                return null;
            }
            String val = json.substring(q1 + 1, q2);
            return val.contains(":") ? val : null;
        }
    }
}