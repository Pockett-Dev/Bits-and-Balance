package org.onenonly.bitsandbalance.client;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionFlag;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.onenonly.bitsandbalance.ClientConfig;
import org.onenonly.bitsandbalance.ContentConfig;
import org.onenonly.bitsandbalance.GameplayConfig;
import org.onenonly.bitsandbalance.WorldConfig;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

final class NeoForgeYaclConfigScreen {
    private static final Set<String> BROAD_GROUPS = Set.of(
            "balance",
            "client",
            "combat",
            "lootTables",
            "mechanics",
            "mobs",
            "potions",
            "recipes",
            "tweaks",
            "visuals"
    );

    private NeoForgeYaclConfigScreen() {
    }

    static Screen create(Screen parent) {
        List<ConfigSection> sections = List.of(
                new ConfigSection("Client", ClientConfig.class, ClientConfig.SPEC),
                new ConfigSection("Gameplay", GameplayConfig.class, GameplayConfig.SPEC),
                new ConfigSection("Content", ContentConfig.class, ContentConfig.SPEC),
                new ConfigSection("Worldgen", WorldConfig.class, WorldConfig.SPEC)
        );

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Bits and Balance Config"))
                .save(() -> saveAndRefresh(sections));

        for (ConfigSection section : sections) {
            ConfigCategory.Builder categoryBuilder = ConfigCategory.createBuilder()
                    .name(Component.literal(section.name()));

            for (FeatureBucket bucket : featureBuckets(section)) {
                addBucket(categoryBuilder, bucket);
            }

            builder.category(categoryBuilder.build());
        }

        ConfigCategory.Builder debugCategoryBuilder = ConfigCategory.createBuilder()
                .name(Component.literal("Debug"));
        boolean hasDebugOptions = false;
        for (ConfigSection section : sections) {
            for (FeatureBucket bucket : featureBuckets(section, true)) {
                addBucket(debugCategoryBuilder, bucket.withDisplayName(section.name() + " - " + bucket.displayName()));
                hasDebugOptions = true;
            }
        }
        if (hasDebugOptions) {
            builder.category(debugCategoryBuilder.build());
        }

        return builder.build().generateScreen(parent);
    }

    private static void saveAndRefresh(List<ConfigSection> sections) {
        for (ConfigSection section : sections) {
            section.spec().save();
        }
        for (ConfigSection section : sections) {
            refreshRuntime(section.configClass());
        }
    }

    private static void refreshRuntime(Class<?> configClass) {
        try {
            Method refreshRuntime = configClass.getDeclaredMethod("refreshRuntime");
            refreshRuntime.setAccessible(true);
            refreshRuntime.invoke(null);
        } catch (NoSuchMethodException ignored) {
            // Config sections without runtime mirrors do not need an immediate refresh hook.
        } catch (IllegalAccessException | InvocationTargetException e) {
            Throwable cause = e instanceof InvocationTargetException invocation && invocation.getCause() != null
                    ? invocation.getCause()
                    : e;
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Failed to refresh " + configClass.getSimpleName() + " after saving config", cause);
        }
    }

    private static dev.isxander.yacl3.api.Option<?> createOption(ConfigOption option, String title) {
        if (option.value() instanceof ModConfigSpec.BooleanValue booleanValue) {
            dev.isxander.yacl3.api.Option.Builder<Boolean> builder = dev.isxander.yacl3.api.Option.<Boolean>createBuilder()
                    .name(Component.literal(title))
                    .description(description(option))
                    .binding(
                            Boolean.TRUE.equals(booleanValue.getDefault()),
                            booleanValue::get,
                            booleanValue::set
                    )
                    .controller(control -> BooleanControllerBuilder.create(control)
                            .valueFormatter(value -> Component.literal(value ? "True" : "False")
                                    .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED))
                            .coloured(true));
            addRestartFlag(builder, option);
            return builder.build();
        }

        if (isColorOption(option)) {
            dev.isxander.yacl3.api.Option.Builder<Color> builder = dev.isxander.yacl3.api.Option.<Color>createBuilder(Color.class)
                    .name(Component.literal(title))
                    .description(description(option))
                    .binding(
                            parseColor(stringify(option.value().getDefault())),
                            () -> parseColor(stringify(option.value().get())),
                            value -> setString(option.value(), formatColor(value, colorAllowsAlpha(option)))
                    )
                    .controller(control -> ColorControllerBuilder.create(control)
                            .allowAlpha(colorAllowsAlpha(option)));
            addRestartFlag(builder, option);
            return builder.build();
        }

        dev.isxander.yacl3.api.Option<?> enumOption = createEnumOption(option, title);
        if (enumOption != null) {
            return enumOption;
        }

