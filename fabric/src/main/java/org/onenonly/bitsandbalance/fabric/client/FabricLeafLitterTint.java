package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FabricLeafLitterTint {
    private FabricLeafLitterTint() {
    }

    private record BiomeTagRange(TagKey<Biome> tag, int[] range) {
    }

    private record ParsedBiomeRanges(Map<Identifier, int[]> byBiome, List<BiomeTagRange> byTag) {
    }

    public static void initClient() {
        if (!FabricClientConfig.leafLitterTintEnabled) {
            return;
        }

        int globalMin = tryParseHexColor(FabricClientConfig.leafLitterTintGlobalMinColor);
        int globalMax = tryParseHexColor(FabricClientConfig.leafLitterTintGlobalMaxColor);
        ParsedBiomeRanges parsed = parseBiomeRanges(FabricClientConfig.leafLitterTintBiomeRanges);

        // Leaf litter should match oak leaves (foliage) tint.
        int defaultColor = FoliageColor.get(0.5D, 1.0D);

        BlockColor blockColor = (state, level, pos, tintIndex) -> {
            int color;
            if (level != null && pos != null) {
                color = BiomeColors.getAverageFoliageColor(level, pos);
            } else {
                color = defaultColor;
            }

            if (!FabricClientConfig.leafLitterTintClampEnabled) {
                return color;
            }

            int min = globalMin;
            int max = globalMax;

            if (level != null && pos != null && level instanceof LevelReader lr) {
                var holder = lr.getBiome(pos);
                boolean matched = false;

                var keyOpt = holder.unwrapKey();
                if (keyOpt.isPresent()) {
                    var biomeId = keyOpt.get().identifier();
                    var range = parsed.byBiome().get(biomeId);
                    if (range != null && range.length >= 2) {
                        min = range[0];
                        max = range[1];
                        matched = true;
                    }
                }

                if (!matched) {
                    for (var tagRange : parsed.byTag()) {
                        if (tagRange == null || tagRange.tag() == null || tagRange.range() == null || tagRange.range().length < 2) {
                            continue;
                        }
                        if (holder.is(tagRange.tag())) {
                            min = tagRange.range()[0];
                            max = tagRange.range()[1];
                            break;
                        }
                    }
                }
            }

            if (min == -1 || max == -1) {
                return color;
            }

            return clampRgb(color, min, max);
        };

        List<Block> blocksToTint = new ArrayList<>();
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            Identifier id = entry.getKey().identifier();
            if (id == null) {
                continue;
            }

            boolean isVanillaLeafLitter = id.getNamespace().equals("minecraft") && id.getPath().equals("leaf_litter");
            boolean isLeafPile = id.getPath().contains("leaf_pile");
            if (!isVanillaLeafLitter && !isLeafPile) {
                continue;
            }

            Block block = entry.getValue();
            blocksToTint.add(block);
        }

        if (!blocksToTint.isEmpty()) {
            ColorProviderRegistry.BLOCK.register(blockColor, blocksToTint.toArray(Block[]::new));
        }
    }

    private static ParsedBiomeRanges parseBiomeRanges(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return new ParsedBiomeRanges(Map.of(), List.of());
        }

        Map<Identifier, int[]> byBiome = new HashMap<>();
        List<BiomeTagRange> byTag = new ArrayList<>();

        for (String raw : entries) {
            if (raw == null) {
                continue;
            }
            String s = raw.trim();
            if (s.isEmpty()) {
                continue;
            }

            int eq = s.indexOf('=');
            if (eq <= 0 || eq >= s.length() - 1) {
                continue;
            }

            String key = s.substring(0, eq).trim();
            String range = s.substring(eq + 1).trim();
            int dash = range.indexOf('-');
            if (dash <= 0 || dash >= range.length() - 1) {
                continue;
            }

            int min = tryParseHexColor(range.substring(0, dash).trim());
            int max = tryParseHexColor(range.substring(dash + 1).trim());
            if (min == -1 || max == -1) {
                continue;
            }

            if (key.startsWith("#")) {
                String tagId = key.substring(1).trim();
                if (tagId.isEmpty()) {
                    continue;
                }
                Identifier tagLoc = tryParseId(tagId);
                if (tagLoc == null) {
                    continue;
                }
                var tag = TagKey.create(Registries.BIOME, tagLoc);
                byTag.add(new BiomeTagRange(tag, new int[]{min, max}));
            } else {
                Identifier biome = tryParseId(key);
                if (biome == null) {
                    continue;
                }
                byBiome.put(biome, new int[]{min, max});
            }
        }

        return new ParsedBiomeRanges(Map.copyOf(byBiome), List.copyOf(byTag));
    }

    private static Identifier tryParseId(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        try {
            return Identifier.parse(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int tryParseHexColor(String raw) {
        if (raw == null) {
            return -1;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return -1;
        }

        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        if (s.length() != 6) {
            return -1;
        }

        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static int lerpRgb(int a, int b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;

        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;

        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);

        return (r << 16) | (g << 8) | bl;
    }

    private static int clampRgb(int color, int min, int max) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int minR = (min >> 16) & 0xFF;
        int minG = (min >> 8) & 0xFF;
        int minB = min & 0xFF;

        int maxR = (max >> 16) & 0xFF;
        int maxG = (max >> 8) & 0xFF;
        int maxB = max & 0xFF;

        int loR = Math.min(minR, maxR);
        int hiR = Math.max(minR, maxR);
        int loG = Math.min(minG, maxG);
        int hiG = Math.max(minG, maxG);
        int loB = Math.min(minB, maxB);
        int hiB = Math.max(minB, maxB);

        r = Math.max(loR, Math.min(hiR, r));
        g = Math.max(loG, Math.min(hiG, g));
        b = Math.max(loB, Math.min(hiB, b));

        return (r << 16) | (g << 8) | b;
    }
}
