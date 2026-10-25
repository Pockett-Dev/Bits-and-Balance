package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

    private record ParsedBiomeRanges(Map<ResourceLocation, int[]> byBiome, List<BiomeTagRange> byTag) {
    }

    public static void initClient() {
        if (!FabricClientConfig.leafLitterTintEnabled) {
            return;
        }

        int globalMin = tryParseHexColor(FabricClientConfig.leafLitterTintGlobalMinColor);
        int globalMax = tryParseHexColor(FabricClientConfig.leafLitterTintGlobalMaxColor);
        ParsedBiomeRanges parsed = parseBiomeRanges(FabricClientConfig.leafLitterTintBiomeRanges);

        BlockColor blockColor = (state, level, pos, tintIndex) -> {
            int color = resolveBaseColor(level, pos);
            LevelReader reader = level instanceof LevelReader lr ? lr : null;

            if (!FabricClientConfig.leafLitterTintClampEnabled) {
                return color;
            }

            int min = globalMin;
            int max = globalMax;

            if (reader != null && pos != null) {
                var holder = reader.getBiome(pos);
                boolean matched = false;
                var keyOpt = holder.unwrapKey();
                if (keyOpt.isPresent()) {
                    ResourceLocation biomeId = keyOpt.get().location();
                    int[] range = parsed.byBiome().get(biomeId);
                    if (range != null && range.length >= 2) {
                        min = range[0];
                        max = range[1];
                        matched = true;
                    }
                }

                if (!matched) {
                    for (BiomeTagRange tagRange : parsed.byTag()) {
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

            if (min != -1 && max != -1) {
                color = clampRgb(color, min, max);
            }

            return color;
        };

        List<Block> blocksToTint = new ArrayList<>();
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (id == null) {
                continue;
            }
            boolean isVanillaLeafLitter = id.getNamespace().equals("minecraft") && id.getPath().equals("leaf_litter");
            if (isVanillaLeafLitter) {
                blocksToTint.add(entry.getValue());
            }
        }

        if (!blocksToTint.isEmpty()) {
            ColorProviderRegistry.BLOCK.register(blockColor, blocksToTint.toArray(Block[]::new));
        }
    }

    private static int computeTint(BlockAndTintGetter level, BlockPos pos, ResourceLocation blockId, int globalMin, int globalMax, ParsedBiomeRanges parsed) {
        int color = resolveBaseColor(level, pos);
        LevelReader reader = level instanceof LevelReader lr ? lr : null;
        if (!FabricClientConfig.leafLitterTintClampEnabled) {
            return color;
        }

        int min = globalMin;
        int max = globalMax;

        if (reader != null && pos != null) {
            var holder = reader.getBiome(pos);
            boolean matched = false;
            var keyOpt = holder.unwrapKey();
            if (keyOpt.isPresent()) {
                ResourceLocation biomeId = keyOpt.get().location();
                int[] range = parsed.byBiome().get(biomeId);
                if (range != null && range.length >= 2) {
                    min = range[0];
                    max = range[1];
                    matched = true;
                }
            }

            if (!matched) {
                for (BiomeTagRange tagRange : parsed.byTag()) {
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

        if (min != -1 && max != -1) {
            color = clampRgb(color, min, max);
        }
        return color;
    }

    private static int resolveBaseColor(BlockAndTintGetter level, BlockPos pos) {
        int defaultColor = FoliageColor.get(0.5D, 1.0D);
        if (level == null || pos == null) {
            return defaultColor;
        }

        Minecraft mc = Minecraft.getInstance();
        BlockColors blockColors = mc.getBlockColors();
        if (blockColors != null) {
            BlockState sampleState = "grass".equalsIgnoreCase(FabricClientConfig.leafLitterTintSource)
                    ? Blocks.GRASS_BLOCK.defaultBlockState()
                    : Blocks.OAK_LEAVES.defaultBlockState();
            int sampled = blockColors.getColor(sampleState, level, pos, 0);
            if (sampled != -1) {
                return sampled;
            }
        }

        if ("grass".equalsIgnoreCase(FabricClientConfig.leafLitterTintSource)) {
            return BiomeColors.getAverageGrassColor(level, pos);
        }

        return BiomeColors.getAverageFoliageColor(level, pos);
    }

    private static ParsedBiomeRanges parseBiomeRanges(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return new ParsedBiomeRanges(Map.of(), List.of());
        }

        Map<ResourceLocation, int[]> byBiome = new HashMap<>();
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
                ResourceLocation tagLoc = tryParseId(key.substring(1).trim());
                if (tagLoc != null) {
                    byTag.add(new BiomeTagRange(TagKey.create(Registries.BIOME, tagLoc), new int[]{min, max}));
                }
            } else {
                ResourceLocation biome = tryParseId(key);
                if (biome != null) {
                    byBiome.put(biome, new int[]{min, max});
                }
            }
        }

        return new ParsedBiomeRanges(Map.copyOf(byBiome), List.copyOf(byTag));
    }

    private static ResourceLocation tryParseId(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return ResourceLocation.parse(s);
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
        r = Math.max(Math.min(minR, maxR), Math.min(Math.max(minR, maxR), r));
        g = Math.max(Math.min(minG, maxG), Math.min(Math.max(minG, maxG), g));
        b = Math.max(Math.min(minB, maxB), Math.min(Math.max(minB, maxB), b));
        return (r << 16) | (g << 8) | b;
    }
}
