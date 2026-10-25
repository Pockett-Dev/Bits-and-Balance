package org.onenonly.bitsandbalance.fabric.config.screen;

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

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class FabricYaclConfigScreen {
    private FabricYaclConfigScreen() {
    }

    static Screen create(Screen parent) {
        List<FabricConfigScreenModel.Category> categories = FabricConfigScreenModel.categories();
        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Bits and Balance Config"))
                .save(() -> saveAll(categories));

        for (FabricConfigScreenModel.Category category : categories) {
            ConfigCategory.Builder categoryBuilder = ConfigCategory.createBuilder()
                    .name(Component.literal(category.name()));

            for (FeatureBucket bucket : featureBuckets(category)) {
                addBucket(categoryBuilder, bucket);
            }

            builder.category(categoryBuilder.build());
        }

        return builder.build().generateScreen(parent);
    }

    private static dev.isxander.yacl3.api.Option<?> createOption(FabricConfigScreenModel.Option option, String title) {
        if (option.isBoolean()) {
            dev.isxander.yacl3.api.Option.Builder<Boolean> builder = dev.isxander.yacl3.api.Option.<Boolean>createBuilder()
                    .name(Component.literal(title))
                    .description(description(option))
                    .binding(
                            option.booleanValue(),
                            option::booleanValue,
                            value -> setBoolean(option, value)
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
                            parseColor(option.pendingText()),
                            () -> parseColor(option.pendingText()),
                            value -> option.setPendingText(formatColor(value, colorAllowsAlpha(option)))
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
                            option.pendingText(),
                            option::pendingText,
                            option::setPendingText
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
                            parseInteger(option.pendingText()),
                            () -> parseInteger(option.pendingText()),
                            value -> option.setPendingText(Integer.toString(value))
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
                            parseDouble(option.pendingText()),
                            () -> parseDouble(option.pendingText()),
                            value -> option.setPendingText(trimDouble(value))
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
                        option.pendingText(),
                        option::pendingText,
                        option::setPendingText
                )
                .controller(StringControllerBuilder::create);
        addRestartFlag(builder, option);
        return builder.build();
    }

    private static void addRestartFlag(dev.isxander.yacl3.api.Option.Builder<?> builder, FabricConfigScreenModel.Option option) {
        if (option.restartRecommended()) {
            builder.flag(OptionFlag.GAME_RESTART);
        }
    }

    private static void addBucket(ConfigCategory.Builder categoryBuilder, FeatureBucket bucket) {
        OptionGroup.Builder groupBuilder = OptionGroup.createBuilder()
                .name(Component.literal(bucket.displayName()))
                .description(groupDescription(bucket))
                .collapsed(false);
        for (FabricConfigScreenModel.Option option : bucket.options()) {
            groupBuilder.option(createOption(option, optionTitle(bucket, option)));
        }
        categoryBuilder.group(groupBuilder.build());
    }

    private static List<FeatureBucket> featureBuckets(FabricConfigScreenModel.Category category) {
        Map<String, FeatureBucket> buckets = new LinkedHashMap<>();
        for (FabricConfigScreenModel.Group group : category.groups()) {
            for (FabricConfigScreenModel.Option option : group.options()) {
                String displayName = bucketDisplayName(option);
                String key = displayName;
                FeatureBucket bucket = buckets.computeIfAbsent(key, ignored -> new FeatureBucket(displayName, group.name()));
                bucket.options().add(option);
            }
        }

        List<FeatureBucket> ordered = new ArrayList<>(buckets.values());
        ordered.sort(Comparator
                .comparing(FabricYaclConfigScreen::bucketSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(FeatureBucket::sourceGroup, String.CASE_INSENSITIVE_ORDER));
        for (FeatureBucket bucket : ordered) {
            bucket.options().sort(Comparator
                    .comparingInt((FabricConfigScreenModel.Option option) -> optionSortRank(bucket, option))
                    .thenComparing((FabricConfigScreenModel.Option option) -> dseOptionSortKey(option.sourceKey()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing((FabricConfigScreenModel.Option option) -> subFeatureSortKey(option), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing((FabricConfigScreenModel.Option option) -> optionTitle(bucket, option), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(FabricConfigScreenModel.Option::name, String.CASE_INSENSITIVE_ORDER));
        }
        return ordered;
    }

    private static String bucketDisplayName(FabricConfigScreenModel.Option option) {
        if (!option.featureName().equals("Depth Scaling Enemies")) {
            if (option.featureName().equals("Ore Veins")) {
                return oreVeinBucketDisplayName(option.sourceKey());
            }
            return canonicalFeatureName(option.featureName());
        }
        return switch (dseSection(option.sourceKey())) {
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
            case "Ore Veins - Coal Andesite" -> "Ore Veins 3";
            case "Ore Veins - Coal Tuff" -> "Ore Veins 4";
            case "Ore Veins - Debug" -> "Ore Veins 5";
            default -> bucket.displayName();
        };
    }

    private static String dseOptionSortKey(String key) {
        return switch (key) {
            case "dseEnabled" -> "0";
            case "dseEnableSurfaceTier" -> "1";
            case "dseEnableMidTier" -> "2";
            case "dseEnableDeepTier" -> "3";
            case "dseOverworldOnly" -> "4";
            default -> "9";
        };
    }

    private static String oreVeinBucketDisplayName(String key) {
        return switch (key) {
            case "enableNetherGoldVeins" -> "Ore Veins - Nether Gold";
            case "enableNetherQuartzVeins", "enableRawQuartzBlockInQuartzVeins", "enableRawQuartzBlock" -> "Ore Veins - Nether Quartz";
            case "enableCoalAndesiteVeins" -> "Ore Veins - Coal Andesite";
            case "enableCoalTuffVeins" -> "Ore Veins - Coal Tuff";
            case "debugVeins" -> "Ore Veins - Debug";
            default -> "Ore Veins";
        };
    }

    private static String dseSection(String key) {
        if (key.startsWith("dseSurface")) {
            return "surface";
        }
        if (key.startsWith("dseMid")) {
            return "mid";
        }
        if (key.startsWith("dseDeep")) {
            return "deep";
        }
        return "general";
    }

    private static String subFeatureSortKey(FabricConfigScreenModel.Option option) {
        return option.subFeatureName().isBlank() ? "\u0000" : option.subFeatureName();
    }

    private static OptionDescription groupDescription(FeatureBucket group) {
        return OptionDescription.of(Component.literal(FabricConfigScreenModel.featureDescription(group.displayName(), group.sourceGroup())));
    }

    private static OptionDescription description(FabricConfigScreenModel.Option option) {
        OptionDescription.Builder builder = OptionDescription.createBuilder()
                .text(Component.literal(option.description()));
        String path = optionPath(option);
        if (!path.isBlank()) {
            builder.text(Component.literal(path).withStyle(ChatFormatting.GRAY));
        }
        if (option.restartRecommended()) {
            builder.text(Component.literal("Restart the game or server, then re-open the world, to fully apply this setting.")
                    .withStyle(ChatFormatting.GOLD));
        }
        return builder.build();
    }

    private static String optionPath(FabricConfigScreenModel.Option option) {
        StringBuilder path = new StringBuilder(option.group()).append(" / ").append(option.featureName());
        if (!option.settingName().equals(optionTitle(option)) && option.subFeatureName().isBlank()) {
            path.append(" / ").append(option.settingName());
        }
        return path.toString();
    }

    private static String optionTitle(FabricConfigScreenModel.Option option) {
        if (isEnabledOption(option) && !option.subFeatureName().isBlank()) {
            return option.subFeatureName();
        }
        if (isEnabledOption(option)) {
            return "Enabled";
        }
        if (!option.subFeatureName().isBlank()) {
            return option.subFeatureName() + " " + option.settingName();
        }
        return !option.settingName().equals(option.name()) ? option.settingName() : option.name();
    }

    private static String optionTitle(FeatureBucket bucket, FabricConfigScreenModel.Option option) {
        String title = optionTitle(option);
        if (option.isBoolean() && isBucketMasterToggle(bucket, option, title)) {
            return "Enabled";
        }
        return title;
    }

    private static boolean isBucketMasterToggle(FeatureBucket bucket, FabricConfigScreenModel.Option option, String title) {
        if (bucket.options().size() == 1 || title.equals(bucket.displayName())) {
            return true;
        }
        String sourceName = normalizedToggleSourceName(option.sourceKey());
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

    private static boolean isEnabledOption(FabricConfigScreenModel.Option option) {
        return option.settingName().equals("Enabled") || option.name().equals("Enabled");
    }

    private static int optionSortRank(FeatureBucket bucket, FabricConfigScreenModel.Option option) {
        String title = optionTitle(bucket, option);
        if (title.equals("Enabled") || isEnabledOption(option)) {
            return 0;
        }
        if (option.isBoolean() && isBucketMasterToggle(bucket, option, optionTitle(option))) {
            return 0;
        }
        return 1;
    }

    private static void setBoolean(FabricConfigScreenModel.Option option, boolean value) {
        if (option.booleanValue() != value) {
            option.toggleBoolean();
        }
    }

    private static void saveAll(List<FabricConfigScreenModel.Category> categories) {
        try {
            FabricConfigScreenModel.saveAll(categories);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage() == null ? "Failed to save config" : e.getMessage(), e);
        }
    }

    private static boolean isColorOption(FabricConfigScreenModel.Option option) {
        return switch (option.sourceKey()) {
            case "leafLitterTintGlobalMinColor", "leafLitterTintGlobalMaxColor", "mentionHighlightColorHex" -> true;
            default -> false;
        };
    }

    private static boolean colorAllowsAlpha(FabricConfigScreenModel.Option option) {
        return option.sourceKey().equals("mentionHighlightColorHex");
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
            // Fall back to black below so malformed config values do not crash the config screen.
        }
        return Color.BLACK;
    }

    private static String formatColor(Color color, boolean includeAlpha) {
        if (includeAlpha && color.getAlpha() < 255) {
            return "%02X%02X%02X%02X".formatted(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
        }
        return "%02X%02X%02X".formatted(color.getRed(), color.getGreen(), color.getBlue());
    }

    private static dev.isxander.yacl3.api.Option<?> createEnumOption(FabricConfigScreenModel.Option option, String title) {
        return switch (option.sourceKey()) {
            case "biomeTitlesSlot" -> createEnumOption(option, title, BiomeTitleSlot.class, BiomeTitleSlot.from(option.pendingText()));
            case "leafLitterTintSource" -> createEnumOption(option, title, LeafLitterTintSource.class, LeafLitterTintSource.from(option.pendingText()));
            case "mentionSoundSource" -> createEnumOption(option, title, MentionSoundSource.class, MentionSoundSource.from(option.pendingText()));
            case "waterloggingMode" -> createEnumOption(option, title, WaterloggingMode.class, WaterloggingMode.from(option.pendingText()));
            default -> null;
        };
    }

    private static <E extends Enum<E> & StringBackedEnum> dev.isxander.yacl3.api.Option<?> createEnumOption(
            FabricConfigScreenModel.Option option,
            String title,
            Class<E> enumClass,
            E current
    ) {
        dev.isxander.yacl3.api.Option.Builder<E> builder = dev.isxander.yacl3.api.Option.<E>createBuilder(enumClass)
                .name(Component.literal(title))
                .description(description(option))
                .binding(
                        current,
                        () -> fromString(enumClass, option.pendingText(), current),
                        value -> option.setPendingText(value.configValue())
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

    private static boolean isStringListOption(FabricConfigScreenModel.Option option) {
        if (!option.kind().equals("List") && !option.kind().equals("Set") && !isSingleStringListOption(option)) {
            return false;
        }
        return switch (option.sourceKey()) {
            case "biomeItemTintAllowedItems",
                 "biomeItemTintDisabledItems",
                 "biomeItemTintFoliageFallbackItems",
                 "biomeItemTintGrassFallbackItems",
                 "biomeTitlesDisabledBiomes",
                 "leafLitterTintBiomeRanges",
                 "mentionSoundEvent",
                 "mentionTriggerModes",
                 "pistonMoveBlacklistBlockIds",
                 "pistonMoveBlacklistNameContains",
                 "snowballNetherEntities",
                 "sourceIFrameBlacklist",
                 "fortuneBrushBlacklist",
                 "blockedLogMessages",
                 "waterloggingBlockIds" -> true;
            default -> false;
        };
    }

    private static boolean isSingleStringListOption(FabricConfigScreenModel.Option option) {
        return option.sourceKey().equals("mentionSoundEvent");
    }

    private static String firstOrBlank(List<String> values) {
        return values.isEmpty() ? "" : values.get(0);
    }

    private static List<String> parseStringList(String text) {
        List<String> values = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return values;
        }
        for (String line : text.lines().toList()) {
            for (String part : line.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isBlank()) {
                    values.add(trimmed);
                }
            }
        }
        return values;
    }

    private static boolean isIntegerSliderOption(FabricConfigScreenModel.Option option) {
        if (!option.kind().equals("Integer")) {
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

    private static boolean isDoubleSliderOption(FabricConfigScreenModel.Option option) {
        if (!option.kind().equals("Decimal")) {
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

    private static SliderSpec<Integer> integerSliderSpec(FabricConfigScreenModel.Option option) {
        String key = normalizedKey(option);
        if (key.contains("offset") || key.contains("position")) {
            return new SliderSpec<>(-256, 256, 1);
        }
        if (key.contains("size") || key.contains("count")) {
            return new SliderSpec<>(0, 64, 1);
        }
        return new SliderSpec<>(0, 100, 1);
    }

    private static SliderSpec<Double> doubleSliderSpec(FabricConfigScreenModel.Option option) {
        String key = normalizedKey(option);
        if (key.contains("tippedarrowlingeringclouds") && key.contains("radius")) {
            return new SliderSpec<>(0.5D, 32.0D, 0.5D);
        }
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

    private static String normalizedKey(FabricConfigScreenModel.Option option) {
        return (option.sourceKey() + " " + option.settingName()).toLowerCase(Locale.ROOT);
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

    private static String trimDouble(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
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
            return name();
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

    private record FeatureBucket(String displayName, String sourceGroup, List<FabricConfigScreenModel.Option> options) {
        FeatureBucket(String displayName, String sourceGroup) {
            this(displayName, sourceGroup, new ArrayList<>());
        }
    }
}
