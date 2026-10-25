package org.onenonly.bitsandbalance.fabric.config.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricCombatConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricConfigPaths;
import org.onenonly.bitsandbalance.fabric.config.FabricContentConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricEnchantingConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricJsonConfigFiles;
import org.onenonly.bitsandbalance.fabric.config.FabricLootConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricMobsConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricRecipesConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricWorldgenConfig;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class FabricConfigScreenModel {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONTENT_FILE = "bitsandbalance-content.json";
    private static final String DESCRIPTION_KEY = "description";
    private static final Map<Class<?>, List<DescriptionEntry>> DESCRIPTION_CACHE = new HashMap<>();

    private FabricConfigScreenModel() {
    }

    public static List<Category> categories() {
        List<Category> categories = new ArrayList<>();
        addCategory(categories, "Client",
                new ConfigGroup("", FabricClientConfig.class, false));
        addCategory(categories, "Gameplay",
                new ConfigGroup("Tweaks", FabricTweaksConfig.class, false),
                new ConfigGroup("Mechanics", FabricMechanicsConfig.class, false),
                new ConfigGroup("Balance", FabricBalanceConfig.class, false),
                new ConfigGroup("Combat", FabricCombatConfig.class, false),
                new ConfigGroup("Mobs", FabricMobsConfig.class, false));
        addCategory(categories, "Content",
                new ConfigGroup("Content", FabricContentConfig.class, true),
                new ConfigGroup("Slab-Family", FabricBuildingConfig.class, true),
                new ConfigGroup("Enchanting", FabricEnchantingConfig.class, true),
                new ConfigGroup("Recipes", FabricRecipesConfig.class, true),
                new ConfigGroup("Potions", FabricPotionConfig.class, true),
                new ConfigGroup("Loot", FabricLootConfig.class, true));
        addCategory(categories, "Worldgen",
                new ConfigGroup("", FabricWorldgenConfig.class, true));
        addDebugCategory(categories);
        return categories;
    }

    public static String auditDump() {
        StringBuilder report = new StringBuilder();
        for (Category category : categories()) {
            for (Group group : category.groups()) {
                for (Option option : group.options()) {
                    report.append(category.name())
                            .append(" > ")
                            .append(group.name().isBlank() ? category.name() : group.name())
                            .append(" > ")
                            .append(option.featureName());
                    if (!option.subFeatureName().isBlank()) {
                        report.append(" > ").append(option.subFeatureName());
                    }
                    report.append(" > ")
                            .append(option.settingName())
                            .append(" > ")
                            .append(option.description())
                            .append(System.lineSeparator());
                }
            }
        }
        return report.toString();
    }

    public static List<AuditFinding> auditFindings() {
        List<AuditFinding> findings = new ArrayList<>();
        Map<String, Set<String>> categoryGroupFeatures = new LinkedHashMap<>();

        for (Category category : categories()) {
            for (Group group : category.groups()) {
                String groupName = group.name().isBlank() ? category.name() : group.name();
                String featureBucket = category.name() + " > " + groupName;
                Set<String> featureNames = categoryGroupFeatures.computeIfAbsent(featureBucket, ignored -> new LinkedHashSet<>());

                for (Option option : group.options()) {
                    featureNames.add(option.featureName());
                    String path = auditPath(category, groupName, option);
                    auditText(findings, path, "feature", option.featureName());
                    auditText(findings, path, "subfeature", option.subFeatureName());
                    auditText(findings, path, "setting", option.settingName());
                    auditDescription(findings, path, option);
                }
            }
        }

        for (Map.Entry<String, Set<String>> entry : categoryGroupFeatures.entrySet()) {
            List<String> features = new ArrayList<>(entry.getValue());
            for (int i = 0; i < features.size(); i++) {
                String left = features.get(i);
                for (int j = i + 1; j < features.size(); j++) {
                    String right = features.get(j);
                    if (nearDuplicateFeatureName(left, right)) {
                        findings.add(new AuditFinding("near-duplicate-feature", entry.getKey(),
                                "Feature groups look split but related: '" + left + "' and '" + right + "'."));
                    }
                }
            }
        }

        return findings;
    }

    public static String auditReport() {
        List<AuditFinding> findings = auditFindings();
        StringBuilder report = new StringBuilder();
        report.append("Bits and Balance Mod Menu Config Audit").append(System.lineSeparator());
        report.append("Findings: ").append(findings.size()).append(System.lineSeparator());
        for (AuditFinding finding : findings) {
            report.append("- [")
                    .append(finding.type())
                    .append("] ")
                    .append(finding.path())
                    .append(": ")
                    .append(finding.message())
                    .append(System.lineSeparator());
        }
        report.append(System.lineSeparator()).append("Generated options:").append(System.lineSeparator());
        report.append(auditDump());
        return report.toString();
    }

    public static void assertAuditClean() {
        List<AuditFinding> findings = auditFindings();
        if (!findings.isEmpty()) {
            throw new IllegalStateException(auditReport());
        }
    }

    public static String featureDescription(String featureName, String sourceGroup) {
        return switch (featureName) {
            case "Alternative Crafting" -> "Configure optional replacement and convenience crafting recipes.";
            case "Ancient City Loot" -> "Configure Bits and Balance additions to ancient city loot chests.";
            case "Biome Titles" -> "Configure the title shown when entering a new biome.";
            case "Custom Brewing" -> "Configure Bits and Balance custom brewing recipe support.";
            case "Custom Loot Tables" -> "Configure Bits and Balance custom loot table additions.";
            case "Fortune Brush" -> "Fortune boosts brushing XP by 50% per level. At level III, common materials triple and rare finds cap at two. Rewards are datapack controlled.";
            case "Brushing Loot" -> "Improve ordinary archaeology finds while keeping each site's artifacts and Sniffer Eggs at their original odds.";
            case "Depth Scaling Enemies - General" -> "Configure the master toggle and dimension restriction for Depth Scaling Enemies.";
            case "Depth Scaling Enemies - Surface Tier" -> "Configure surface-tier Depth Scaling Enemies armor and weapon settings.";
            case "Depth Scaling Enemies - Mid-Depth Tier" -> "Configure mid-depth Depth Scaling Enemies threshold, armor, and weapon settings.";
            case "Depth Scaling Enemies - Deep Tier" -> "Configure deep-tier Depth Scaling Enemies threshold, armor, and weapon settings.";
            case "End City Loot" -> "Configure Bits and Balance additions to end city loot chests.";
            case "Ore Veins - Nether Gold" -> "Configure Nether gold vein world generation.";
            case "Ore Veins - Nether Quartz" -> "Configure Nether quartz vein world generation and raw quartz pockets.";
            case "Ore Veins - Raw Quartz" -> "Configure raw quartz block availability for vein-related world generation.";
            case "Ore Veins - Coal Andesite" -> "Configure shallow Overworld coal and andesite vein generation.";
            case "Ore Veins - Coal Tuff" -> "Configure deep Overworld coal and tuff vein generation.";
            case "Ore Veins - Debug" -> "Configure Ore Veins debug logging.";
            case "Extra Mining XP", "More Mining XP" -> "Configure bonus XP dropped by ores.";
            case "Ore Smelting XP" -> "Configure XP from smelting or blasting Silk Touched vanilla ore blocks.";
            case "Improved Phantoms" -> "Configure improved phantom behavior and related combat options.";
            case "Log Suppressions" -> "Configure client log and warning suppression options.";
            case "Raw Ore Smelting/Blasting" -> "Configure smelting and blasting recipes for raw metal blocks.";
            case "Second Chance" -> "Configure the anti-one-shot safety net for players.";
            case "Stair Recipes" -> "Configure stair recipe output overrides and compact stair recipes.";
            case "Purposes for Goat Horns" -> "Configure optional powers for each vanilla goat horn.";
            case "Waterlogging" -> "Configure automatic waterlogging behavior for placed blocks.";
            default -> "Configure " + featureName + " settings"
                    + (sourceGroup == null || sourceGroup.isBlank() || sourceGroup.equals(featureName) ? "." : " in " + sourceGroup + ".");
        };
    }

    private static String auditPath(Category category, String groupName, Option option) {
        StringBuilder path = new StringBuilder(category.name())
                .append(" > ")
                .append(groupName)
                .append(" > ")
                .append(option.featureName());
        if (!option.subFeatureName().isBlank()) {
            path.append(" > ").append(option.subFeatureName());
        }
        return path.append(" > ").append(option.settingName())
                .append(" (")
                .append(option.owner().getSimpleName())
                .append(".")
                .append(option.sourceKey())
                .append(")")
                .toString();
    }

    private static void auditText(List<AuditFinding> findings, String path, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.equals("automatic") || lower.equals("option") || lower.equals("boolean option")) {
            findings.add(new AuditFinding("weak-" + label, path, "Weak " + label + " label: '" + value + "'."));
        }
        if (value.contains("DSE") || value.contains("Dse")) {
            findings.add(new AuditFinding("abbreviation", path, "Use 'Depth Scaling Enemies' instead of '" + value + "'."));
        }
        if (value.contains("Level30")) {
            findings.add(new AuditFinding("missing-space", path, "Use 'Level 30' spacing instead of '" + value + "'."));
        }
    }

    private static void auditDescription(List<AuditFinding> findings, String path, Option option) {
        String description = option.description();
        if (description == null || description.isBlank()) {
            findings.add(new AuditFinding("missing-description", path, "Description is missing."));
            return;
        }

        if (isWeakDescription(description, option.kind())) {
            findings.add(new AuditFinding("weak-description", path, "Description is too generic: '" + description + "'."));
        }
        if (description.equals(option.featureName() + " settings.")
                || description.equals(option.settingName() + " settings.")
                || description.equals(option.name() + " settings.")) {
            findings.add(new AuditFinding("weak-description", path, "Description repeats the label instead of explaining behavior."));
        }
    }

    private static boolean nearDuplicateFeatureName(String left, String right) {
        String leftNorm = singularFeatureName(left);
        String rightNorm = singularFeatureName(right);
        return !left.equals(right) && !leftNorm.isBlank() && leftNorm.equals(rightNorm);
    }

    private static String singularFeatureName(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("\\bheads\\b", "head")
                .replaceAll("\\bslabs\\b", "slab")
                .replaceAll("\\bsteps\\b", "step")
                .replaceAll("\\bitems\\b", "item")
                .replaceAll("\\bblocks\\b", "block")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static void addCategory(List<Category> categories, String name, ConfigGroup... groups) {
        List<Group> modelGroups = new ArrayList<>();
        for (ConfigGroup group : groups) {
            List<Option> options = new ArrayList<>();
            collectOptions(group.owner(), group.owner(), group.label(), group.restartRecommended(), options);
            assignFeatureLabels(options);
            sortOptions(options);
            String label = group.label().isBlank() ? name : group.label();
            modelGroups.add(new Group(label, options));
        }
        categories.add(new Category(name, modelGroups));
    }

    private static void addDebugCategory(List<Category> categories) {
        List<Group> debugGroups = new ArrayList<>();
        for (Category category : categories) {
            List<Option> debugOptions = new ArrayList<>();
            for (Group group : category.groups()) {
                List<Option> remainingOptions = new ArrayList<>();
                for (Option option : group.options()) {
                    if (isDebugOption(option)) {
                        debugOptions.add(option);
                    } else {
                        remainingOptions.add(option);
                    }
                }
                group.options().clear();
                group.options().addAll(remainingOptions);
            }
            if (!debugOptions.isEmpty()) {
                sortOptions(debugOptions);
                debugGroups.add(new Group(category.name(), debugOptions));
            }
        }
        if (!debugGroups.isEmpty()) {
            categories.add(new Category("Debug", debugGroups));
        }
    }

    private static boolean isDebugOption(Option option) {
        String sourceKey = option.sourceKey().toLowerCase(Locale.ROOT);
        return sourceKey.contains("debug") || sourceKey.equals("logmissingids");
    }

    private static void sortOptions(List<Option> options) {
        options.sort(Comparator
                .comparing(Option::featureName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Option::subFeatureName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Option::settingName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Option::name, String.CASE_INSENSITIVE_ORDER));
    }

    private static void collectOptions(Class<?> owner, Class<?> type, String prefix, boolean restartRecommended, List<Option> options) {
        Field[] fields = type.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (!isEditableConfigField(field)) {
                continue;
            }
            if (isHiddenMirrorConfigField(owner, field.getName())) {
                continue;
            }

            FieldOption option = createOption(owner, field, prefix, restartRecommended, i);
            if (option != null) {
                options.add(option);
            }
        }

        Class<?>[] nestedTypes = type.getDeclaredClasses();
        for (Class<?> nestedType : nestedTypes) {
            if (!Modifier.isStatic(nestedType.getModifiers())) {
                continue;
            }
            collectOptions(owner, nestedType, combine(prefix, humanizeTypeName(nestedType.getSimpleName())), restartRecommended, options);
        }
    }

    private static boolean isEditableConfigField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers) && !field.isSynthetic();
    }

    private static boolean isHiddenMirrorConfigField(Class<?> owner, String fieldName) {
        if (owner != FabricTweaksConfig.class) {
            return false;
        }
        return switch (fieldName) {
            case "enableEnhancedSlabs",
                 "enhancedSlabsPlaceOnTop",
                 "enhancedSlabsHangBelow",
                 "enhancedSlabsKneeSlabMining",
                 "enhancedSlabsMixedDoubleSlabs",
                 "enhancedSlabsVerticalSlabs",
                 "enhancedSlabsSteps" -> true;
            default -> false;
        };
    }

    private static FieldOption createOption(Class<?> owner, Field field, String prefix, boolean restartRecommended, int order) {
        Class<?> type = field.getType();
        field.setAccessible(true);
        try {
            Object value = field.get(null);
            if (type == boolean.class || type == Boolean.class) {
                return createFieldOption(owner, field, prefix, "Boolean", value, restartRecommended, order);
            }
            if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
                return createFieldOption(owner, field, prefix, "Integer", value, restartRecommended, order);
            }
            if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
                return createFieldOption(owner, field, prefix, "Decimal", value, restartRecommended, order);
            }
            if (type == String.class) {
                return createFieldOption(owner, field, prefix, "Text", value, restartRecommended, order);
            }
            if (List.class.isAssignableFrom(type) && isStringCollectionField(field) && isStringCollection(value)) {
                return createFieldOption(owner, field, prefix, "List", copyStringList(value), restartRecommended, order);
            }
            if (Set.class.isAssignableFrom(type) && isStringCollectionField(field) && isStringCollection(value)) {
                return createFieldOption(owner, field, prefix, "Set", copyStringList(value), restartRecommended, order);
            }
            if (Map.class.isAssignableFrom(type) && isStringIntegerMap(value)) {
                return createFieldOption(owner, field, prefix, "Map", copyStringIntegerMap(value), restartRecommended, order);
            }
        } catch (IllegalAccessException ignored) {
            return null;
        }
        return null;
    }

    private static FieldOption createFieldOption(Class<?> owner, Field field, String prefix, String kind, Object value, boolean restartRecommended, int order) {
        String name = optionName(prefix, field.getName());
        String description = optionDescription(owner, field, prefix, name, kind, restartRecommended);
        return new FieldOption(owner, field, field.getName(), name, prefix, kind, description, value, restartRecommended, order);
    }

    private static boolean isStringCollection(Object value) {
        if (value == null) {
            return true;
        }
        if (!(value instanceof Collection<?> collection)) {
            return false;
        }
        for (Object element : collection) {
            if (!(element instanceof String)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isStringCollectionField(Field field) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            return false;
        }
        Type[] arguments = parameterizedType.getActualTypeArguments();
        return arguments.length == 1 && arguments[0] == String.class;
    }

    private static List<String> copyStringList(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return new ArrayList<>();
        }
        List<String> copy = new ArrayList<>();
        for (Object element : collection) {
            copy.add(Objects.toString(element, ""));
        }
        return copy;
    }

    private static boolean isStringIntegerMap(Object value) {
        if (value == null) {
            return true;
        }
        if (!(value instanceof Map<?, ?> map)) {
            return false;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof Number)) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Integer> copyStringIntegerMap(Object value) {
        Map<String, Integer> copy = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(Objects.toString(entry.getKey(), ""), ((Number) entry.getValue()).intValue());
            }
        }
        return copy;
    }

    public static void saveAll(List<Category> categories) throws IOException {
        Set<Class<?>> owners = new LinkedHashSet<>();
        for (Category category : categories) {
            for (Option option : category.options()) {
                option.applyPendingValue();
                owners.add(option.owner());
            }
        }

        for (Class<?> owner : owners) {
            save(owner);
        }

        DESCRIPTION_CACHE.clear();
        for (Class<?> owner : owners) {
            refreshRuntime(owner);
        }
    }

    private static void refreshRuntime(Class<?> owner) throws IOException {
        try {
            Method init = owner.getDeclaredMethod("init");
            init.setAccessible(true);
            init.invoke(null);
        } catch (NoSuchMethodException ignored) {
            // Some config owners are only used as write helpers and have no runtime mirrors.
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw io(e);
        }
    }

    private static void save(Class<?> owner) throws IOException {
        try {
            Method save = owner.getDeclaredMethod("save");
            save.setAccessible(true);
            save.invoke(null);
            return;
        } catch (NoSuchMethodException ignored) {
            // Fall through to the normalized JSON writers used by most Fabric config classes.
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw io(e);
        }

        if (owner == FabricBuildingConfig.class) {
            saveBuildingConfig();
            return;
        }

        Path configPath = FabricConfigPaths.resolve(fileName(owner));
        try {
            Method writer = owner.getDeclaredMethod("writeDefaults", Path.class);
            writer.setAccessible(true);
            writer.invoke(null, configPath);
            return;
        } catch (NoSuchMethodException ignored) {
            // Try the few config writers that also accept a mutable root object.
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw io(e);
        }

        try {
            Method writer = owner.getDeclaredMethod("writeDefaults", Path.class, JsonObject.class);
            writer.setAccessible(true);
            JsonObject root = FabricJsonConfigFiles.readRoot(configPath, GSON);
            writer.invoke(null, configPath, root);
        } catch (NoSuchMethodException e) {
            throw new IOException("No config save method found for " + owner.getSimpleName(), e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw io(e);
        }
    }

    private static String fileName(Class<?> owner) throws IOException {
        try {
            Field fileName = owner.getDeclaredField("FILE_NAME");
            fileName.setAccessible(true);
            return (String) fileName.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IOException("No config file name found for " + owner.getSimpleName(), e);
        }
    }

    private static void saveBuildingConfig() throws IOException {
        Path configPath = FabricConfigPaths.resolve(CONTENT_FILE);
        JsonObject root = FabricJsonConfigFiles.readRoot(configPath, GSON);
        JsonObject enhancedSlabs = object(root, "enhancedSlabs");
        enhancedSlabs.addProperty("enabled", FabricBuildingConfig.enableEnhancedSlabs);
        enhancedSlabs.addProperty("placeOnTop", FabricBuildingConfig.enhancedSlabsPlaceOnTop);
        enhancedSlabs.addProperty("hangBelow", FabricBuildingConfig.enhancedSlabsHangBelow);
        enhancedSlabs.addProperty("kneeSlabMining", FabricBuildingConfig.enhancedSlabsKneeSlabMining);
        enhancedSlabs.addProperty("mixedDoubleSlabs", FabricBuildingConfig.enhancedSlabsMixedDoubleSlabs);
        object(root, "verticalSlabs").addProperty("enabled", FabricBuildingConfig.enhancedSlabsVerticalSlabs);
        object(root, "stepsAndVerticalSteps").addProperty("enabled", FabricBuildingConfig.enhancedSlabsSteps);
        root.remove("building");
        FabricBuildingConfig.syncRuntimeFlags();
        FabricJsonConfigFiles.writeRoot(configPath, GSON, root);
    }

    private static JsonObject object(JsonObject root, String key) {
        JsonObject object = root.getAsJsonObject(key);
        if (object == null) {
            object = new JsonObject();
            root.add(key, object);
        }
        return object;
    }

    private static IOException io(Exception e) {
        Throwable cause = e instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause()
                : e;
        if (cause instanceof IOException io) {
            return io;
        }
        return new IOException(cause);
    }

    private static String optionDescription(Class<?> owner, Field field, String prefix, String optionName, String kind, boolean restartRecommended) {
        String description = sourceKeyDescription(owner, field.getName());
        if (description.isBlank()) {
            description = configDescription(owner, field, prefix, optionName);
        }
        if (isWeakDescription(description, kind)) {
            description = fallbackDescription(field.getName(), optionName);
        }
        if (restartRecommended) {
            description += " Restart the game or server, then re-open the world, to fully apply this setting.";
        }
        return description;
    }

    private static boolean isWeakDescription(String description, String kind) {
        if (description == null || description.isBlank()) {
            return true;
        }
        String normalized = description.trim().toLowerCase(Locale.ROOT);
        String withoutApplyAdvice = normalized
                .replace(" restart the game or server, then re-open the world, to fully apply this setting.", "")
                .trim();
        return withoutApplyAdvice.equals(kind.toLowerCase(Locale.ROOT) + " option.")
                || normalized.equals("boolean option.")
                || withoutApplyAdvice.endsWith(" settings.")
                || withoutApplyAdvice.endsWith(" setting.");
    }

    private static String fallbackDescription(String sourceKey, String optionName) {
        if (sourceKey.startsWith("enable") && sourceKey.length() > "enable".length()) {
            return "Enable or disable " + lowerSentence(humanize(sourceKey.substring("enable".length()))) + ".";
        }
        if (sourceKey.startsWith("allow") && sourceKey.length() > "allow".length()) {
            return "Allow " + lowerSentence(humanize(sourceKey.substring("allow".length()))) + ".";
        }
        if (sourceKey.startsWith("disable") && sourceKey.length() > "disable".length()) {
            return "Disable " + lowerSentence(humanize(sourceKey.substring("disable".length()))) + ".";
        }
        return "Configure " + lowerSentence(optionName) + ".";
    }

    private static String lowerSentence(String value) {
        if (value == null || value.isBlank()) {
            return "this option";
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static String sourceKeyDescription(Class<?> owner, String sourceKey) {
        if (owner == FabricMechanicsConfig.class) {
            return switch (sourceKey) {
                case "enableCrawlingMechanic" -> "Enable the crawl keybind and land-crawling pose for moving through one-block spaces.";
                case "enableToggleStance" -> "Enable the Toggle Stance keybind for cycling between standing, sneaking, and crawling.";
                case "enableCoyoteTimeJump" -> "Allow a short grace-period jump just after walking off an edge.";
                case "coyoteTimeDelayMs" -> "Delay in milliseconds after leaving ground before coyote-time jumping becomes available.";
                case "coyoteTimeWindowMs" -> "Maximum grace period in milliseconds for jumping after leaving ground.";
                case "coyoteTimeDebug" -> "Log coyote-time jump state for debugging.";
                case "enableDoorKnocking" -> "Enable the knock keybind while looking at doors to play a knock sound and alert nearby players.";
                case "enableRapidFireJump" -> "Reduce jump delay so repeated jumps can fire quickly while pressing or holding jump.";
                case "rapidFireJumpDebug" -> "Log rapid-fire jump state for debugging.";
                case "enableCozyCampfire" -> "Allow campfires to periodically grant the configured cozy effect to nearby entities.";
                case "enableChatMentions" -> "Highlight chat mentions and optionally play a notification sound when your name is mentioned.";
                case "mentionInputHighlightUsernames" -> "Highlight matching usernames while composing chat input.";
                case "enableNavigatorCompass" -> "Enable extra compass HUD and tooltip readouts, including optional coordinates and distance.";
                case "enableWaterlogging" -> "Automatically waterlog waterloggable blocks placed into water.";
                case "enableGoatHornPurposes" -> "Master switch for all optional goat horn powers; vanilla sounds still work.";
                case "goatHornSeparateCooldowns" -> "Give each goat horn its own cooldown instead of sharing vanilla's goat-horn cooldown.";
                case "goatHornDream" -> "Enable Dream's ten-second ghost projection with a 60-second cooldown.";
                case "goatHornYearn" -> "Follow a moving crit-particle guide to the center of a large cave opening.";
                case "goatHornPonder" -> "Suggest up to five dark places to put lights, with spacing based on your carried light source.";
                case "goatHornSing", "goatHornSeek", "goatHornFeel",
                     "goatHornAdmire", "goatHornCall" -> "Enable this horn's optional power when one is added.";
                case "leashedTeleportDebug" -> "Log leashed teleport state for debugging.";
                case "waterloggingDoors" -> "Allow doors to be automatically waterlogged per block half.";
                case "waterloggingFenceGates" -> "Allow fence gates to be automatically waterlogged.";
                case "waterloggingPressurePlates" -> "Allow pressure plates, including weighted pressure plates, to be automatically waterlogged.";
                case "waterloggingWorldgen" -> "Allow automatic waterlogging during world generation or structure placement. Disabled by default; still requires water at the placed block.";
                case "waterloggingMode" -> "Choose BLACKLIST to affect all blocks except listed ids, or WHITELIST to affect only listed ids.";
                case "waterloggingBlockIds" -> "Block ids used as the Waterlogging blacklist or whitelist. Examples: minecraft:oak_fence, minecraft:oak_door.";
                default -> "";
            };
        }
        if (owner == FabricClientConfig.class) {
            return switch (sourceKey) {
                case "blockedLogMessages" -> "Case-sensitive text fragments to hide at any log level. Enter one fragment or a JSON array of quoted fragments. Commas inside a fragment are preserved. Defaults include far-chunk errors and removed-recipe warnings. This list is the only control for message filtering. Remove an entry to stop blocking it; [] disables all message filtering. Blank entries are ignored.";
                case "disableGuardianJumpscare" -> "Suppress the Elder Guardian Mining Fatigue appearance effect.";
                case "playGuardianJumpscareSound" -> "Play the normal Elder Guardian Mining Fatigue sound while the appearance effect is suppressed.";
                case "enableLevel30OldSound" -> "Use the classic level 30 experience sound when reaching level 30.";
                case "enableChatHeads" -> "Show player heads beside chat messages.";
                case "chatHeadSize" -> "Size of player heads in chat, measured in pixels.";
                case "chatHeadOffset" -> "Horizontal pixel offset for player heads in chat.";
                case "chatHeadOffsetY" -> "Vertical pixel offset for player heads in chat.";
                case "chatHeadsShadow" -> "Render chat heads with a subtle shaded depth effect.";
                case "biomeTitlesSuppressInStructures" -> "Hide biome titles while the server reports that you are inside a generated structure.";
                case "biomeTitlesDisabledBiomes" -> "Biome title blacklist. Use biome ids or tags that should never show titles. Examples: minecraft:river, #minecraft:is_ocean.";
                case "biomeItemTintAllowedItems" -> "Optional whitelist of extra biome-sensitive item IDs that auto-detect misses. Examples: minecraft:grass_block, examplemod:blue_maple_leaves.";
                case "biomeItemTintDisabledItems" -> "Blacklist of block/item IDs that should never be biome-tinted. Examples: minecraft:dead_bush, minecraft:sugar_cane.";
                case "biomeItemTintGrassFallbackItems" -> "Item IDs that should use biome grass color when the real block color provider gives no tint. Example: examplemod:clover.";
                case "biomeItemTintFoliageFallbackItems" -> "Item IDs that should use biome foliage color when the real block color provider gives no tint. Example: wilderwild:baobab_leaves.";
                case "usageTickerEnabled" -> "Show the usage ticker HUD element.";
                case "usageTickerShowArmorSlots" -> "Render armor slots in the usage ticker overlay.";
                case "usageTickerInvert" -> "Invert the usage ticker direction and layout.";
                case "usageTickerOffsetX" -> "Horizontal pixel offset for the usage ticker.";
                case "arrowPriorityOffsetX" -> "Move the Arrow Priority selector left or right from its default position below the crosshair, in GUI pixels.";
                case "arrowPriorityShowArrowSlot" -> "Show the next projectile beside the hotbar while holding a bow or crossbow with a saved Arrow Priority choice.";
                case "arrowPriorityOffsetY" -> "Move the Arrow Priority selector up or down from its default position below the crosshair, in GUI pixels.";
                case "usageTickerOffsetY" -> "Vertical pixel offset for the usage ticker.";
                case "usageTickerDebug" -> "Log usage ticker debug information.";
                default -> "";
            };
        }
        if (owner == FabricTweaksConfig.class) {
            return switch (sourceKey) {
                case "enableAutomaticToolRestock" -> "Automatically replace broken hotbar tools from matching tools in your inventory.";
                case "enableAutomaticBlockRestock" -> "Automatically refill depleted hotbar building blocks from matching blocks in your inventory.";
                case "enableItemSharing" -> "Allow players to share the item they are holding in chat.";
                case "itemShareCooldownSeconds" -> "Minimum delay between item-share messages per player, in seconds.";
                case "enableMoreMiningXp" -> "Increase XP drops from mining vanilla and variant ores.";
                case "enableUnbreakableTrialSpawners" -> "Make trial spawners unbreakable.";
                case "enableUnbreakableVaults" -> "Make vaults unbreakable.";
                case "despawnVexWithEvoker" -> "Despawn vexes when their owning evoker despawns or dies.";
                case "despawnBulletsWithShulker" -> "Despawn shulker bullets when their owning shulker despawns or dies.";
                case "enableTippedArrowLingeringClouds" -> "Allow tipped arrows to create lingering effect clouds when they hit.";
                case "tippedArrowLingeringCloudsMobsEnabled" -> "Allow tipped arrows fired by mobs to create lingering clouds.";
                case "tippedArrowLingeringCloudsPlayerRadius" -> "Starting radius for lingering clouds from player-fired and non-mob tipped arrows.";
                case "tippedArrowLingeringCloudsMobRadius" -> "Starting radius for lingering clouds from mob-fired tipped arrows.";
                case "tippedArrowLingeringCloudsDisabledMobIds" -> "Mob entity ids whose fired tipped arrows should not create lingering clouds. Examples: minecraft:skeleton, minecraft:stray.";
                default -> "";
            };
        }
        if (owner == FabricBalanceConfig.class) {
            return switch (sourceKey) {
                case "dseEnabled" -> "Enable depth-based equipment scaling for hostile mob spawns.";
                case "dseOverworldOnly" -> "Limit depth scaling enemies to the Overworld.";
                case "dseEnableSurfaceTier" -> "Enable the surface spawn tier for depth scaling enemies.";
                case "dseEnableMidTier" -> "Enable the mid-depth spawn tier for depth scaling enemies.";
                case "dseEnableDeepTier" -> "Enable the deep spawn tier for depth scaling enemies.";
                default -> "";
            };
        }
        if (owner == FabricCombatConfig.class) {
            return switch (sourceKey) {
                case "enableSecondChance" -> "Enable the Second Chance anti-one-shot mechanic for players.";
                case "secondChanceExcludeFallDamage" -> "Prevent fall damage from triggering Second Chance.";
                case "secondChanceHealthThresholdHearts" -> "Player must have more than this many hearts before a lethal hit can trigger Second Chance.";
                case "secondChanceCooldownDays" -> "Cooldown in Minecraft days between Second Chance triggers. Set to 0 to disable the cooldown.";
                case "secondChanceResetOnRespawn" -> "Reset the Second Chance cooldown after the player respawns.";
                case "secondChanceToastEnabled" -> "Show an advancement-style toast when Second Chance saves the player.";
                case "secondChanceResistanceEnabled" -> "Apply Resistance after Second Chance triggers.";
                case "secondChanceResistanceLevel" -> "Resistance effect level to apply after Second Chance triggers.";
                case "secondChanceNauseaEnabled" -> "Apply Nausea after Second Chance triggers.";
                case "secondChanceNauseaLevel" -> "Nausea effect level to apply after Second Chance triggers.";
                default -> "";
            };
        }
        if (owner == FabricContentConfig.class) {
            return switch (sourceKey) {
                case "enableGlowGoo" -> "Enable Glow Goo, a throwable item that creates bright waterloggable splatter and can apply Bioluminescence.";
                case "glowGooSplatterDecayEnabled" -> "Allow newly placed Glow Goo Splatter to disappear after the configured lifetime. Disabled by default.";
                case "glowGooSplatterDecayTimeSeconds" -> "Lifetime of newly placed Glow Goo Splatter when decay is enabled, in seconds.";
                default -> "";
            };
        }
        if (owner == FabricWorldgenConfig.class) {
            return switch (sourceKey) {
                case "disabledKeys" -> "Worldgen blacklist of resource keys to forcibly disable. Examples: bitsandbalance:nether_gold_vein, bitsandbalance:nether_quartz_vein.";
                default -> "";
            };
        }
        if (owner == FabricBuildingConfig.class) {
            return switch (sourceKey) {
                case "enableEnhancedSlabs" -> "Master toggle for slab-family interaction behavior.";
                case "enhancedSlabsPlaceOnTop" -> "Legacy top-placement behavior toggle kept for compatibility.";
                case "enhancedSlabsHangBelow" -> "Allow lanterns, chains, and signs to hang below supported slabs and steps.";
                case "enhancedSlabsKneeSlabMining" -> "Allow targeted half-mining for supported slab-family blocks.";
                case "enhancedSlabsMixedDoubleSlabs" -> "Allow different slab types to combine into mixed double slabs.";
                case "enhancedSlabsVerticalSlabs" -> "Enable generated vertical slab blocks, recipes, placement support, and creative-tab entries.";
                case "enhancedSlabsSteps" -> "Enable generated step and vertical-step blocks, recipes, placement support, and creative-tab entries.";
                default -> "";
            };
        }
        if (owner == FabricMobsConfig.class) {
            return switch (sourceKey) {
                case "enableImprovedPhantoms" -> "Enable improved phantom behavior, including carrying, drop, damage, and slowness options.";
                case "enablePhantomPickup" -> "Allow phantoms to pick up and carry players when attacking.";
                case "enablePhantomSlownessStacking" -> "Enable stacking Slowness effects when players are hit by phantoms.";
                case "enableCaveSpidersInCaves" -> "Allow cave spiders to replace normal spiders in caves using the configured chance.";
                default -> "";
            };
        }
        if (owner == FabricEnchantingConfig.class && sourceKey.equals("fortuneBrushBlacklist"))
            return "Item IDs excluded from Fortune brushing quantity bonuses, e.g. minecraft:diamond. Keeps the normal reward and XP bonus. Applies to new reveals; already revealed rewards keep their saved count.";
        if (owner == FabricEnchantingConfig.class && sourceKey.equals("enableFortuneBrush"))
            return "Enable Fortune brushing drop and XP bonuses. Fortune remains applicable through an anvil when bonuses are disabled. Reward profiles are editable through datapacks.";
        if (owner == FabricLootConfig.class && (sourceKey.startsWith("brushing") || sourceKey.equals("enableBrushingLoot"))) {
            return "Enable improved ordinary brushing finds while preserving artifact and Sniffer Egg odds. Also requires Custom Loot Tables.";
        }
        if (owner == FabricPotionConfig.class) {
            return switch (sourceKey) {
                case "enableWitheringPotion" -> "Enable the Withering potion family, including brewing recipes and potion variants.";
                case "enableResurfacingPotion" -> "Enable the Resurfacing potion family, including brewing recipes and potion variants.";
                case "enableDisplacementPotion" -> "Enable the Displacement potion family, including brewing recipes and potion variants.";
                case "enableReturningPotion" -> "Enable the Returning potion family, including brewing recipes and potion variants.";
                case "enableLevitationPotion" -> "Enable the Levitation potion family, including brewing recipes and potion variants.";
                case "enableHastePotion" -> "Enable the Haste potion family, including brewing recipes and potion variants.";
                case "enableBlindnessPotion" -> "Brew Blindness with Awkward Potion + Ink Sac. Blinded mobs can only see and target entities within 3 blocks.";
                case "enableGlowingPotion" -> "Enable the Glowing potion family, including brewing recipes and potion variants.";
                case "enableBioluminescencePotion" -> "Enable the Bioluminescence potion family, including brewing recipes and potion variants.";
                default -> "";
            };
        }
        if (owner == FabricRecipesConfig.class) {
            return switch (sourceKey) {
                case "enableAlternativeRepeaterRecipe" -> "Enable the alternate Redstone Repeater recipe using sticks and stone.";
                case "enableChestFromLogsRecipe" -> "Enable crafting 4 chests directly from logs.";
                case "enableMapInkSacRecipe" -> "Enable the alternate Map recipe using an ink sac instead of a compass.";
                case "enableRecoveryCompassRecipe" -> "Enable the Recovery Compass recipe using echo shards and a compass.";
                case "enableEnderEyeRecipe" -> "Enable the Ender Eye recipe using an echo shard, wind charge, blaze powder, and ender pearl.";
                case "enableEnchantingTableDiamondBlocksRecipe" -> "Replace the Enchanting Table recipe diamonds with diamond blocks.";
                case "enableRawIronSmeltingRecipe" -> "Enable smelting Raw Iron Blocks into Iron Blocks.";
                case "enableRawGoldSmeltingRecipe" -> "Enable smelting Raw Gold Blocks into Gold Blocks.";
                case "enableRawCopperSmeltingRecipe" -> "Enable smelting Raw Copper Blocks into Copper Blocks.";
                case "enableRawIronBlastingRecipe" -> "Enable blasting Raw Iron Blocks into Iron Blocks.";
                case "enableRawGoldBlastingRecipe" -> "Enable blasting Raw Gold Blocks into Gold Blocks.";
                case "enableRawCopperBlastingRecipe" -> "Enable blasting Raw Copper Blocks into Copper Blocks.";
                case "enableOreSmeltingXp" -> "Enable configurable experience rewards for smelting or blasting vanilla ore blocks.";
                case "coalOreSmeltingXp" -> "XP awarded for Coal Ore and Deepslate Coal Ore.";
                case "lapisOreSmeltingXp" -> "XP awarded for Lapis Ore and Deepslate Lapis Ore.";
                case "netherQuartzOreSmeltingXp" -> "XP awarded for Nether Quartz Ore.";
                case "ironOreSmeltingXp" -> "XP awarded for Iron Ore and Deepslate Iron Ore.";
                case "copperOreSmeltingXp" -> "XP awarded for Copper Ore and Deepslate Copper Ore.";
                case "redstoneOreSmeltingXp" -> "XP awarded for Redstone Ore and Deepslate Redstone Ore.";
                case "goldOreSmeltingXp" -> "XP awarded for Gold Ore and Deepslate Gold Ore.";
                case "netherGoldOreSmeltingXp" -> "XP awarded for Nether Gold Ore.";
                case "diamondOreSmeltingXp" -> "XP awarded for Diamond Ore and Deepslate Diamond Ore.";
                case "emeraldOreSmeltingXp" -> "XP awarded for Emerald Ore and Deepslate Emerald Ore.";
                case "ancientDebrisSmeltingXp" -> "XP awarded for Ancient Debris.";
                case "enableStairRecipeOverride" -> "Override all stair recipes, vanilla, modded, and custom, to output 8 stairs instead of 4.";
                case "enableCompactStairRecipe" -> "Enable compact 2x2 stair recipes for all stair types.";
                default -> "";
            };
        }
        return "";
    }

    private static String configDescription(Class<?> owner, Field field, String prefix, String optionName) {
        List<DescriptionEntry> entries = descriptions(owner);
        if (entries.isEmpty()) {
            return "";
        }

        String fieldName = field.getName();
        String fieldNorm = normalizeSearch(fieldName);
        String optionNorm = normalizeSearch(optionName);
        String prefixNorm = normalizeSearch(prefix);
        Set<String> allowedRoots = allowedDescriptionRoots(owner);
        DescriptionEntry best = null;
        int bestScore = 0;

        for (DescriptionEntry entry : entries) {
            if (!allowedRoots.isEmpty() && !allowedRoots.contains(descriptionRoot(entry))) {
                continue;
            }
            String keyNorm = normalizeSearch(entry.key());
            String pathNorm = normalizeSearch(entry.path());
            String pathWithoutKeyNorm = normalizeSearch(entry.parentPath());
            boolean enabledKey = entry.key().equals("enabled") || keyNorm.isBlank();
            int score = 0;

            if (entry.key().equals(fieldName) || entry.path().endsWith("." + fieldName)) {
                score = 120;
            } else if (pathNorm.equals(optionNorm)) {
                score = 110;
            } else if (enabledKey && !pathWithoutKeyNorm.isBlank() && fieldNorm.contains(pathWithoutKeyNorm)) {
                score = 105;
            } else if (!enabledKey && fieldNorm.endsWith(keyNorm)) {
                score = 90;
            } else if (!enabledKey && fieldNorm.contains(keyNorm)) {
                score = 80;
            } else if (pathNorm.contains(fieldNorm) || fieldNorm.contains(pathNorm)) {
                score = 65;
            }

            if (score > 0 && !prefixNorm.isBlank() && !pathNorm.contains(prefixNorm)) {
                score -= 8;
            }
            if (score > bestScore) {
                bestScore = score;
                best = entry;
            }
        }

        return bestScore >= 55 && best != null ? best.description() : "";
    }

    private static Set<String> allowedDescriptionRoots(Class<?> owner) {
        if (owner == FabricRecipesConfig.class) {
            return Set.of("alternativeCrafting", "rawBlockSmelting", "rawBlockBlasting", "stairRecipes");
        }
        if (owner == FabricPotionConfig.class) {
            return Set.of("potions");
        }
        if (owner == FabricLootConfig.class) {
            return Set.of("lootTables");
        }
        if (owner == FabricBuildingConfig.class) {
            return Set.of("enhancedSlabs", "verticalSlabs", "stepsAndVerticalSteps");
        }
        if (owner == FabricContentConfig.class) {
            return Set.of("glowGoo", "rawQuartzBlock", "dogMusicDisc");
        }
        if (owner == FabricEnchantingConfig.class) {
            return Set.of("aerodynamic", "cascading");
        }
        return Set.of();
    }

    private static String descriptionRoot(DescriptionEntry entry) {
        String path = entry.path();
        int dot = path.indexOf('.');
        return dot >= 0 ? path.substring(0, dot) : path;
    }

    private static List<DescriptionEntry> descriptions(Class<?> owner) {
        if (DESCRIPTION_CACHE.containsKey(owner)) {
            return DESCRIPTION_CACHE.get(owner);
        }

        List<DescriptionEntry> entries = new ArrayList<>();
        try {
            JsonObject root = FabricJsonConfigFiles.readRoot(FabricConfigPaths.resolve(fileName(owner)), GSON);
            collectDescriptions(root, "", entries);
        } catch (IOException | RuntimeException ignored) {
            // Missing or invalid config descriptions should never prevent the screen from opening.
        }
        DESCRIPTION_CACHE.put(owner, entries);
        return entries;
    }

    private static void collectDescriptions(JsonObject object, String path, List<DescriptionEntry> entries) {
        JsonElement descriptions = object.get(DESCRIPTION_KEY);
        if (descriptions instanceof JsonObject descriptionObject) {
            for (Map.Entry<String, JsonElement> entry : descriptionObject.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    String key = entry.getKey();
                    String entryPath = path.isBlank() ? key : path + "." + key;
                    entries.add(new DescriptionEntry(entryPath, key, path, entry.getValue().getAsString()));
                }
            }
        }

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (DESCRIPTION_KEY.equals(entry.getKey()) || !(entry.getValue() instanceof JsonObject child)) {
                continue;
            }
            String childPath = path.isBlank() ? entry.getKey() : path + "." + entry.getKey();
            collectDescriptions(child, childPath, entries);
        }
    }

    private static String optionName(String prefix, String fieldName) {
        return humanizeFieldName(fieldName);
    }

    private static void assignFeatureLabels(List<Option> options) {
        Map<String, String> enabledFeatureStems = enabledFeatureStems(options);
        if (!enabledFeatureStems.isEmpty()) {
            for (Option option : options) {
                String stem = matchingEnabledFeatureStem(option.sourceKey(), enabledFeatureStems);
                if (!stem.isBlank()) {
                    String featureName = enabledFeatureStems.get(stem);
                    option.setFeatureLabels(featureName, settingNameForFeatureStem(option.sourceKey(), stem, featureName));
                    continue;
                }
                option.setFeatureLabels(option.name(), option.name());
            }
        }

        Map<String, Integer> prefixCounts = new HashMap<>();
        for (Option option : options) {
            if (!option.featureName().equals(option.name())) {
                continue;
            }
            List<String> words = words(option.name());
            int maxPrefixLength = Math.min(3, Math.max(0, words.size() - 1));
            for (int length = 1; length <= maxPrefixLength; length++) {
                String prefix = String.join(" ", words.subList(0, length));
                prefixCounts.merge(prefix, 1, Integer::sum);
            }
        }

        Map<String, Integer> assignedCounts = new HashMap<>();
        for (Option option : options) {
            if (!option.featureName().equals(option.name())) {
                assignedCounts.merge(option.featureName(), 1, Integer::sum);
                continue;
            }
            String feature = bestFeaturePrefix(option.name(), prefixCounts);
            if (feature.isBlank()) {
                feature = option.name();
            }
            assignedCounts.merge(feature, 1, Integer::sum);
        }

        for (Option option : options) {
            if (!option.featureName().equals(option.name())) {
                continue;
            }
            String feature = bestFeaturePrefix(option.name(), prefixCounts);
            if (feature.isBlank() || assignedCounts.getOrDefault(feature, 0) <= 1) {
                option.setFeatureLabels(option.name(), option.name());
                continue;
            }

            String setting = option.name().length() > feature.length()
                    ? option.name().substring(feature.length()).trim()
                    : "Enabled";
            if (setting.isBlank()) {
                setting = "Enabled";
            }
            option.setFeatureLabels(feature, setting);
        }

        assignWorldgenFeatureLabels(options);
        assignFeatureLabelOverrides(options);
        normalizeEnabledFeatureTitles(options);
    }

    private static void assignFeatureLabelOverrides(List<Option> options) {
        for (Option option : options) {
            if (option.owner() == FabricClientConfig.class) {
                assignClientFeatureLabel(option);
            } else if (option.owner() == FabricTweaksConfig.class) {
                assignTweaksFeatureLabel(option);
            } else if (option.owner() == FabricBalanceConfig.class) {
                assignBalanceFeatureLabel(option);
            } else if (option.owner() == FabricMechanicsConfig.class) {
                assignMechanicsFeatureLabel(option);
            } else if (option.owner() == FabricMobsConfig.class) {
                assignMobsFeatureLabel(option);
            } else if (option.owner() == FabricRecipesConfig.class) {
                assignRecipesFeatureLabel(option);
            } else if (option.owner() == FabricCombatConfig.class) {
                assignCombatFeatureLabel(option);
            } else if (option.owner() == FabricPotionConfig.class) {
                assignPotionFeatureLabel(option);
            } else if (option.owner() == FabricLootConfig.class) {
                assignLootFeatureLabel(option);
            } else if (option.owner() == FabricEnchantingConfig.class) {
                assignEnchantingFeatureLabel(option);
            }
        }
    }

    private static void assignClientFeatureLabel(Option option) {
        switch (option.sourceKey()) {
            case "disableGuardianJumpscare" -> option.setFeatureLabels("Guardian Jumpscare", "Disable Appearance");
            case "playGuardianJumpscareSound" -> option.setFeatureLabels("Guardian Jumpscare", "Play Sound");
            case "enableLevel30OldSound" -> option.setFeatureLabels("Level 30 Old Sound", "Level 30 Old Sound");
            case "biomeTitlesEnabled" -> option.setFeatureLabels("Biome Titles", "Enabled");
            case "biomeTitlesRecentSize" -> option.setFeatureLabels("Biome Titles", "Recent Size");
            case "biomeTitlesShowTicks" -> option.setFeatureLabels("Biome Titles", "Show Duration");
            case "biomeTitlesFadeTicks" -> option.setFeatureLabels("Biome Titles", "Fade Duration");
            case "biomeTitlesScale" -> option.setFeatureLabels("Biome Titles", "Scale");
            case "biomeTitlesOffsetX" -> option.setFeatureLabels("Biome Titles", "Horizontal Offset");
            case "biomeTitlesOffsetY" -> option.setFeatureLabels("Biome Titles", "Vertical Offset");
            case "biomeTitlesShadow" -> option.setFeatureLabels("Biome Titles", "Text Shadow");
            case "biomeTitlesSuppressInStructures" -> option.setFeatureLabels("Biome Titles", "Suppress In Structures");
            case "biomeTitlesSlot" -> option.setFeatureLabels("Biome Titles", "Display Slot");
            case "biomeTitlesDisabledBiomes" -> option.setFeatureLabels("Biome Titles", "Blacklist");
            case "biomeItemTintAllowedItems" -> option.setFeatureLabels("Biome Item Tint", "Whitelist");
            case "biomeItemTintDisabledItems" -> option.setFeatureLabels("Biome Item Tint", "Blacklist");
            case "biomeItemTintGrassFallbackItems" -> option.setFeatureLabels("Biome Item Tint", "Grass Fallbacks");
            case "biomeItemTintFoliageFallbackItems" -> option.setFeatureLabels("Biome Item Tint", "Foliage Fallbacks");
            case "enableChatHeads" -> option.setFeatureLabels("Chat Heads", "Enabled");
            case "chatHeadSize" -> option.setFeatureLabels("Chat Heads", "Size");
            case "chatHeadOffset" -> option.setFeatureLabels("Chat Heads", "Horizontal Offset");
            case "chatHeadOffsetY" -> option.setFeatureLabels("Chat Heads", "Vertical Offset");
            case "chatHeadsShadow" -> option.setFeatureLabels("Chat Heads", "Text Shadow");
            case "usageTickerEnabled" -> option.setFeatureLabels("Usage Ticker", "Enabled");
            case "usageTickerShowArmorSlots" -> option.setFeatureLabels("Usage Ticker", "Show Armor Slots");
            case "usageTickerInvert" -> option.setFeatureLabels("Usage Ticker", "Invert Direction");
            case "usageTickerOffsetX" -> option.setFeatureLabels("Usage Ticker", "Horizontal Offset");
            case "arrowPriorityOffsetX" -> option.setFeatureLabels("Arrow Priority", "Horizontal Offset");
            case "arrowPriorityShowArrowSlot" -> option.setFeatureLabels("Arrow Priority", "Show Arrow Slot");
            case "arrowPriorityOffsetY" -> option.setFeatureLabels("Arrow Priority", "Vertical Offset");
            case "usageTickerOffsetY" -> option.setFeatureLabels("Usage Ticker", "Vertical Offset");
            case "usageTickerDebug" -> option.setFeatureLabels("Usage Ticker", "Debug Logging");
            case "suppressExperimentalSettingsWarning" -> option.setFeatureLabels("Log Suppressions", "Experimental Settings Warning");
            case "blockedLogMessages" -> option.setFeatureLabels("Log Suppressions", "Blocked Messages");
            default -> {
            }
        }
    }

    private static void assignTweaksFeatureLabel(Option option) {
        switch (option.sourceKey()) {
            case "enableAutomaticToolRestock" -> option.setFeatureLabels("Tool and Block Restock", "Tool Restock");
            case "enableAutomaticBlockRestock" -> option.setFeatureLabels("Tool and Block Restock", "Block Restock");
            case "enableItemSharing" -> option.setFeatureLabels("Item Sharing", "Enabled");
            case "itemShareCooldownSeconds" -> option.setFeatureLabels("Item Sharing", "Cooldown Seconds");
            case "despawnVexWithEvoker" -> option.setFeatureLabels("Despawn With Owner", "Vexes With Evoker");
            case "despawnBulletsWithShulker" -> option.setFeatureLabels("Despawn With Owner", "Shulker Bullets With Shulker");
            case "enableUnbreakableTrialSpawners" -> option.setFeatureLabels("Unbreakable Trial Blocks", "Trial Spawners");
            case "enableUnbreakableVaults" -> option.setFeatureLabels("Unbreakable Trial Blocks", "Vaults");
            case "enableTippedArrowLingeringClouds" -> option.setFeatureLabels("Tipped Arrows Lingering Clouds", "Enabled");
            case "tippedArrowLingeringCloudsMobsEnabled" -> option.setFeatureLabels("Tipped Arrows Lingering Clouds", "Mobs Enabled");
            case "tippedArrowLingeringCloudsPlayerRadius" -> option.setFeatureLabels("Tipped Arrows Lingering Clouds", "Player Radius");
            case "tippedArrowLingeringCloudsMobRadius" -> option.setFeatureLabels("Tipped Arrows Lingering Clouds", "Mob Radius");
            case "tippedArrowLingeringCloudsDisabledMobIds" -> option.setFeatureLabels("Tipped Arrows Lingering Clouds", "Disabled Mob IDs");
            case "enableMoreMiningXp" -> option.setFeatureLabels("Extra Mining XP", "Enabled");
            case "coalXpMin" -> option.setFeatureLabels("Extra Mining XP", "Minimum", "Coal");
            case "coalXpMax" -> option.setFeatureLabels("Extra Mining XP", "Maximum", "Coal");
            case "diamondXpMin" -> option.setFeatureLabels("Extra Mining XP", "Minimum", "Diamond");
            case "diamondXpMax" -> option.setFeatureLabels("Extra Mining XP", "Maximum", "Diamond");
            case "emeraldXpMin" -> option.setFeatureLabels("Extra Mining XP", "Minimum", "Emerald");
            case "emeraldXpMax" -> option.setFeatureLabels("Extra Mining XP", "Maximum", "Emerald");
            case "lapisXpMin" -> option.setFeatureLabels("Extra Mining XP", "Minimum", "Lapis");
            case "lapisXpMax" -> option.setFeatureLabels("Extra Mining XP", "Maximum", "Lapis");
            case "redstoneXpMin" -> option.setFeatureLabels("Extra Mining XP", "Minimum", "Redstone");
            case "redstoneXpMax" -> option.setFeatureLabels("Extra Mining XP", "Maximum", "Redstone");
            case "enableFastClimbableSlide" -> option.setFeatureLabels("Improved Climbing", "Fast Descent");
            case "fastClimbableSlideSpeed" -> option.setFeatureLabels("Improved Climbing", "Descent Speed");
            case "fastClimbableLookDownMinDeg" -> option.setFeatureLabels("Improved Climbing", "Minimum Look Angle", "Descent");
            case "fastClimbableLookDownMaxDeg" -> option.setFeatureLabels("Improved Climbing", "Maximum Look Angle", "Descent");
            case "enableFastClimbableAscend" -> option.setFeatureLabels("Improved Climbing", "Fast Ascent");
            case "fastClimbableAscendSpeed" -> option.setFeatureLabels("Improved Climbing", "Ascent Speed");
            case "fastClimbableLookUpMinDeg" -> option.setFeatureLabels("Improved Climbing", "Minimum Look Angle", "Ascent");
            case "fastClimbableLookUpMaxDeg" -> option.setFeatureLabels("Improved Climbing", "Maximum Look Angle", "Ascent");
            case "enableResponsiveShields" -> option.setFeatureLabels("Responsive Shields", "Enabled");
            case "shieldRaiseTime" -> option.setFeatureLabels("Responsive Shields", "Raise Time");
            case "enableGlowingGlowberries" -> option.setFeatureLabels("Glowing Glowberries", "Enabled");
            case "glowingGlowberriesDuration" -> option.setFeatureLabels("Glowing Glowberries", "Player Duration");
            case "foxGlowingDuration" -> option.setFeatureLabels("Glowing Glowberries", "Fox Duration");
            case "enableGlowingTorchflowers" -> option.setFeatureLabels("Glowing Torchflowers", "Enabled");
            case "enableSitting" -> option.setFeatureLabels("Sitting", "Enabled");
            case "toggleSitting" -> option.setFeatureLabels("Sitting", "Toggle Mode");
            case "enableCompostableItems" -> option.setFeatureLabels("Compostable Items", "Enabled");
            case "enableCompostableRottenFlesh" -> option.setFeatureLabels("Compostable Items", "Rotten Flesh");
            case "enableCompostablePoisonousPotato" -> option.setFeatureLabels("Compostable Items", "Poisonous Potato");
            case "enableDoubleDoorOpening" -> option.setFeatureLabels("Double Door Opening", "Enabled");
            case "doubleDoorCrouchSingle" -> option.setFeatureLabels("Double Door Opening", "Crouch Opens Single");
            case "doubleDoorChainTrapdoors" -> option.setFeatureLabels("Double Door Opening", "Chain Trapdoors");
            case "doubleDoorSameBlockOnly" -> option.setFeatureLabels("Double Door Opening", "Same Block Only");
            case "doubleDoorsWithRedstone" -> option.setFeatureLabels("Double Door Opening", "Works With Redstone");
            case "doubleDoorRedstoneIncludeIron" -> option.setFeatureLabels("Double Door Opening", "Include Iron Doors", "Redstone");
            case "doubleDoorRedstoneIncludeTrapdoors" -> option.setFeatureLabels("Double Door Opening", "Include Trapdoors", "Redstone");
            case "allowMixedCandlePlacement" -> option.setFeatureLabels("Candle Bundles", "Mixed Candle Placement");
            case "enableImprovedRecoveryCompass" -> option.setFeatureLabels("Improved Recovery Compass", "Enabled");
            case "recoveryCompassKeepOnDeath" -> option.setFeatureLabels("Improved Recovery Compass", "Keep On Death");
            case "recoveryCompassShowDistance" -> option.setFeatureLabels("Improved Recovery Compass", "Show Distance");
            case "recoveryCompassShowCoordsOnRightClick" -> option.setFeatureLabels("Improved Recovery Compass", "Show Coordinates On Right Click");
            case "recoveryCompassSculkParticles" -> option.setFeatureLabels("Improved Recovery Compass", "Sculk Particles");
            case "enableEnderdragonEggAlways" -> option.setFeatureLabels("Enderdragon Egg", "Always Spawn");
            case "enderdragonEggUseRandomPlacement" -> option.setFeatureLabels("Enderdragon Egg", "Use Random Placement");
            case "enderdragonEggSpawnRadius" -> option.setFeatureLabels("Enderdragon Egg", "Spawn Radius");
            case "enderdragonEggSpawnDelaySeconds" -> option.setFeatureLabels("Enderdragon Egg", "Spawn Delay Seconds");
            case "enderdragonEggShowParticles" -> option.setFeatureLabels("Enderdragon Egg", "Show Particles");
            case "brushingXpEnabled" -> option.setFeatureLabels("Brushing XP", "Enabled");
            case "brushingXpMin" -> option.setFeatureLabels("Brushing XP", "Minimum");
            case "brushingXpMax" -> option.setFeatureLabels("Brushing XP", "Maximum");
            case "enableTorchFuel" -> option.setFeatureLabels("Torch Fuel", "Enabled");
            case "torchFuelBurnTime" -> option.setFeatureLabels("Torch Fuel", "Burn Time");
            case "enableUncapMenuFps" -> option.setFeatureLabels("Uncap Menu FPS", "Enabled");
            case "curseHidePumpkinOverlayOnVanishing" -> option.setFeatureLabels("Curse of Vanishing Usage", "Hide Pumpkin Overlay with Curse of Vanishing");
            default -> {
            }
        }
    }

    private static void assignBalanceFeatureLabel(Option option) {
        String key = option.sourceKey();
        if (key.startsWith("dse")) {
            String setting = switch (key) {
                case "dseEnabled" -> "Enabled";
                case "dseOverworldOnly" -> "Overworld Only";
                case "dseEnableSurfaceTier" -> "Surface Tier";
                case "dseEnableMidTier" -> "Mid-Depth Tier";
                case "dseEnableDeepTier" -> "Deep Tier";
                case "dseMidDepthY" -> "Depth Y";
                case "dseDeepDepthY" -> "Depth Y";
                case "dseDeepMaxIronPieces" -> "Max Iron Pieces";
                default -> option.settingName()
                        .replace("DSE ", "")
                        .replace("Dse ", "")
                        .replace("Dse", "Depth Scaling Enemies")
                        .replace("Dse ", "")
                        .replace("Surface ", "")
                        .replace("Mid ", "")
                        .replace("Deep ", "");
            };
            if (key.startsWith("dseSurface")) {
                option.setFeatureLabels("Depth Scaling Enemies", setting, "Surface");
            } else if (key.startsWith("dseMid")) {
                option.setFeatureLabels("Depth Scaling Enemies", setting, "Mid Depth");
            } else if (key.startsWith("dseDeep")) {
                option.setFeatureLabels("Depth Scaling Enemies", setting, "Deep");
            } else {
                option.setFeatureLabels("Depth Scaling Enemies", setting);
            }
            return;
        }

        switch (key) {
            case "enableNerfedMending", "nerfedMendingMultiplier" -> option.setFeatureLabels("Nerfed Mending", option.sourceKey().equals("enableNerfedMending") ? "Enabled" : "XP Multiplier");
            case "enableNerfedDiscounts" -> option.setFeatureLabels("Nerfed Villager Discounts", "Enabled");
            case "removeZombieCureDiscounts" -> option.setFeatureLabels("Nerfed Villager Discounts", "Remove Zombie Cure Discounts");
            case "maxVillagerDiscount" -> option.setFeatureLabels("Nerfed Villager Discounts", "Maximum Discount");
            case "enableSourceDependentIFrames" -> option.setFeatureLabels("Source Dependent I-Frames", "Enabled");
            case "sourceIFrameBlacklist" -> option.setFeatureLabels("Source Dependent I-Frames", "Blacklist");
            case "enableFullEnderDragonXp" -> option.setFeatureLabels("Full Ender Dragon XP", "Enabled");
            default -> {
            }
        }
    }

    private static void assignEnchantingFeatureLabel(Option option) {
        switch (option.sourceKey()) {
            case "enableFortuneBrush" -> option.setFeatureLabels("Fortune Brush", "Enabled");
            case "fortuneBrushBlacklist" -> option.setFeatureLabels("Fortune Brush", "Blacklist");
            case "enableCascading" -> option.setFeatureLabels("Cascading", "Enabled");
            case "cascadingMaxActiveLevel" -> option.setFeatureLabels("Cascading", "Maximum Level");
            case "cascadingLevelOneMaxOres" -> option.setFeatureLabels("Cascading", "Level I Ore Limit");
            case "cascadingLevelTwoMaxOres" -> option.setFeatureLabels("Cascading", "Level II Ore Limit");
            case "cascadingLevelThreeMaxOres" -> option.setFeatureLabels("Cascading", "Level III Ore Limit");
            default -> {
            }
        }
    }

    private static void assignCombatFeatureLabel(Option option) {
        switch (option.sourceKey()) {
            case "enableSnowballRework" -> option.setFeatureLabels("Snowball Rework", "Enabled");
            case "snowballBaseDamage" -> option.setFeatureLabels("Snowball Rework", "Base Damage");
            case "snowballNetherDamage" -> option.setFeatureLabels("Snowball Rework", "Nether Damage");
            case "snowballMinFreezeSeconds" -> option.setFeatureLabels("Snowball Rework", "Minimum Freeze Seconds");
            case "snowballMaxFreezeSeconds" -> option.setFeatureLabels("Snowball Rework", "Maximum Freeze Seconds");
            case "snowballNetherEntities" -> option.setFeatureLabels("Snowball Rework", "Nether Entities");
            case "enableSecondChance" -> option.setFeatureLabels("Second Chance", "Enabled");
            case "secondChanceExcludeFallDamage" -> option.setFeatureLabels("Second Chance", "Exclude Fall Damage");
            case "secondChanceHealthThresholdHearts" -> option.setFeatureLabels("Second Chance", "Health Threshold Hearts");
            case "secondChanceCooldownDays" -> option.setFeatureLabels("Second Chance", "Cooldown Days");
            case "secondChanceResetOnRespawn" -> option.setFeatureLabels("Second Chance", "Reset On Respawn");
            case "secondChanceToastEnabled" -> option.setFeatureLabels("Second Chance", "Toast");
            case "secondChanceResistanceEnabled" -> option.setFeatureLabels("Second Chance", "Enabled", "Apply Resistance");
            case "secondChanceResistanceLevel" -> option.setFeatureLabels("Second Chance", "Level", "Apply Resistance");
            case "secondChanceNauseaEnabled" -> option.setFeatureLabels("Second Chance", "Enabled", "Apply Nausea");
            case "secondChanceNauseaLevel" -> option.setFeatureLabels("Second Chance", "Level", "Apply Nausea");
            case "enableBetterCombatIntegration" -> option.setFeatureLabels("Improved Phantoms", "Better Combat Integration");
            default -> {
            }
        }
    }

    private static void assignMechanicsFeatureLabel(Option option) {
        switch (option.sourceKey()) {
            case "enableGoatHornPurposes" -> option.setFeatureLabels("Purposes for Goat Horns", "Enabled");
            case "goatHornSeparateCooldowns" -> option.setFeatureLabels("Purposes for Goat Horns", "Separate Cooldowns");
            case "goatHornPonder" -> option.setFeatureLabels("Purposes for Goat Horns", "Ponder");
            case "goatHornSing" -> option.setFeatureLabels("Purposes for Goat Horns", "Sing");
            case "goatHornSeek" -> option.setFeatureLabels("Purposes for Goat Horns", "Seek");
            case "goatHornFeel" -> option.setFeatureLabels("Purposes for Goat Horns", "Feel");
            case "goatHornAdmire" -> option.setFeatureLabels("Purposes for Goat Horns", "Admire");
            case "goatHornCall" -> option.setFeatureLabels("Purposes for Goat Horns", "Call");
            case "goatHornYearn" -> option.setFeatureLabels("Purposes for Goat Horns", "Yearn");
            case "goatHornDream" -> option.setFeatureLabels("Purposes for Goat Horns", "Dream");
            case "enableCrawlingMechanic" -> option.setFeatureLabels("Crawling", "Enabled");
            case "enableToggleStance" -> option.setFeatureLabels("Toggle Stance", "Enabled");
            case "enableCoyoteTimeJump" -> option.setFeatureLabels("Coyote Time Jump", "Enabled");
            case "coyoteTimeDelayMs" -> option.setFeatureLabels("Coyote Time Jump", "Delay Ms");
            case "coyoteTimeWindowMs" -> option.setFeatureLabels("Coyote Time Jump", "Window Ms");
            case "coyoteTimeDebug" -> option.setFeatureLabels("Coyote Time Jump", "Debug");
            case "rapidFireJumpDebug" -> option.setFeatureLabels("Rapid Fire Jump", "Debug Logging");
            case "leashedTeleportDebug" -> option.setFeatureLabels("Leashed Teleport", "Debug Logging");
            case "enableBottleOfCloud" -> option.setFeatureLabels("Bottle o' Cloud", "Enabled");
            case "bottleOfCloudDurationTicks" -> option.setFeatureLabels("Bottle o' Cloud", "Duration Ticks");
            case "bottleOfCloudFadeStartTicks" -> option.setFeatureLabels("Bottle o' Cloud", "Fade Start Ticks");
            case "bottleOfCloudPlacementDistance" -> option.setFeatureLabels("Bottle o' Cloud", "Placement Distance");
            case "bottleOfCloudFallDamageMultiplier" -> option.setFeatureLabels("Bottle o' Cloud", "Fall Damage Multiplier");
            case "mentionHighlightColorHex" -> option.setFeatureLabels("Chat Mentions", "Highlight Color HEX");
            case "pistonMoveBlacklistBlockIds" -> option.setFeatureLabels("Pistons Move Tile Entities", "Blacklist Block IDs");
            case "enableWaterlogging" -> option.setFeatureLabels("Waterlogging", "Enabled");
            case "waterloggingDoors" -> option.setFeatureLabels("Waterlogging", "Doors");
            case "waterloggingFenceGates" -> option.setFeatureLabels("Waterlogging", "Fence Gates");
            case "waterloggingPressurePlates" -> option.setFeatureLabels("Waterlogging", "Pressure Plates");
            case "waterloggingWorldgen" -> option.setFeatureLabels("Waterlogging", "Worldgen");
            case "waterloggingMode" -> option.setFeatureLabels("Waterlogging", "Mode");
            case "waterloggingBlockIds" -> option.setFeatureLabels("Waterlogging", "Block IDs");
            default -> {
            }
        }
    }

    private static void assignMobsFeatureLabel(Option option) {
        switch (option.sourceKey()) {
            case "enableCaveSpidersInCaves" -> option.setFeatureLabels("Cave Spiders In Caves", "Enabled");
            case "caveSpiderReplacementChance" -> option.setFeatureLabels("Cave Spiders In Caves", "Replacement Chance");
            case "enableImprovedPhantoms" -> option.setFeatureLabels("Improved Phantoms", "Enabled");
            case "enableImprovedWanderingTraders" -> option.setFeatureLabels("Improved Wandering Traders", "Enabled");
            case "replaceVanillaWanderingTraderTrades" -> option.setFeatureLabels("Improved Wandering Traders", "Replace Vanilla Trades");
            case "enableBuildingMaterialsTrades" -> option.setFeatureLabels("Improved Wandering Traders", "Building Materials Trades");
            case "enableRareItemsTrades" -> option.setFeatureLabels("Improved Wandering Traders", "Rare Items Trades");
            case "enableUtilityItemsTrades" -> option.setFeatureLabels("Improved Wandering Traders", "Utility Items Trades");
            case "enableDecorativeItemsTrades" -> option.setFeatureLabels("Improved Wandering Traders", "Decorative Items Trades");
            case "enablePhantomPickup" -> option.setFeatureLabels("Improved Phantoms", "Enabled", "Pickup");
            case "phantomPickupChance" -> option.setFeatureLabels("Improved Phantoms", "Chance", "Pickup");
            case "phantomPickupMinDelay" -> option.setFeatureLabels("Improved Phantoms", "Minimum Delay", "Pickup");
            case "phantomPickupSpeedMultiplier" -> option.setFeatureLabels("Improved Phantoms", "Speed Multiplier", "Pickup");
            case "enablePhantomSlownessStacking" -> option.setFeatureLabels("Improved Phantoms", "Stacking Enabled", "Slowness");
            case "phantomSlownessDuration" -> option.setFeatureLabels("Improved Phantoms", "Duration", "Slowness");
            case "phantomSlownessMaxLevel" -> option.setFeatureLabels("Improved Phantoms", "Max Level", "Slowness");
            case "enablePhantomDoubleDamage" -> option.setFeatureLabels("Improved Phantoms", "Double Damage");
            case "enablePhantomAutoDrop" -> option.setFeatureLabels("Improved Phantoms", "Enabled", "Auto Drop");
            case "phantomAutoDropSeconds" -> option.setFeatureLabels("Improved Phantoms", "Seconds", "Auto Drop");
            case "enablePhantomDropOnHit" -> option.setFeatureLabels("Improved Phantoms", "Drop On Hit");
            case "enablePhantomSlowFallingOnHitDismount" -> option.setFeatureLabels("Improved Phantoms", "Slow Falling On Hit Dismount");
            case "dualRiders" -> option.setFeatureLabels("Dual Riders for Horses", "Enabled");
            default -> {
            }
        }
    }

    private static void assignRecipesFeatureLabel(Option option) {
        switch (option.sourceKey()) {
            case "enableAlternativeRepeaterRecipe" -> option.setFeatureLabels("Alternative Crafting", "Redstone Repeater");
            case "enableChestFromLogsRecipe" -> option.setFeatureLabels("Alternative Crafting", "Chest From Logs");
            case "enableMapInkSacRecipe" -> option.setFeatureLabels("Alternative Crafting", "Map With Ink Sac");
            case "enableRecoveryCompassRecipe" -> option.setFeatureLabels("Alternative Crafting", "Recovery Compass");
            case "enableEnderEyeRecipe" -> option.setFeatureLabels("Alternative Crafting", "Ender Eye");
            case "enableEnchantingTableDiamondBlocksRecipe" -> option.setFeatureLabels("Alternative Crafting", "Enchanting Table Diamond Blocks");
            case "enableRawIronSmeltingRecipe" -> option.setFeatureLabels("Raw Ore Smelting/Blasting", "Smelting", "Raw Iron");
            case "enableRawGoldSmeltingRecipe" -> option.setFeatureLabels("Raw Ore Smelting/Blasting", "Smelting", "Raw Gold");
            case "enableRawCopperSmeltingRecipe" -> option.setFeatureLabels("Raw Ore Smelting/Blasting", "Smelting", "Raw Copper");
            case "enableRawIronBlastingRecipe" -> option.setFeatureLabels("Raw Ore Smelting/Blasting", "Blasting", "Raw Iron");
            case "enableRawGoldBlastingRecipe" -> option.setFeatureLabels("Raw Ore Smelting/Blasting", "Blasting", "Raw Gold");
            case "enableRawCopperBlastingRecipe" -> option.setFeatureLabels("Raw Ore Smelting/Blasting", "Blasting", "Raw Copper");
            case "enableOreSmeltingXp" -> option.setFeatureLabels("Ore Smelting XP", "Enabled");
            case "coalOreSmeltingXp" -> option.setFeatureLabels("Ore Smelting XP", "Coal Ore");
            case "lapisOreSmeltingXp" -> option.setFeatureLabels("Ore Smelting XP", "Lapis Ore");
            case "netherQuartzOreSmeltingXp" -> option.setFeatureLabels("Ore Smelting XP", "Nether Quartz Ore");
            case "ironOreSmeltingXp" -> option.setFeatureLabels("Ore Smelting XP", "Iron Ore");
            case "copperOreSmeltingXp" -> option.setFeatureLabels("Ore Smelting XP", "Copper Ore");
            case "redstoneOreSmeltingXp" -> option.setFeatureLabels("Ore Smelting XP", "Redstone Ore");
            case "goldOreSmeltingXp" -> option.setFeatureLabels("Ore Smelting XP", "Gold Ore");
            case "netherGoldOreSmeltingXp" -> option.setFeatureLabels("Ore Smelting XP", "Nether Gold Ore");
            case "diamondOreSmeltingXp" -> option.setFeatureLabels("Ore Smelting XP", "Diamond Ore");
            case "emeraldOreSmeltingXp" -> option.setFeatureLabels("Ore Smelting XP", "Emerald Ore");
            case "ancientDebrisSmeltingXp" -> option.setFeatureLabels("Ore Smelting XP", "Ancient Debris");
            case "enableStairRecipeOverride" -> option.setFeatureLabels("Stair Recipes", "Override Vanilla Output");
            case "enableCompactStairRecipe" -> option.setFeatureLabels("Stair Recipes", "Compact Recipe");
            default -> {
            }
        }
    }

    private static void assignPotionFeatureLabel(Option option) {
        switch (option.sourceKey()) {
            case "enableCustomBrewing" -> option.setFeatureLabels("Custom Brewing", "Enabled");
            default -> {
            }
        }
    }

    private static void assignLootFeatureLabel(Option option) {
        String key = option.sourceKey();
        switch (key) {
            case "enableBrushingLoot" -> option.setFeatureLabels("Brushing Loot", "Enabled");
            case "brushingDesertWellEnabled" -> option.setFeatureLabels("Brushing Loot", "Enabled", "Desert Wells");
            case "brushingDesertPyramidEnabled" -> option.setFeatureLabels("Brushing Loot", "Enabled", "Desert Pyramids");
            case "brushingTrailRuinsEnabled" -> option.setFeatureLabels("Brushing Loot", "Enabled", "Trail Ruins");
            case "brushingColdOceanRuinsEnabled" -> option.setFeatureLabels("Brushing Loot", "Enabled", "Cold Ocean Ruins");
            case "brushingWarmOceanRuinsEnabled" -> option.setFeatureLabels("Brushing Loot", "Enabled", "Warm Ocean Ruins");
            case "enableCustomLootTables" -> option.setFeatureLabels("Custom Loot Tables", "Enabled");
            case "enableDogMusicDisc" -> option.setFeatureLabels("Dog Music Disc", "Enabled");
            case "logMissingIds" -> option.setFeatureLabels("Loot Logging", "Log Missing IDs");
            case "enableMineshaftLoot" -> option.setFeatureLabels("Mineshaft Loot", "Enabled");
            case "enableStrongholdLoot" -> option.setFeatureLabels("Stronghold Loot", "Enabled");
            case "enableAncientCityLoot" -> option.setFeatureLabels("Ancient City Loot", "Enabled");
            case "enableEndCityLoot" -> option.setFeatureLabels("End City Loot", "Loot Injections");
            case "enableSimpleDungeonLoot" -> option.setFeatureLabels("Simple Dungeon Loot", "Enabled");
            case "enableTrialChamberLoot" -> option.setFeatureLabels("Trial Chamber Loot", "Enabled");
            case "enableEndCityAerodynamicBook" -> option.setFeatureLabels("End City Loot", "Enabled", "Aerodynamic Book");
            case "endCityAerodynamicBookChance" -> option.setFeatureLabels("End City Loot", "Appearance Chance", "Aerodynamic Book");
            default -> assignLootEntryFeatureLabel(option, key);
        }
    }

    private static void assignLootEntryFeatureLabel(Option option, String key) {
        assignLootEntryFeatureLabel(option, key, "mineshaft", "Mineshaft Loot", "Resurfacing Potion", "resurfacing");
        assignLootEntryFeatureLabel(option, key, "mineshaft", "Mineshaft Loot", "Returning Potion", "returning");
        assignLootEntryFeatureLabel(option, key, "mineshaft", "Mineshaft Loot", "Haste Potion", "haste");
        assignLootEntryFeatureLabel(option, key, "mineshaft", "Mineshaft Loot", "Strong Haste Potion", "strongHaste");
        assignLootEntryFeatureLabel(option, key, "stronghold", "Stronghold Loot", "Resurfacing Potion", "resurfacing");
        assignLootEntryFeatureLabel(option, key, "stronghold", "Stronghold Loot", "Returning Potion", "returning");
        assignLootEntryFeatureLabel(option, key, "ancientCity", "Ancient City Loot", "Resurfacing Potion", "resurfacing");
        assignLootEntryFeatureLabel(option, key, "ancientCity", "Ancient City Loot", "Returning Potion", "returning");
        assignLootEntryFeatureLabel(option, key, "endCity", "End City Loot", "Returning Potion", "returning");
        assignLootEntryFeatureLabel(option, key, "simpleDungeon", "Simple Dungeon Loot", "Resurfacing Potion", "resurfacing");
        assignLootEntryFeatureLabel(option, key, "simpleDungeon", "Simple Dungeon Loot", "Returning Potion", "returning");
        assignLootEntryFeatureLabel(option, key, "trialCommon", "Trial Chamber Loot", "Resurfacing Potion", "resurfacing");
        assignLootEntryFeatureLabel(option, key, "trialRare", "Trial Chamber Loot", "Resurfacing Potion", "resurfacing");
        assignLootEntryFeatureLabel(option, key, "trialRare", "Trial Chamber Loot", "Returning Potion", "returning");
        assignLootEntryFeatureLabel(option, key, "trialUnique", "Trial Chamber Loot", "Returning Potion", "returning");
    }

    private static void assignLootEntryFeatureLabel(Option option, String key, String prefix, String featureName, String subFeatureName, String entryName) {
        String fieldPrefix = prefix + upperFirst(entryName);
        if (key.equals("enable" + upperFirst(fieldPrefix) + "Potion")) {
            option.setFeatureLabels(featureName, "Enabled", subFeatureName);
            return;
        }

        if (!key.startsWith(fieldPrefix)) {
            return;
        }

        String setting = switch (key.substring(fieldPrefix.length())) {
            case "Weight" -> "Weight";
            case "MinCount" -> "Minimum Count";
            case "MaxCount" -> "Maximum Count";
            default -> "";
        };
        if (!setting.isBlank()) {
            option.setFeatureLabels(featureName, setting, subFeatureName);
        }
    }

    private static void assignWorldgenFeatureLabels(List<Option> options) {
        for (Option option : options) {
            if (option.owner() != FabricWorldgenConfig.class) {
                continue;
            }

            switch (option.sourceKey()) {
                case "debugVeins" -> option.setFeatureLabels("Ore Veins", "Debug Logging", "Debug");
                case "disableNetherLavaSprings" -> option.setFeatureLabels("Lava Springs", "Disable Nether Lava Springs");
                case "enableAzaleaWoodGeneration" -> option.setFeatureLabels("Azalea Wood", "Generation");
                case "enableAzaleaWoodset" -> option.setFeatureLabels("Azalea Wood", "Woodset");
                case "enableCoalAndesiteVeins" -> option.setFeatureLabels("Ore Veins", "Andesite Veins", "Overworld Coal");
                case "enableCoalTuffVeins" -> option.setFeatureLabels("Ore Veins", "Tuff Veins", "Overworld Coal");
                case "enableNetherGoldVeins" -> option.setFeatureLabels("Ore Veins", "Enabled", "Nether Gold");
                case "enableNetherQuartzVeins" -> option.setFeatureLabels("Ore Veins", "Enabled", "Nether Quartz");
                case "enableOreVariants" -> option.setFeatureLabels("Ore Variants", "Enabled");
                case "enableParallelProcessing" -> option.setFeatureLabels("Ore Variants", "Parallel Processing");
                case "enableRawQuartzBlock" -> option.setFeatureLabels("Ore Veins", "Enabled", "Raw Quartz");
                case "enableRawQuartzBlockInQuartzVeins" -> option.setFeatureLabels("Ore Veins", "Place In Nether Quartz Veins", "Raw Quartz");
                case "keepExposedNetherLavaSprings" -> option.setFeatureLabels("Lava Springs", "Keep Exposed Nether Lava Springs");
                case "parallelProcessingThreadCount" -> option.setFeatureLabels("Ore Variants", "Parallel Thread Count");
                case "disabledKeys" -> option.setFeatureLabels("Worldgen Blacklist", "Keys");
                default -> {
                    if (option.name().equals("Disabled") || option.name().equals("Blacklist")) {
                        option.setFeatureLabels("Worldgen Blacklist", "Keys");
                    }
                }
            }
        }
    }

    private static void normalizeEnabledFeatureTitles(List<Option> options) {
        for (Option option : options) {
            if (!option.featureName().equals("Enabled")) {
                continue;
            }

            String featureName = featureNameFromEnabledOption(option);
            if (featureName.isBlank() || featureName.equals("Enabled")) {
                continue;
            }
            option.setFeatureLabels(featureName, option.settingName(), option.subFeatureName());
        }
    }

    private static String featureNameFromEnabledOption(Option option) {
        String sourceKey = option.sourceKey();
        if (sourceKey.startsWith("enable") && sourceKey.length() > "enable".length()) {
            return humanize(sourceKey.substring("enable".length()));
        }
        if (sourceKey.endsWith("Enabled") && sourceKey.length() > "Enabled".length()) {
            return humanize(sourceKey.substring(0, sourceKey.length() - "Enabled".length()));
        }
        if (sourceKey.equals("enabled") && !option.group().isBlank()) {
            return option.group();
        }
        if (!option.name().equals("Enabled")) {
            return option.name();
        }
        return "";
    }

    private static Map<String, String> enabledFeatureStems(List<Option> options) {
        Set<String> sourceKeys = new LinkedHashSet<>();
        for (Option option : options) {
            sourceKeys.add(option.sourceKey());
        }

        Map<String, String> stems = new LinkedHashMap<>();
        for (String sourceKey : sourceKeys) {
            if (!sourceKey.startsWith("enable") || sourceKey.length() <= "enable".length()) {
                continue;
            }

            String featureSuffix = sourceKey.substring("enable".length());
            String stem = lowerFirst(featureSuffix);
            boolean hasRelatedSetting = false;
            for (String candidate : sourceKeys) {
                if (!candidate.equals(sourceKey) && candidate.startsWith(stem)) {
                    hasRelatedSetting = true;
                    break;
                }
            }
            if (hasRelatedSetting) {
                stems.put(stem, humanize(featureSuffix));
            }
        }
        addAliasFeatureStem(stems, sourceKeys, "enableChatMentions", "Chat Mentions", "chatMentions", "mention");
        addAliasFeatureStem(stems, sourceKeys, "enableBottleOfCloud", "Bottle o' Cloud", "bottleOfCloud");
        addAliasFeatureStem(stems, sourceKeys, "enableDismountEntities", "Dismount Entities", "dismountEntities", "dismount");
        addAliasFeatureStem(stems, sourceKeys, "enablePistonsMoveTileEntities", "Pistons Move Tile Entities", "pistonsMoveTileEntities", "pistonMove");
        addAliasFeatureStem(stems, sourceKeys, "enableVillagersFollowEmeralds", "Villagers Follow Emeralds", "villagersFollowEmeralds", "villagerEmerald");
        return stems;
    }

    private static void addAliasFeatureStem(Map<String, String> stems, Set<String> sourceKeys, String enableKey, String featureName, String... aliases) {
        if (!sourceKeys.contains(enableKey)) {
            return;
        }

        boolean hasRelatedSetting = false;
        for (String alias : aliases) {
            for (String sourceKey : sourceKeys) {
                if (!sourceKey.equals(enableKey) && sourceKey.startsWith(alias)) {
                    hasRelatedSetting = true;
                    break;
                }
            }
            if (hasRelatedSetting) {
                break;
            }
        }
        if (!hasRelatedSetting) {
            return;
        }

        for (String alias : aliases) {
            stems.put(alias, featureName);
        }
    }

    private static String matchingEnabledFeatureStem(String sourceKey, Map<String, String> stems) {
        String best = "";
        for (String stem : stems.keySet()) {
            String enableKey = "enable" + upperFirst(stem);
            if ((sourceKey.equals(enableKey) || sourceKey.startsWith(stem)) && stem.length() > best.length()) {
                best = stem;
            }
        }
        return best;
    }

    private static String settingNameForFeatureStem(String sourceKey, String stem, String featureName) {
        if (sourceKey.equals("enable" + upperFirst(stem))) {
            return "Enabled";
        }

        String remainder = sourceKey.length() > stem.length() ? sourceKey.substring(stem.length()) : "";
        String setting = humanize(remainder);
        if (setting.isBlank() || setting.equals(featureName)) {
            return "Enabled";
        }
        return setting;
    }

    private static String lowerFirst(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static String upperFirst(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String bestFeaturePrefix(String name, Map<String, Integer> prefixCounts) {
        List<String> words = words(name);
        int maxPrefixLength = Math.min(3, Math.max(0, words.size() - 1));
        for (int length = maxPrefixLength; length >= 1; length--) {
            String prefix = String.join(" ", words.subList(0, length));
            if (prefixCounts.getOrDefault(prefix, 0) >= 2) {
                return prefix;
            }
        }
        return "";
    }

    private static List<String> words(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return List.of(name.trim().split("\\s+"));
    }

    private static String combine(String prefix, String name) {
        return prefix.isBlank() ? name : prefix + " " + name;
    }

    private static String humanizeTypeName(String value) {
        return humanize(value.replace("Config", ""));
    }

    private static String humanizeFieldName(String value) {
        String name = value;
        if (name.startsWith("enable") && name.length() > "enable".length()) {
            name = name.substring("enable".length());
        }
        return humanize(name);
    }

    private static String humanize(String value) {
        String spaced = value
                .replace('_', ' ')
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                .trim();
        if (spaced.isEmpty()) {
            return value;
        }

        StringBuilder result = new StringBuilder();
        for (String part : spaced.split("\\s+")) {
            if (part.length() <= 2 && part.equals(part.toUpperCase(Locale.ROOT))) {
                result.append(part);
            } else {
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1));
                }
            }
            result.append(' ');
        }
        return normalizeAcronyms(result.toString().trim());
    }

    private static String normalizeAcronyms(String value) {
        return value
                .replace(" Ids", " IDs")
                .replace(" Id", " ID")
                .replace(" Xp", " XP")
                .replace(" Fps", " FPS")
                .replace(" Hex", " HEX");
    }

    private static String normalizeSearch(String value) {
        String humanized = humanize(value)
                .replace('-', ' ')
                .replace('.', ' ')
                .replace('_', ' ')
                .toLowerCase(Locale.ROOT);
        return humanized.replaceAll("\\b(enable|enabled|is|config|fabric)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record Category(String name, List<Group> groups) {
        public List<Option> options() {
            List<Option> options = new ArrayList<>();
            for (Group group : groups) {
                options.addAll(group.options());
            }
            return options;
        }
    }

    public record Group(String name, List<Option> options) {
    }

    private record ConfigGroup(String label, Class<?> owner, boolean restartRecommended) {
    }

    private record DescriptionEntry(String path, String key, String parentPath, String description) {
    }

    public record AuditFinding(String type, String path, String message) {
    }

    public abstract static class Option {
        private final Class<?> owner;
        private final String sourceKey;
        private final String name;
        private final String group;
        private final String kind;
        private final String description;
        private final boolean restartRecommended;
        private final int order;
        private String featureName;
        private String subFeatureName;
        private String settingName;

        protected Option(Class<?> owner, String sourceKey, String name, String group, String kind, String description, boolean restartRecommended, int order) {
            this.owner = owner;
            this.sourceKey = sourceKey;
            this.name = name;
            this.group = group;
            this.kind = kind;
            this.description = description;
            this.restartRecommended = restartRecommended;
            this.order = order;
            this.featureName = name;
            this.subFeatureName = "";
            this.settingName = name;
        }

        public final Class<?> owner() {
            return owner;
        }

        public final String name() {
            return name;
        }

        public final String sourceKey() {
            return sourceKey;
        }

        public final String kind() {
            return kind;
        }

        public final String description() {
            return description;
        }

        public final String group() {
            return group;
        }

        public final String featureName() {
            return featureName;
        }

        public final String subFeatureName() {
            return subFeatureName;
        }

        public final String settingName() {
            return settingName;
        }

        private void setFeatureLabels(String featureName, String settingName) {
            this.featureName = featureName;
            this.subFeatureName = "";
            this.settingName = settingName;
        }

        private void setFeatureLabels(String featureName, String settingName, String subFeatureName) {
            this.featureName = featureName;
            this.subFeatureName = subFeatureName;
            this.settingName = settingName;
        }

        public final boolean matches(String query) {
            String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            if (normalized.isBlank()) {
                return true;
            }
            return name.toLowerCase(Locale.ROOT).contains(normalized)
                    || featureName.toLowerCase(Locale.ROOT).contains(normalized)
                    || subFeatureName.toLowerCase(Locale.ROOT).contains(normalized)
                    || settingName.toLowerCase(Locale.ROOT).contains(normalized)
                    || description.toLowerCase(Locale.ROOT).contains(normalized)
                    || group.toLowerCase(Locale.ROOT).contains(normalized)
                    || owner.getSimpleName().toLowerCase(Locale.ROOT).contains(normalized);
        }

        public final boolean restartRecommended() {
            return restartRecommended;
        }

        public final int order() {
            return order;
        }

        public abstract boolean isBoolean();

        public abstract boolean booleanValue();

        public abstract void toggleBoolean();

        public abstract String pendingText();

        public abstract void setPendingText(String value);

        public abstract void resetPendingValue();

        public abstract void applyPendingValue() throws IOException;

        public abstract String valueText();

        public abstract boolean hasPendingChange();
    }

    public static final class FieldOption extends Option {
        private final Field field;
        private final Object initialValue;
        private Object pendingValue;
        private String pendingText;

        private FieldOption(Class<?> owner, Field field, String sourceKey, String name, String group, String kind, String description, Object value, boolean restartRecommended, int order) {
            super(owner, sourceKey, name, group, kind, description, restartRecommended, order);
            this.field = field;
            this.initialValue = immutableCopy(value);
            this.pendingValue = immutableCopy(value);
            this.pendingText = toText(this.pendingValue);
        }

        @Override
        public boolean isBoolean() {
            Class<?> type = field.getType();
            return type == boolean.class || type == Boolean.class;
        }

        @Override
        public boolean booleanValue() {
            return pendingValue instanceof Boolean value && value;
        }

        @Override
        public void toggleBoolean() {
            if (isBoolean()) {
                pendingValue = !booleanValue();
                pendingText = toText(pendingValue);
            }
        }

        @Override
        public String pendingText() {
            return pendingText;
        }

        @Override
        public void setPendingText(String value) {
            this.pendingText = value;
        }

        @Override
        public void resetPendingValue() {
            pendingValue = immutableCopy(initialValue);
            pendingText = toText(pendingValue);
        }

        @Override
        public void applyPendingValue() throws IOException {
            Object value = parsePendingValue();
            try {
                field.set(null, value);
                pendingValue = immutableCopy(value);
                pendingText = toText(value);
            } catch (IllegalAccessException e) {
                throw new IOException("Failed to update " + name(), e);
            }
        }

        @Override
        public String valueText() {
            return isBoolean() ? (booleanValue() ? "Enabled" : "Disabled") : pendingText;
        }

        @Override
        public boolean hasPendingChange() {
            if (isBoolean()) {
                return !Objects.equals(pendingValue, initialValue);
            }
            return !Objects.equals(pendingText, toText(initialValue));
        }

        private Object parsePendingValue() throws IOException {
            Class<?> type = field.getType();
            String text = pendingText == null ? "" : pendingText.trim();
            try {
                if (type == boolean.class || type == Boolean.class) {
                    return booleanValue();
                }
                if (type == int.class || type == Integer.class) {
                    return Integer.parseInt(text);
                }
                if (type == long.class || type == Long.class) {
                    return Long.parseLong(text);
                }
                if (type == double.class || type == Double.class) {
                    return Double.parseDouble(text);
                }
                if (type == float.class || type == Float.class) {
                    return Float.parseFloat(text);
                }
                if (type == String.class) {
                    return pendingText == null ? "" : pendingText;
                }
                if (List.class.isAssignableFrom(type)) {
                    if (sourceKey().equals("blockedLogMessages")) {
                        return org.onenonly.bitsandbalance.common.client.LogMessageList.parse(pendingText);
                    }
                    return parseStringList(pendingText);
                }
                if (Set.class.isAssignableFrom(type)) {
                    return new LinkedHashSet<>(parseStringList(pendingText));
                }
                if (Map.class.isAssignableFrom(type)) {
                    return parseStringIntegerMap(pendingText);
                }
            } catch (NumberFormatException e) {
                throw new IOException("Invalid value for " + name() + ": " + pendingText, e);
            }
            throw new IOException("Unsupported option type for " + name());
        }

        private static Object immutableCopy(Object value) {
            if (value instanceof List<?> || value instanceof Set<?>) {
                return new ArrayList<>(copyStringList(value));
            }
            if (value instanceof Map<?, ?>) {
                return copyStringIntegerMap(value);
            }
            return value;
        }

        private String toText(Object value) {
            if (value instanceof Collection<?> collection) {
                if (sourceKey().equals("blockedLogMessages")) {
                    return org.onenonly.bitsandbalance.common.client.LogMessageList.format(collection);
                }
                return String.join(", ", collection.stream().map(Objects::toString).toList());
            }
            if (value instanceof Map<?, ?> map) {
                List<String> entries = new ArrayList<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    entries.add(entry.getKey() + "=" + entry.getValue());
                }
                return String.join(", ", entries);
            }
            return Objects.toString(value, "");
        }

        private static List<String> parseStringList(String text) {
            List<String> values = new ArrayList<>();
            if (text == null || text.isBlank()) {
                return values;
            }
            for (String value : text.split(",")) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    values.add(trimmed);
                }
            }
            return values;
        }

        private static Map<String, Integer> parseStringIntegerMap(String text) throws IOException {
            Map<String, Integer> values = new LinkedHashMap<>();
            if (text == null || text.isBlank()) {
                return values;
            }
            for (String entry : text.split(",")) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int split = trimmed.indexOf('=');
                if (split <= 0 || split == trimmed.length() - 1) {
                    throw new IOException("Map entries must use key=value: " + trimmed);
                }
                values.put(trimmed.substring(0, split).trim(), Integer.parseInt(trimmed.substring(split + 1).trim()));
            }
            return values;
        }
    }
}
