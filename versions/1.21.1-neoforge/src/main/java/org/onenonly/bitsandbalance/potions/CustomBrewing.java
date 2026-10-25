package org.onenonly.bitsandbalance.potions;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
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

    public record Rule(ResourceLocation inputPotion, ResourceLocation reagentItem, ResourceLocation resultPotion) {}

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
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "poison"), ResourceLocation.fromNamespaceAndPath("minecraft", "wither_rose"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "withering")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "long_poison"), ResourceLocation.fromNamespaceAndPath("minecraft", "wither_rose"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "withering")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "harming"), ResourceLocation.fromNamespaceAndPath("minecraft", "wither_rose"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "withering")));
            // Strong variants produce strong withering directly
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "strong_poison"), ResourceLocation.fromNamespaceAndPath("minecraft", "wither_rose"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_withering")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "strong_harming"), ResourceLocation.fromNamespaceAndPath("minecraft", "wither_rose"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_withering")));
            // Extend Withering with Redstone
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "withering"), ResourceLocation.fromNamespaceAndPath("minecraft", "redstone"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "long_withering")));
            // Upgrade to Strong Withering with Glowstone (from regular withering)
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "withering"), ResourceLocation.fromNamespaceAndPath("minecraft", "glowstone_dust"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_withering")));
        }
        
        if (Config.enableLevitationPotion) {
            // Levitation from Slow Falling + Shulker Shell
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "slow_falling"), ResourceLocation.fromNamespaceAndPath("minecraft", "shulker_shell"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "levitation")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "long_slow_falling"), ResourceLocation.fromNamespaceAndPath("minecraft", "shulker_shell"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "levitation")));
            // Extend Levitation with Redstone
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "levitation"), ResourceLocation.fromNamespaceAndPath("minecraft", "redstone"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "long_levitation")));
            // Strengthen Levitation with Glowstone
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "levitation"), ResourceLocation.fromNamespaceAndPath("minecraft", "glowstone_dust"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_levitation")));
        }
        
        if (Config.enableHastePotion) {
            // Haste from Speed + Quartz
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "swiftness"), ResourceLocation.fromNamespaceAndPath("minecraft", "quartz"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "haste")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "long_swiftness"), ResourceLocation.fromNamespaceAndPath("minecraft", "quartz"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "haste")));
            // Strong variants produce strong haste directly
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "strong_swiftness"), ResourceLocation.fromNamespaceAndPath("minecraft", "quartz"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_haste")));
            // Extend Haste with Redstone
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "haste"), ResourceLocation.fromNamespaceAndPath("minecraft", "redstone"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "long_haste")));
            // Upgrade to Strong Haste with Glowstone (from regular haste)
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "haste"), ResourceLocation.fromNamespaceAndPath("minecraft", "glowstone_dust"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_haste")));
        }

        if (Config.enableBlindnessPotion) {
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "awkward"), ResourceLocation.fromNamespaceAndPath("minecraft", "ink_sac"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "blindness")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "blindness"), ResourceLocation.fromNamespaceAndPath("minecraft", "redstone"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "long_blindness")));
        }

        if (Config.enableGlowingPotion) {
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "awkward"), ResourceLocation.fromNamespaceAndPath("minecraft", "glow_berries"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "glowing")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "glowing"), ResourceLocation.fromNamespaceAndPath("minecraft", "redstone"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "long_glowing")));
        }

        if (Config.enableGlowingPotion && Config.enableBioluminescencePotion) {
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "glowing"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "glow_goo"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "bioluminescence")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "bioluminescence"), ResourceLocation.fromNamespaceAndPath("minecraft", "glowstone_dust"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_bioluminescence")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "strong_bioluminescence"), ResourceLocation.fromNamespaceAndPath("minecraft", "redstone"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "long_strong_bioluminescence")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "long_glowing"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "glow_goo"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "long_bioluminescence")));
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "long_bioluminescence"), ResourceLocation.fromNamespaceAndPath("minecraft", "glowstone_dust"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "long_strong_bioluminescence")));
        }
        
        if (Config.enableDisplacementPotion) {
            // Displacement from Awkward Potion + Ender Eye
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath("minecraft", "awkward"), ResourceLocation.fromNamespaceAndPath("minecraft", "ender_eye"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "displacement")));
        }
        
        if (Config.enableReturningPotion) {
            // Returning from Displacement Potion + Echo Shard
            list.add(new Rule(ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "displacement"), ResourceLocation.fromNamespaceAndPath("minecraft", "echo_shard"), ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "returning")));
        }
        
        setRules(list);
        LOG.debug("[{}][brew] Loaded {} default custom brewing rule(s)", BitsAndBalance.MODID, list.size());
    }

    /** Find a matching rule for the given base potion and reagent item. */
    public static @Nullable Rule findMatch(Holder<Potion> basePotion, Item reagentItem) {
        ResourceLocation baseId = BuiltInRegistries.POTION.getKey(basePotion.value());
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(reagentItem);
        if (baseId == null || itemId == null) return null;
        for (Rule r : RULES) {
            if (r.inputPotion.equals(baseId) && r.reagentItem.equals(itemId)) return r;
        }
        return null;
    }

    /** Resolve a rule's result potion Holder. Returns null if missing. */
    public static @Nullable Holder<Potion> resultHolder(Rule rule) {
        Potion potion = BuiltInRegistries.POTION.get(rule.resultPotion);
        return potion != null ? BuiltInRegistries.POTION.wrapAsHolder(potion) : null;
    }

    /** Resolve convenience holders for items/potions. */
    public static @Nullable Holder<Potion> potionHolder(ResourceLocation id) {
        Potion potion = BuiltInRegistries.POTION.get(id);
        return potion != null ? BuiltInRegistries.POTION.wrapAsHolder(potion) : null;
    }

    /** Datapack reload listener that loads JSON rule files from data/<ns>/custom_brewing/*.json. */
    public static class ReloadListener implements PreparableReloadListener {
        @Override
        public CompletableFuture<Void> reload(
            @Nonnull PreparationBarrier barrier,
            @Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller preparationsProfiler,
            @Nonnull ProfilerFiller reloadProfiler,
            @Nonnull Executor backgroundExecutor,
            @Nonnull Executor gameExecutor
        ) {
            return CompletableFuture.supplyAsync(() -> {
                Map<ResourceLocation, Rule> loaded = new LinkedHashMap<>();
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

        private @Nullable Rule parseRule(String json, ResourceLocation resLoc) {
            try {
                // Minimal hand-rolled parse for very small JSON format to avoid bringing a JSON library.
                // Expected keys: input.potion, reagent.item, result.potion
                String inPot = extract(json, "\"input\"", "\"potion\"");
                String reagent = extract(json, "\"reagent\"", "\"item\"");
                String outPot = extract(json, "\"result\"", "\"potion\"");
                if (inPot == null || reagent == null || outPot == null) return null;
                return new Rule(ResourceLocation.parse(inPot), ResourceLocation.parse(reagent), ResourceLocation.parse(outPot));
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