        if (isStringListOption(option)) {
            dev.isxander.yacl3.api.Option.Builder<String> builder = dev.isxander.yacl3.api.Option.<String>createBuilder()
                    .name(Component.literal(title))
                    .description(description(option))
                    .binding(
                            listText(option, option.value().getDefault()),
                            () -> listText(option, option.value().get()),
                            text -> setFromText(option.value(), text)
                    )
                    .controller(StringControllerBuilder::create);
            addRestartFlag(builder, option);
            return builder.build();
        }

        if (isIntegerSliderOption(option)) {
            SliderSpec<Integer> spec = integerSliderSpec(option);
            dev.isxander.yacl3.api.Option.Builder<Integer> builder = dev.isxander.yacl3.api.Option.<Integer>createBuilder(Integer.class)
                    .name(Component.literal(title))
                    .description(description(option))
                    .binding(
                            parseInteger(stringify(option.value().getDefault())),
                            () -> parseInteger(stringify(option.value().get())),
                            value -> setNumber(option.value(), value)
                    )
                    .controller(control -> IntegerSliderControllerBuilder.create(control)
                            .range(spec.min(), spec.max())
                            .step(spec.step()));
            addRestartFlag(builder, option);
            return builder.build();
        }

        if (isDoubleSliderOption(option)) {
            SliderSpec<Double> spec = doubleSliderSpec(option);
            dev.isxander.yacl3.api.Option.Builder<Double> builder = dev.isxander.yacl3.api.Option.<Double>createBuilder(Double.class)
                    .name(Component.literal(title))
                    .description(description(option))
                    .binding(
                            parseDouble(stringify(option.value().getDefault())),
                            () -> parseDouble(stringify(option.value().get())),
                            value -> setNumber(option.value(), value)
                    )
                    .controller(control -> DoubleSliderControllerBuilder.create(control)
                            .range(spec.min(), spec.max())
                            .step(spec.step()));
            addRestartFlag(builder, option);
            return builder.build();
        }

