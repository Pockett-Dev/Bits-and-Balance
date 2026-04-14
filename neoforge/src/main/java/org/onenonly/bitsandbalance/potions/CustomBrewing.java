package org.onenonly.bitsandbalance.potions;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.jetbrains.annotations.Nullable;
import javax.annotation.Nonnull;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.onenonly.bitsandbalance.Config;
/**
 * Custom brewing rules manager. Provides a small, data-reloadable registry of (input potion, reagent item) -> result potion.
 * Rules can be defined via datapack JSON files under data/<namespace>/custom_brewing/*.json and are also seeded by defaults in code.
 */
public final class CustomBrewing {
    private static final Logger LOG = LogUtils.getLogger();

    public record Rule(Identifier inputPotion, Identifier reagentItem, Identifier resultPotion) {}

    private static volatile List<Rule> RULES = List.of();

    private CustomBrewing() {}

    public static List<Rule> rules() {
        return RULES;
    }

    public static void setRules(List<Rule> newRules) {
        RULES = List.copyOf(newRules);
    }

    public static void loadDefaults() {
        List<Rule> list = new ArrayList<>();
        if (Config.enableWitheringPotion) {
            // Baseline Withering from Poison/Harming potions + Wither Rose
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "poison"), Identifier.fromNamespaceAndPath("minecraft", "wither_rose"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "withering")));
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "long_poison"), Identifier.fromNamespaceAndPath("minecraft", "wither_rose"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "withering")));
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "harming"), Identifier.fromNamespaceAndPath("minecraft", "wither_rose"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "withering")));
            // Strong variants produce strong withering directly
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "strong_poison"), Identifier.fromNamespaceAndPath("minecraft", "wither_rose"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_withering")));
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "strong_harming"), Identifier.fromNamespaceAndPath("minecraft", "wither_rose"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_withering")));
            // Extend Withering with Redstone
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "withering"), Identifier.fromNamespaceAndPath("minecraft", "redstone"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "long_withering")));
            // Upgrade to Strong Withering with Glowstone (from regular withering)
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "withering"), Identifier.fromNamespaceAndPath("minecraft", "glowstone_dust"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_withering")));
        }
        
        if (Config.enableLevitationPotion) {
            // Levitation from Slow Falling + Shulker Shell
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "slow_falling"), Identifier.fromNamespaceAndPath("minecraft", "shulker_shell"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "levitation")));
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "long_slow_falling"), Identifier.fromNamespaceAndPath("minecraft", "shulker_shell"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "levitation")));
            // Extend Levitation with Redstone
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "levitation"), Identifier.fromNamespaceAndPath("minecraft", "redstone"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "long_levitation")));
            // Strengthen Levitation with Glowstone
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "levitation"), Identifier.fromNamespaceAndPath("minecraft", "glowstone_dust"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_levitation")));
        }
        
        if (Config.enableHastePotion) {
            // Haste from Speed + Quartz
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "swiftness"), Identifier.fromNamespaceAndPath("minecraft", "quartz"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "haste")));
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "long_swiftness"), Identifier.fromNamespaceAndPath("minecraft", "quartz"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "haste")));
            // Strong variants produce strong haste directly
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "strong_swiftness"), Identifier.fromNamespaceAndPath("minecraft", "quartz"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_haste")));
            // Extend Haste with Redstone
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "haste"), Identifier.fromNamespaceAndPath("minecraft", "redstone"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "long_haste")));
            // Upgrade to Strong Haste with Glowstone (from regular haste)
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "haste"), Identifier.fromNamespaceAndPath("minecraft", "glowstone_dust"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_haste")));
        }

        if (Config.enableGlowingPotion) {
            // Glowing from Awkward + Glow Berries
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "awkward"), Identifier.fromNamespaceAndPath("minecraft", "glow_berries"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "glowing")));
            // Extend Glowing with Redstone
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "glowing"), Identifier.fromNamespaceAndPath("minecraft", "redstone"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "long_glowing")));
        }

        if (Config.enableGlowingPotion && Config.enableBioluminescencePotion) {
            // Bioluminescence from Glowing + Glow Goo
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "glowing"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "glow_goo"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "bioluminescence")));
            // Strong Bioluminescence from Bioluminescence + Glowstone
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "bioluminescence"), Identifier.fromNamespaceAndPath("minecraft", "glowstone_dust"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_bioluminescence")));
                // Extended Strong Bioluminescence from Strong Bioluminescence + Redstone
                list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_bioluminescence"), Identifier.fromNamespaceAndPath("minecraft", "redstone"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "long_strong_bioluminescence")));
            // Long Bioluminescence from Long Glowing + Glow Goo
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "long_glowing"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "glow_goo"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "long_bioluminescence")));
                // Extended Strong Bioluminescence from Long Bioluminescence + Glowstone
                list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "long_bioluminescence"), Identifier.fromNamespaceAndPath("minecraft", "glowstone_dust"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "long_strong_bioluminescence")));
        }
        
        if (Config.enableDisplacementPotion) {
            // Displacement from Awkward Potion + Ender Eye
            list.add(new Rule(Identifier.fromNamespaceAndPath("minecraft", "awkward"), Identifier.fromNamespaceAndPath("minecraft", "ender_eye"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "displacement")));
        }
        
        if (Config.enableReturningPotion) {
            // Returning from Displacement Potion + Echo Shard
            list.add(new Rule(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "displacement"), Identifier.fromNamespaceAndPath("minecraft", "echo_shard"), Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "returning")));
        }
        
        setRules(list);
        LOG.debug("[{}][brew] Loaded {} default custom brewing rule(s)", BitsAndBalance.MODID, list.size());
    }

    /** Find a matching rule for the given base potion and reagent item. */
    public static @Nullable Rule findMatch(Holder<Potion> basePotion, Item reagentItem) {
        Identifier baseId = BuiltInRegistries.POTION.getKey(basePotion.value());
        Identifier itemId = BuiltInRegistries.ITEM.getKey(reagentItem);
        if (baseId == null || itemId == null) return null;
        for (Rule r : RULES) {
            if (r.inputPotion.equals(baseId) && r.reagentItem.equals(itemId)) return r;
        }
        return null;
    }

    /** Resolve a rule's result potion Holder. Returns null if missing. */
    public static @Nullable Holder<Potion> resultHolder(Rule rule) {
        return BuiltInRegistries.POTION.get(rule.resultPotion).orElse(null);
    }

    /** Resolve convenience holders for items/potions. */
    public static @Nullable Holder<Potion> potionHolder(Identifier id) {
        return BuiltInRegistries.POTION.get(id).orElse(null);
    }

    /** Datapack reload listener that loads JSON rule files from data/<ns>/custom_brewing/*.json. */
    public static class ReloadListener implements PreparableReloadListener {
        @Override
        public CompletableFuture<Void> reload(
            @Nonnull SharedState sharedState,
            @Nonnull Executor backgroundExecutor,
            @Nonnull PreparationBarrier barrier,
            @Nonnull Executor gameExecutor
        ) {
            ResourceManager resourceManager = sharedState.resourceManager();
            return CompletableFuture.supplyAsync(() -> {
                Map<Identifier, Rule> loaded = new LinkedHashMap<>();
                try {
                    // Iterate all namespaces to support overrides under other packs
                    for (String ignored : resourceManager.getNamespaces()) {
                        // Iterate through all namespaces to support overrides under other packs
                        // Collect resources whose path starts with custom_brewing/ and ends with .json
                        var resources = resourceManager.listResources("custom_brewing", p -> p.getPath().endsWith(".json"));
                        for (var entry : resources.entrySet()) {
                            var resLoc = entry.getKey();
                            try {
                                var opt = resourceManager.getResource(resLoc);
                                if (opt.isEmpty()) continue;
                                try (var in = opt.get().open();
                                     var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                                    StringBuilder sb = new StringBuilder();
                                    String line;
                                    while ((line = reader.readLine()) != null) sb.append(line);
                                    var json = sb.toString().trim();
                                    Rule rule = parseRule(json, resLoc);
                                    if (rule != null) loaded.put(resLoc, rule);
                                }
                            } catch (Throwable e) {
                                LOG.debug("[{}][brew] Failed loading custom_brewing {}: {}", BitsAndBalance.MODID, resLoc, e.toString());
                            }
                        }
                    }
                } catch (Throwable t) {
                    LOG.debug("[{}][brew] custom_brewing scan failed: {}", BitsAndBalance.MODID, t.toString());
                }
                return new ArrayList<>(loaded.values());
            }, backgroundExecutor).thenCompose(barrier::wait).thenAcceptAsync(list -> {
                if (list == null || list.isEmpty()) {
                    loadDefaults();
                } else {
                    setRules(list);
                    LOG.info("[{}][brew] Loaded {} custom brewing rule(s) from datapacks", BitsAndBalance.MODID, list.size());
                }
            }, gameExecutor);
        }

        private @Nullable Rule parseRule(String json, Identifier resLoc) {
            try {
                // Minimal hand-rolled parse for very small JSON format to avoid bringing a JSON library.
                // Expected keys: input.potion, reagent.item, result.potion
                String inPot = extract(json, "\"input\"", "\"potion\"");
                String reagent = extract(json, "\"reagent\"", "\"item\"");
                String outPot = extract(json, "\"result\"", "\"potion\"");
                if (inPot == null || reagent == null || outPot == null) return null;
                return new Rule(Identifier.parse(inPot), Identifier.parse(reagent), Identifier.parse(outPot));
            } catch (Throwable e) {
                LOG.debug("[{}][brew] Could not parse rule {}: {}", BitsAndBalance.MODID, resLoc, e.toString());
                return null;
            }
        }

        private @Nullable String extract(String json, String parentKey, String childKey) {
            int pIdx = json.indexOf(parentKey);
            if (pIdx < 0) return null;
            int cIdx = json.indexOf(childKey, pIdx);
            if (cIdx < 0) return null;
            int q1 = json.indexOf('"', cIdx + childKey.length());
            if (q1 < 0) return null;
            int q2 = json.indexOf('"', q1 + 1);
            if (q2 < 0) return null;
            String val = json.substring(q1 + 1, q2);
            // If value looks like key:value pair e.g., "potion":"namespace:id", handle already
            if (val.contains(":")) return val;
            return null;
        }
    }
}