        dev.isxander.yacl3.api.Option.Builder<String> builder = dev.isxander.yacl3.api.Option.<String>createBuilder()
                .name(Component.literal(title))
                .description(description(option))
                .binding(
                        stringify(option.value().getDefault()),
                        () -> stringify(option.value().get()),
                        text -> setFromText(option.value(), text)
                )
                .controller(StringControllerBuilder::create);
        addRestartFlag(builder, option);
        return builder.build();
    }

    private static void addRestartFlag(dev.isxander.yacl3.api.Option.Builder<?> builder, ConfigOption option) {
        if (option.value().getSpec().restartType() != ModConfigSpec.RestartType.NONE) {
            builder.flag(OptionFlag.GAME_RESTART);
        }
    }

    private static void addBucket(ConfigCategory.Builder categoryBuilder, FeatureBucket bucket) {
        OptionGroup.Builder groupBuilder = OptionGroup.createBuilder()
                .name(Component.literal(bucket.displayName()))
                .description(groupDescription(bucket))
                .collapsed(false);
        for (ConfigOption option : bucket.options()) {
            groupBuilder.option(createOption(option, optionTitle(bucket, option)));
        }
        categoryBuilder.group(groupBuilder.build());
    }

    private static List<FeatureBucket> featureBuckets(ConfigSection section) {
        return featureBuckets(section, false);
    }

    private static List<FeatureBucket> featureBuckets(ConfigSection section, boolean debugOnly) {
        Map<String, FeatureBucket> buckets = new LinkedHashMap<>();
        for (ConfigOption option : discoverOptions(section)) {
            if (isDebugOption(option) != debugOnly) {
                continue;
            }
            String displayName = bucketDisplayName(option);
            String key = displayName;
            FeatureBucket bucket = buckets.computeIfAbsent(key, ignored -> new FeatureBucket(displayName));
            bucket.options().add(option);
        }

        List<FeatureBucket> ordered = new ArrayList<>(buckets.values());
        ordered.sort(Comparator.comparing(NeoForgeYaclConfigScreen::bucketSortKey, String.CASE_INSENSITIVE_ORDER));
        for (FeatureBucket bucket : ordered) {
            bucket.options().sort(Comparator
                    .comparingInt((ConfigOption option) -> optionSortRank(bucket, option))
                    .thenComparing((ConfigOption option) -> dseOptionSortKey(optionKey(option)), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing((ConfigOption option) -> option.subFeatureName().isBlank() ? "\u0000" : option.subFeatureName(), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(option -> optionTitle(bucket, option), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(option -> String.join(".", option.value().getPath()), String.CASE_INSENSITIVE_ORDER));
        }
        return ordered;
    }

    private static boolean isDebugOption(ConfigOption option) {
        String key = optionKey(option).toLowerCase(Locale.ROOT);
        return key.contains("debug") || key.equals("logmissingids");
    }

    private static String bucketDisplayName(ConfigOption option) {
        if (!option.featureName().equals("Depth Scaling Enemies")) {
            if (option.featureName().equals("Vein Features")) {
                return oreVeinBucketDisplayName(optionKey(option));
            }
            if (option.featureName().equals("Raw Quartz Block")) {
                return "Ore Veins - Nether Quartz";
            }
            return canonicalFeatureName(option.featureName());
        }
        return switch (dseSection(optionKey(option))) {
            case "surface" -> "Depth Scaling Enemies - Surface Tier";
            case "mid" -> "Depth Scaling Enemies - Mid-Depth Tier";
            case "deep" -> "Depth Scaling Enemies - Deep Tier";
            default -> "Depth Scaling Enemies - General";
        };
    }

    private static String canonicalFeatureName(String featureName) {
        String canonical = featureName;
        while (canonical.endsWith(" Settings")) {
            canonical = canonical.substring(0, canonical.length() - " Settings".length()).trim();
        }
        if (canonical.equals("Biome Item Tinting")) {
            return "Biome Item Tint";
        }
        return canonical;
    }

    private static String bucketSortKey(FeatureBucket bucket) {
        return switch (bucket.displayName()) {
            case "Depth Scaling Enemies - General" -> "Depth Scaling Enemies 0";
            case "Depth Scaling Enemies - Surface Tier" -> "Depth Scaling Enemies 1";
            case "Depth Scaling Enemies - Mid-Depth Tier" -> "Depth Scaling Enemies 2";
            case "Depth Scaling Enemies - Deep Tier" -> "Depth Scaling Enemies 3";
            case "Ore Veins - Nether Gold" -> "Ore Veins 0";
            case "Ore Veins - Nether Quartz" -> "Ore Veins 1";
            case "Ore Veins - Coal Andesite" -> "Ore Veins 2";
            case "Ore Veins - Coal Tuff" -> "Ore Veins 3";
            case "Ore Veins - Debug" -> "Ore Veins 4";
            default -> bucket.displayName();
        };
    }

    private static String dseOptionSortKey(String key) {
        return switch (key) {
            case "enabled" -> "0";
            case "enableSurfaceTier" -> "1";
            case "enableMidTier" -> "2";
            case "enableDeepTier" -> "3";
            case "overworldOnly" -> "4";
            default -> "9";
        };
    }

    private static String oreVeinBucketDisplayName(String key) {
        return switch (key) {
            case "enableNetherGoldVeins" -> "Ore Veins - Nether Gold";
            case "enableNetherQuartzVeins", "enableRawQuartzBlockInQuartzVeins" -> "Ore Veins - Nether Quartz";
            case "enableCoalAndesiteVein", "enableCoalAndesiteVeins" -> "Ore Veins - Coal Andesite";
            case "enableCoalTuffVeins" -> "Ore Veins - Coal Tuff";
            case "debugVeins" -> "Ore Veins - Debug";
            default -> "Ore Veins";
        };
    }

    private static String dseSection(String key) {
        if (key.startsWith("surface")) {
            return "surface";
        }
        if (key.startsWith("mid")) {
            return "mid";
        }
        if (key.startsWith("deep")) {
            return "deep";
        }
        return "general";
    }

    private static String optionKey(ConfigOption option) {
        List<String> path = option.value().getPath();
        return path.isEmpty() ? "" : path.get(path.size() - 1);
    }

    private static List<ConfigOption> discoverOptions(ConfigSection section) {
        List<ConfigOption> options = new ArrayList<>();
        for (Field field : section.configClass().getDeclaredFields()) {
            if (field.getName().equals("BLOCKED_MESSAGES_VERSION")
                    || !Modifier.isStatic(field.getModifiers()) || !ModConfigSpec.ConfigValue.class.isAssignableFrom(field.getType())) {
                continue;
            }

            try {
                field.setAccessible(true);
                ModConfigSpec.ConfigValue<?> value = (ModConfigSpec.ConfigValue<?>) field.get(null);
                options.add(createOption(section, field.getName(), value));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to read NeoForge config field " + field.getName(), e);
            }
        }
        return options;
    }

    private static ConfigOption createOption(ConfigSection section, String fieldName, ModConfigSpec.ConfigValue<?> value) {
        List<String> path = value.getPath();
        int featureIndex = featureIndex(path);
        List<String> featurePath = path.subList(0, Math.min(featureIndex + 1, path.size()));
        String featureName = displayNameOverride(sectionName(section.spec(), featurePath, prettify(path.get(featureIndex))));
        String subFeatureName = displayNameOverride(subFeatureName(section.spec(), path, featureIndex));
        String settingName = displayNameOverride(settingName(path, fieldName));
        return new ConfigOption(section, value, featureName, String.join(".", featurePath), subFeatureName, settingName);
    }

    private static int featureIndex(List<String> path) {
        if (path.size() <= 2) {
            return 0;
        }
        String first = path.get(0);
        if (BROAD_GROUPS.contains(first)) {
            return 1;
        }
        return 0;
    }

    private static String subFeatureName(ModConfigSpec spec, List<String> path, int featureIndex) {
        if (path.size() <= featureIndex + 2) {
            return "";
        }
        List<String> subFeaturePath = path.subList(0, path.size() - 1);
        return sectionName(spec, subFeaturePath, prettify(path.get(path.size() - 2)));
    }

    private static String sectionName(ModConfigSpec spec, List<String> path, String fallback) {
        if (path.isEmpty()) {
            return fallback;
        }
        String comment = spec.getLevelComment(path);
        if (comment == null || comment.isBlank()) {
            return fallback;
        }
        return comment.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse(fallback);
    }

    private static String settingName(List<String> path, String fieldName) {
        String key = path.get(path.size() - 1);
        String endCityLootSetting = endCityLootSettingName(path, key);
        if (!endCityLootSetting.isBlank()) {
            return endCityLootSetting;
        }
        String depthScalingTier = depthScalingTierName(key);
        if (!depthScalingTier.isBlank()) {
            return depthScalingTier;
        }
        if (key.equals("allowMixedCandlePlacement")) {
            return "Mixed Candle Placement";
        }
        if (key.equals("hidePumpkinOverlayOnVanishing")) {
            return "Hide Pumpkin Overlay with Curse of Vanishing";
        }
        if (key.equals("dualRiders")) {
            return "Enabled";
        }
        if (isEnabledKey(key, fieldName)) {
            return "Enabled";
        }
        return prettify(stripEnablePrefix(key));
    }

    private static String endCityLootSettingName(List<String> path, String key) {
        if (path.size() == 2 && path.get(0).equals("endCityLoot") && key.equals("enableEndCityLoot")) {
            return "Loot Injections";
        }
        return "";
    }

    private static String depthScalingTierName(String key) {
        return switch (key) {
            case "enableSurfaceTier" -> "Surface Tier";
            case "enableMidTier" -> "Mid-Depth Tier";
            case "enableDeepTier" -> "Deep Tier";
            default -> "";
        };
    }

    private static boolean isEnabledKey(String key, String fieldName) {
        String lowerKey = key.toLowerCase(Locale.ROOT);
        return lowerKey.equals("enabled")
                || lowerKey.startsWith("enable")
                || fieldName.toLowerCase(Locale.ROOT).startsWith("enable_");
    }

    private static String optionTitle(ConfigOption option) {
        if (isEnabledOption(option) && !option.subFeatureName().isBlank()) {
            return option.subFeatureName();
        }
        if (!option.subFeatureName().isBlank()) {
            return option.subFeatureName() + " " + option.settingName();
        }
        return option.settingName();
    }

    private static String optionTitle(FeatureBucket bucket, ConfigOption option) {
        if (bucket.displayName().equals("Ore Veins - Nether Quartz") && option.featureName().equals("Raw Quartz Block")) {
            return "Raw Quartz";
        }
        String title = optionTitle(option);
        if (option.value() instanceof ModConfigSpec.BooleanValue && isBucketMasterToggle(bucket, option, title)) {
            return "Enabled";
        }
        return title;
    }

    private static boolean isBucketMasterToggle(FeatureBucket bucket, ConfigOption option, String title) {
        if (bucket.options().size() == 1 || title.equals(bucket.displayName())) {
            return true;
        }
        String sourceName = normalizedToggleSourceName(optionKey(option));
        if (sourceName.isBlank()) {
            return false;
        }
        String bucketName = normalizedDisplayName(bucket.displayName());
        String featureName = normalizedDisplayName(option.featureName());
        if (sourceName.equals(bucketName) || sourceName.equals(featureName)) {
            return true;
        }
        if (bucketName.startsWith("oreveins")) {
            String oreName = bucketName.substring("oreveins".length());
            return sourceName.equals(oreName) || sourceName.equals(oreName + "veins");
        }
        return false;
    }

    private static String normalizedToggleSourceName(String sourceKey) {
        if (sourceKey == null || sourceKey.isBlank()) {
            return "";
        }
        String value = sourceKey;
        if (value.startsWith("enable") && value.length() > "enable".length() && Character.isUpperCase(value.charAt("enable".length()))) {
            value = value.substring("enable".length());
        } else if (value.endsWith("Enabled") && value.length() > "Enabled".length()) {
            value = value.substring(0, value.length() - "Enabled".length());
        } else {
            return "";
        }
        return normalizedDisplayName(value);
    }

    private static String normalizedDisplayName(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private static boolean isEnabledOption(ConfigOption option) {
        return option.settingName().equals("Enabled");
    }

    private static int optionSortRank(FeatureBucket bucket, ConfigOption option) {
        String title = optionTitle(bucket, option);
        if (title.equals("Enabled") || isEnabledOption(option)) {
            return 0;
        }
        if (option.value() instanceof ModConfigSpec.BooleanValue && isBucketMasterToggle(bucket, option, optionTitle(option))) {
            return 0;
        }
        return 1;
    }

    private static OptionDescription groupDescription(FeatureBucket bucket) {
        return OptionDescription.of(Component.literal(featureDescription(bucket.displayName())));
    }

    private static String featureDescription(String featureName) {
        return switch (featureName) {
            case "Alternative Recipes" -> "Configure optional replacement and convenience crafting recipes.";
            case "Ancient City Loot" -> "Configure Bits and Balance additions to ancient city loot chests.";
            case "Biome Titles" -> "Configure the title shown when entering a new biome.";
            case "Custom Brewing" -> "Configure Bits and Balance custom brewing recipe support.";
            case "Depth Scaling Enemies - General" -> "Configure the master toggle and dimension restriction for Depth Scaling Enemies.";
            case "Depth Scaling Enemies - Surface Tier" -> "Configure surface-tier Depth Scaling Enemies armor and weapon settings.";
            case "Depth Scaling Enemies - Mid-Depth Tier" -> "Configure mid-depth Depth Scaling Enemies threshold, armor, and weapon settings.";
            case "Depth Scaling Enemies - Deep Tier" -> "Configure deep-tier Depth Scaling Enemies threshold, armor, and weapon settings.";
            case "End City Loot" -> "Configure Bits and Balance additions to end city loot chests.";
            case "Ore Veins - Nether Gold" -> "Configure Nether gold vein world generation.";
            case "Ore Veins - Nether Quartz" -> "Configure Nether quartz vein world generation and raw quartz pockets.";
            case "Ore Veins - Coal Andesite" -> "Configure shallow Overworld coal and andesite vein generation.";
            case "Ore Veins - Coal Tuff" -> "Configure deep Overworld coal and tuff vein generation.";
            case "Ore Veins - Debug" -> "Configure Ore Veins debug logging.";
            case "Ore Smelting XP" -> "Configure XP from smelting or blasting Silk Touched vanilla ore blocks.";
            case "Improved Phantoms" -> "Configure improved phantom behavior and related combat options.";
            case "Log Filtering" -> "Configure client log and warning suppression options.";
            case "Raw Ore Smelting/Blasting" -> "Configure smelting and blasting recipes for raw metal blocks.";
            case "Second Chance" -> "Configure the anti-one-shot safety net for players.";
            case "Stair Recipes" -> "Configure stair recipe output overrides and compact stair recipes.";
            case "Waterlogging" -> "Configure automatic waterlogging behavior for placed blocks.";
            default -> "Configure " + featureName + " settings.";
        };
    }

    private static OptionDescription description(ConfigOption option) {
        OptionDescription.Builder builder = OptionDescription.createBuilder();
        String comment = option.value().getSpec().getComment();
        if (comment != null && !comment.isBlank()) {
            for (String line : comment.lines().map(String::trim).filter(line -> !line.isBlank()).toList()) {
                builder.text(Component.literal(line));
            }
        }
        builder.text(Component.literal(option.section().name() + " / " + option.featureName() + " / " + option.settingName())
                .withStyle(ChatFormatting.GRAY));
        if (option.value().getSpec().restartType() != ModConfigSpec.RestartType.NONE) {
            builder.text(Component.literal("Restart the game or server, then re-open the world, to fully apply this setting.")
                    .withStyle(ChatFormatting.GOLD));
        }
        return builder.build();
    }

    private static void setFromText(ModConfigSpec.ConfigValue<?> value, String text) {
        Object parsed = parseValue(value, text);
        if (value.getSpec().test(parsed)) {
            setUnchecked(value, parsed);
        }
    }

    private static Object parseValue(ModConfigSpec.ConfigValue<?> value, String text) {
        Object defaultValue = value.getDefault();
        Class<?> type = value.getSpec().getClazz();
        String trimmed = text.trim();

        if (defaultValue instanceof Integer || type == Integer.class || type == int.class) {
            return Integer.parseInt(trimmed);
        }
        if (defaultValue instanceof Long || type == Long.class || type == long.class) {
            return Long.parseLong(trimmed);
        }
        if (defaultValue instanceof Double || type == Double.class || type == double.class || defaultValue instanceof Float) {
            return Double.parseDouble(trimmed);
        }
        if (defaultValue instanceof List<?> || List.class.isAssignableFrom(type)) {
            if (value.getPath().get(value.getPath().size() - 1).equals("blockedMessages")) {
                return org.onenonly.bitsandbalance.common.client.LogMessageList.parse(text);
            }
            return parseStringList(text);
        }
        return text;
    }

    private static List<String> parseStringList(String text) {
        return text.lines()
                .flatMap(line -> List.of(line.split(",")).stream())
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .collect(Collectors.toList());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setUnchecked(ModConfigSpec.ConfigValue value, Object parsed) {
        value.set(parsed);
    }

    private static void setString(ModConfigSpec.ConfigValue<?> value, String text) {
        setUnchecked(value, text);
    }

    private static void setList(ModConfigSpec.ConfigValue<?> value, List<String> values) {
        if (isSingleStringListOption(value)) {
            setUnchecked(value, values.isEmpty() ? "" : values.get(0));
        } else {
            setUnchecked(value, values);
        }
    }

    private static void setNumber(ModConfigSpec.ConfigValue<?> value, Number number) {
        Object defaultValue = value.getDefault();
        Class<?> type = value.getSpec().getClazz();
        if (defaultValue instanceof Integer || type == Integer.class || type == int.class) {
            setUnchecked(value, number.intValue());
        } else if (defaultValue instanceof Long || type == Long.class || type == long.class) {
            setUnchecked(value, number.longValue());
        } else if (defaultValue instanceof Float || type == Float.class || type == float.class) {
            setUnchecked(value, number.floatValue());
        } else {
            setUnchecked(value, number.doubleValue());
        }
    }

    private static String listText(ConfigOption option, Object value) {
        if (optionKey(option).equals("blockedMessages") && value instanceof java.util.Collection<?> messages) {
            return org.onenonly.bitsandbalance.common.client.LogMessageList.format(messages);
        }
        return stringify(value);
    }

    private static String stringify(Object value) {
        if (value instanceof List<?> list) {
            StringJoiner joiner = new StringJoiner(", ");
            for (Object item : list) {
                joiner.add(Objects.toString(item, ""));
            }
            return joiner.toString();
        }
        return Objects.toString(value, "");
    }

    private static boolean isColorOption(ConfigOption option) {
        return switch (optionKey(option)) {
            case "globalMinColor", "globalMaxColor", "highlightColorHex" -> true;
            default -> false;
        };
    }

    private static boolean colorAllowsAlpha(ConfigOption option) {
        return optionKey(option).equals("highlightColorHex");
    }

    private static Color parseColor(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        try {
            if (normalized.length() == 8) {
                long argb = Long.parseLong(normalized, 16);
                return new Color((int) argb, true);
            }
            if (normalized.length() == 6) {
                return new Color(Integer.parseInt(normalized, 16));
            }
        } catch (NumberFormatException ignored) {
            // Fall back to black so malformed config values do not crash the config screen.
        }
        return Color.BLACK;
    }

    private static String formatColor(Color color, boolean includeAlpha) {
        if (includeAlpha && color.getAlpha() < 255) {
            return "%02X%02X%02X%02X".formatted(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
        }
        return "%02X%02X%02X".formatted(color.getRed(), color.getGreen(), color.getBlue());
    }

    private static dev.isxander.yacl3.api.Option<?> createEnumOption(ConfigOption option, String title) {
        return switch (optionKey(option)) {
            case "slot" -> option.featureName().equals("Biome Titles")
                    ? createEnumOption(option, title, BiomeTitleSlot.class, BiomeTitleSlot.from(stringify(option.value().get())))
                    : null;
            case "source" -> option.featureName().equals("Leaf Litter / Leaf Pile Tint")
                    ? createEnumOption(option, title, LeafLitterTintSource.class, LeafLitterTintSource.from(stringify(option.value().get())))
                    : null;
            case "soundSource" -> createEnumOption(option, title, MentionSoundSource.class, MentionSoundSource.from(stringify(option.value().get())));
            case "mode" -> option.featureName().equals("Waterlogging")
                    ? createEnumOption(option, title, WaterloggingMode.class, WaterloggingMode.from(stringify(option.value().get())))
                    : null;
            default -> null;
        };
    }

    private static <E extends Enum<E> & StringBackedEnum> dev.isxander.yacl3.api.Option<?> createEnumOption(
            ConfigOption option,
            String title,
            Class<E> enumClass,
            E current
    ) {
        dev.isxander.yacl3.api.Option.Builder<E> builder = dev.isxander.yacl3.api.Option.<E>createBuilder(enumClass)
                .name(Component.literal(title))
                .description(description(option))
                .binding(
                        current,
                        () -> fromString(enumClass, stringify(option.value().get()), current),
                        value -> setString(option.value(), value.configValue())
                )
                .controller(control -> EnumControllerBuilder.create(control)
                        .enumClass(enumClass)
                        .valueFormatter(value -> Component.literal(value.displayName())));
        addRestartFlag(builder, option);
        return builder.build();
    }

    private static <E extends Enum<E> & StringBackedEnum> E fromString(Class<E> enumClass, String text, E fallback) {
        String normalized = normalizeEnumText(text);
        for (E value : enumClass.getEnumConstants()) {
            if (normalizeEnumText(value.configValue()).equals(normalized) || normalizeEnumText(value.name()).equals(normalized)) {
                return value;
            }
        }
        return fallback;
    }

    private static String normalizeEnumText(String text) {
        return text == null ? "" : text.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static boolean isStringListOption(ConfigOption option) {
        Object defaultValue = option.value().getDefault();
        boolean listBacked = defaultValue instanceof List<?> || List.class.isAssignableFrom(option.value().getSpec().getClazz());
        if (!listBacked && !isSingleStringListOption(option.value())) {
            return false;
        }
        return switch (optionKey(option)) {
            case "whitelist",
                 "blacklist",
                 "grassFallbackItems",
                 "foliageFallbackItems",
                 "biomeRanges",
                 "soundEvent",
                 "triggerModes",
                 "blacklistBlockIds",
                 "blacklistNameContains",
                 "snowballNetherEntities",
                 "blockedMessages",
                 "blockIds" -> true;
            default -> false;
        };
    }

    private static boolean isSingleStringListOption(ModConfigSpec.ConfigValue<?> value) {
        return value.getPath().contains("chatMentions") && optionKey(value).equals("soundEvent");
    }

    private static String optionKey(ModConfigSpec.ConfigValue<?> value) {
        List<String> path = value.getPath();
        return path.isEmpty() ? "" : path.get(path.size() - 1);
    }

    private static boolean isIntegerSliderOption(ConfigOption option) {
        Object defaultValue = option.value().getDefault();
        Class<?> type = option.value().getSpec().getClazz();
        if (!(defaultValue instanceof Integer || type == Integer.class || type == int.class)) {
            return false;
        }
        String key = normalizedKey(option);
        return key.contains("offset")
                || key.contains("position")
                || key.contains("size")
                || key.contains("count")
                || key.contains("volume")
                || key.contains("pitch")
                || key.contains("chance")
                || key.contains("radius")
                || key.contains("distance")
                || key.contains("scale");
    }

    private static boolean isDoubleSliderOption(ConfigOption option) {
        Object defaultValue = option.value().getDefault();
        Class<?> type = option.value().getSpec().getClazz();
        if (!(defaultValue instanceof Double || type == Double.class || type == double.class || defaultValue instanceof Float)) {
            return false;
        }
        String key = normalizedKey(option);
        return key.contains("offset")
                || key.contains("position")
                || key.contains("volume")
                || key.contains("pitch")
                || key.contains("chance")
                || key.contains("radius")
                || key.contains("distance")
                || key.contains("scale")
                || key.contains("speed")
                || key.contains("multiplier")
                || key.contains("strength")
                || key.contains("health");
    }

    private static SliderSpec<Integer> integerSliderSpec(ConfigOption option) {
        String key = normalizedKey(option);
        if (key.contains("offset") || key.contains("position")) {
            return new SliderSpec<>(-256, 256, 1);
        }
        if (key.contains("size") || key.contains("count")) {
            return new SliderSpec<>(0, 64, 1);
        }
        return new SliderSpec<>(0, 100, 1);
    }

    private static SliderSpec<Double> doubleSliderSpec(ConfigOption option) {
        String key = normalizedKey(option);
        if (key.contains("volume") || key.contains("pitch")) {
            return new SliderSpec<>(0.0D, 2.0D, 0.05D);
        }
        if (key.contains("chance") || key.contains("strength")) {
            return new SliderSpec<>(0.0D, 1.0D, 0.01D);
        }
        if (key.contains("health")) {
            return new SliderSpec<>(0.0D, 20.0D, 0.5D);
        }
        if (key.contains("scale")) {
            return new SliderSpec<>(0.25D, 5.0D, 0.05D);
        }
        if (key.contains("speed") || key.contains("multiplier")) {
            return new SliderSpec<>(0.0D, 10.0D, 0.05D);
        }
        if (key.contains("radius") || key.contains("distance")) {
            return new SliderSpec<>(0.0D, 128.0D, 0.5D);
        }
        return new SliderSpec<>(0.0D, 100.0D, 1.0D);
    }

    private static String normalizedKey(ConfigOption option) {
        return (String.join(".", option.value().getPath()) + " " + option.settingName()).toLowerCase(Locale.ROOT);
    }

    private static int parseInteger(String text) {
        try {
            return Integer.parseInt(text == null ? "0" : text.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static double parseDouble(String text) {
        try {
            return Double.parseDouble(text == null ? "0" : text.trim());
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }

    private static String stripEnablePrefix(String key) {
        if (key.length() > "enable".length() && key.startsWith("enable") && Character.isUpperCase(key.charAt("enable".length()))) {
            return key.substring("enable".length());
        }
        return key;
    }

    private static String displayNameOverride(String value) {
        return switch (value) {
            case "Bottle of Cloud", "Bottle Of Cloud" -> "Bottle o' Cloud";
            case "Horses" -> "Dual Riders for Horses";
            case "Raw Ore Cooking" -> "Raw Ore Smelting/Blasting";
            default -> value;
        };
    }

    private static String prettify(String key) {
        StringBuilder result = new StringBuilder();
        String normalized = key.replace('_', ' ').replace('-', ' ').replace('.', ' ');
        char previous = 0;
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (i > 0 && Character.isUpperCase(current) && Character.isLowerCase(previous)) {
                result.append(' ');
            }
            result.append(current);
            previous = current;
        }
        String[] words = result.toString().trim().split("\\s+");
        StringJoiner title = new StringJoiner(" ");
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            title.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
        }
        return normalizeAcronyms(title.toString());
    }

    private static String normalizeAcronyms(String value) {
        return value
                .replace(" Ids", " IDs")
                .replace(" Id", " ID")
                .replace(" Xp", " XP")
                .replace(" Fps", " FPS")
                .replace(" Hex", " HEX");
    }

    private record ConfigSection(String name, Class<?> configClass, ModConfigSpec spec) {
    }

    private record ConfigOption(ConfigSection section, ModConfigSpec.ConfigValue<?> value, String featureName,
                                String featurePath, String subFeatureName, String settingName) {
    }

    private record FeatureBucket(String displayName, List<ConfigOption> options) {
        FeatureBucket(String displayName) {
            this(displayName, new ArrayList<>());
        }

        FeatureBucket withDisplayName(String displayName) {
            return new FeatureBucket(displayName, options);
        }
    }

    private interface StringBackedEnum {
        String configValue();

        String displayName();
    }

    private enum BiomeTitleSlot implements StringBackedEnum {
        TITLE("title", "Title"),
        SUBTITLE("subtitle", "Subtitle");

        private final String configValue;
        private final String displayName;

        BiomeTitleSlot(String configValue, String displayName) {
            this.configValue = configValue;
            this.displayName = displayName;
        }

        static BiomeTitleSlot from(String text) {
            return normalizeEnumText(text).equals("TITLE") ? TITLE : SUBTITLE;
        }

        @Override
        public String configValue() {
            return configValue;
        }

        @Override
        public String displayName() {
            return displayName;
        }
    }

    private enum LeafLitterTintSource implements StringBackedEnum {
        FOLIAGE("foliage", "Foliage"),
        GRASS("grass", "Grass");

        private final String configValue;
        private final String displayName;

        LeafLitterTintSource(String configValue, String displayName) {
            this.configValue = configValue;
            this.displayName = displayName;
        }

        static LeafLitterTintSource from(String text) {
            return normalizeEnumText(text).equals("GRASS") ? GRASS : FOLIAGE;
        }

        @Override
        public String configValue() {
            return configValue;
        }

        @Override
        public String displayName() {
            return displayName;
        }
    }

    private enum MentionSoundSource implements StringBackedEnum {
        MASTER,
        MUSIC,
        RECORDS,
        WEATHER,
        BLOCKS,
        HOSTILE,
        NEUTRAL,
        PLAYERS,
        AMBIENT,
        VOICE,
        UI;

        static MentionSoundSource from(String text) {
            return fromString(MentionSoundSource.class, text, PLAYERS);
        }

        @Override
        public String configValue() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String displayName() {
            return Component.translatable("soundCategory." + name().toLowerCase(Locale.ROOT)).getString();
        }
    }

    private enum WaterloggingMode implements StringBackedEnum {
        BLACKLIST("BLACKLIST", "Blacklist"),
        WHITELIST("WHITELIST", "Whitelist");

        private final String configValue;
        private final String displayName;

        WaterloggingMode(String configValue, String displayName) {
            this.configValue = configValue;
            this.displayName = displayName;
        }

        static WaterloggingMode from(String text) {
            String normalized = normalizeEnumText(text);
            return normalized.equals("ALLOWLIST") || normalized.equals("WHITELIST") ? WHITELIST : BLACKLIST;
        }

        @Override
        public String configValue() {
            return configValue;
        }

        @Override
        public String displayName() {
            return displayName;
        }
    }

    private record SliderSpec<T extends Number>(T min, T max, T step) {
    }
}
