package org.onenonly.bitsandbalance.fabric.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.mechanics.GlowGooRuntime;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricCombatConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricContentConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricEnchantingConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricLootConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricMobsConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricRecipesConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricWorldgenConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FabricConfigBootAudit {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-fabric-config-audit");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String AUDIT_FILE_NAME = "bitsandbalance-fabric-config-audit.json";
    private static final List<Class<?>> CONFIG_CLASSES = List.of(
            FabricBalanceConfig.class,
            FabricBuildingConfig.class,
            FabricClientConfig.class,
            FabricCombatConfig.class,
            FabricContentConfig.class,
            FabricEnchantingConfig.class,
            FabricLootConfig.class,
            FabricMechanicsConfig.class,
            FabricMobsConfig.class,
            FabricPotionConfig.class,
            FabricRecipesConfig.class,
            FabricTweaksConfig.class
    );

    private FabricConfigBootAudit() {
    }

        public static BootAuditResult writeBootAudit() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        root.addProperty("loader", "fabric");
        root.addProperty("generatedAt", Instant.now().toString());

        JsonObject booleanFlags = new JsonObject();
        JsonArray falseFeatureLikeFlags = new JsonArray();
        int flagCount = 0;

        for (Class<?> configClass : CONFIG_CLASSES) {
            for (Field field : getAuditedFields(configClass)) {
                try {
                    boolean value = field.getBoolean(null);
                    String key = configClass.getSimpleName() + "." + field.getName();
                    booleanFlags.addProperty(key, value);
                    if (isFeatureLikeField(field.getName()) && !value) {
                        falseFeatureLikeFlags.add(key);
                    }
                    flagCount++;
                } catch (IllegalAccessException exception) {
                    LOGGER.warn("Skipping Fabric boot audit field {}.{}: {}",
                            configClass.getSimpleName(),
                            field.getName(),
                            exception.toString());
                }
            }
        }

        addBoolean(booleanFlags, falseFeatureLikeFlags, "FabricWorldgenConfig.enableNetherGoldVeins",
                FabricWorldgenConfig.isEnabled(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "nether_gold_vein")));
        addBoolean(booleanFlags, falseFeatureLikeFlags, "FabricWorldgenConfig.enableNetherQuartzVeins",
                FabricWorldgenConfig.isEnabled(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "nether_quartz_vein")));
        addBoolean(booleanFlags, falseFeatureLikeFlags, "FabricWorldgenConfig.enableOreVariants",
                FabricWorldgenConfig.isOreVariantsEnabled());
        addBoolean(booleanFlags, falseFeatureLikeFlags, "FabricWorldgenConfig.enableParallelProcessing",
                FabricWorldgenConfig.isParallelProcessingEnabled());
        addBoolean(booleanFlags, falseFeatureLikeFlags, "FabricWorldgenConfig.enableAzaleaWoodGeneration",
                FabricWorldgenConfig.isAzaleaWoodGenerationEnabled());
        addBoolean(booleanFlags, falseFeatureLikeFlags, "FabricWorldgenConfig.enableAzaleaWoodset",
                FabricWorldgenConfig.isAzaleaWoodsetEnabled());
        addBoolean(booleanFlags, falseFeatureLikeFlags, "FabricWorldgenConfig.enableDogMusicDisc",
                FabricWorldgenConfig.isDogMusicDiscEnabled());
        addBoolean(booleanFlags, falseFeatureLikeFlags, "FabricWorldgenConfig.enableCoalAndesiteVeins",
                FabricWorldgenConfig.isCoalAndesiteVeinsEnabled());
        addBoolean(booleanFlags, falseFeatureLikeFlags, "FabricWorldgenConfig.enableCoalTuffVeins",
                FabricWorldgenConfig.isCoalTuffVeinsEnabled());
        addBoolean(booleanFlags, falseFeatureLikeFlags, "FabricWorldgenConfig.enableAnyMojangStyleCoalVeins",
                FabricWorldgenConfig.isAnyMojangStyleCoalVeinsEnabled());
        addBoolean(booleanFlags, falseFeatureLikeFlags, "FabricWorldgenConfig.debugVeins",
                FabricWorldgenConfig.isVeinsDebugEnabled());

        addBoolean(booleanFlags, falseFeatureLikeFlags, "BitsAndBalanceCommon.oreVariantsEnabled",
                BitsAndBalanceCommon.isOreVariantsEnabled());
        addBoolean(booleanFlags, falseFeatureLikeFlags, "BitsAndBalanceCommon.parallelWorldgenProcessingEnabled",
                BitsAndBalanceCommon.isParallelWorldgenProcessingEnabled());
        addBoolean(booleanFlags, falseFeatureLikeFlags, "BitsAndBalanceCommon.rawQuartzBlockInQuartzVeinsEnabled",
                BitsAndBalanceCommon.isEnableRawQuartzBlockInQuartzVeins());

        int dynamicStepBlockCount = StepDynamicRegistry.getAllStepBlocksSnapshot().size();
        int dynamicVerticalStepBlockCount = VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot().size();
        boolean verticalStepRuntimeEnabled = FabricTweaksConfig.isStepRuntimeEnabled()
                && FabricTweaksConfig.isVerticalSlabRuntimeEnabled()
                && dynamicVerticalStepBlockCount > 0;

        JsonObject effectiveCapabilities = new JsonObject();
        effectiveCapabilities.addProperty("enhancedSlabTweaksMasterEnabled", FabricTweaksConfig.enableEnhancedSlabs);
        effectiveCapabilities.addProperty("generatedEnhancedSlabContentEnabled", FabricTweaksConfig.isGeneratedEnhancedSlabContentEnabled());
        effectiveCapabilities.addProperty("verticalSlabRuntimeEnabled", FabricTweaksConfig.isVerticalSlabRuntimeEnabled());
        effectiveCapabilities.addProperty("stepRuntimeEnabled", FabricTweaksConfig.isStepRuntimeEnabled());
        effectiveCapabilities.addProperty("verticalStepRuntimeEnabled", verticalStepRuntimeEnabled);
        effectiveCapabilities.addProperty("dynamicStepBlockCount", dynamicStepBlockCount);
        effectiveCapabilities.addProperty("dynamicVerticalStepBlockCount", dynamicVerticalStepBlockCount);
        effectiveCapabilities.addProperty("mixedDoubleSlabRuntimeEnabled",
                FabricTweaksConfig.enableEnhancedSlabs && FabricTweaksConfig.enhancedSlabsMixedDoubleSlabs);
        effectiveCapabilities.addProperty("kneeSlabMiningRuntimeEnabled",
                FabricTweaksConfig.enableEnhancedSlabs && FabricTweaksConfig.enhancedSlabsKneeSlabMining);

        JsonArray checks = new JsonArray();
        int failedChecks = 0;
        failedChecks += addCheck(
                checks,
                "content.glowGooRuntimeMatchesConfig",
                FabricContentConfig.enableGlowGoo == GlowGooRuntime.enabled,
                Map.of(
                        "config", Boolean.toString(FabricContentConfig.enableGlowGoo),
                        "runtime", Boolean.toString(GlowGooRuntime.enabled)
                )
        );
        failedChecks += addCheck(
                checks,
                "content.glowGooImpactParticlesMatchRuntime",
                FabricContentConfig.glowGooImpactParticlesEnabled == GlowGooRuntime.impactParticlesEnabled,
                Map.of(
                        "config", Boolean.toString(FabricContentConfig.glowGooImpactParticlesEnabled),
                        "runtime", Boolean.toString(GlowGooRuntime.impactParticlesEnabled)
                )
        );
        failedChecks += addCheck(
                checks,
                "content.glowGooBioluminescenceMatchesRuntime",
                FabricContentConfig.glowGooBioluminescenceEnabled == GlowGooRuntime.bioluminescenceEnabled,
                Map.of(
                        "config", Boolean.toString(FabricContentConfig.glowGooBioluminescenceEnabled),
                        "runtime", Boolean.toString(GlowGooRuntime.bioluminescenceEnabled)
                )
        );
        failedChecks += addCheck(
                checks,
                "building.verticalSlabsMirrorMatchesTweaks",
                FabricBuildingConfig.enhancedSlabsVerticalSlabs == FabricTweaksConfig.enhancedSlabsVerticalSlabs,
                Map.of(
                        "building", Boolean.toString(FabricBuildingConfig.enhancedSlabsVerticalSlabs),
                        "tweaksMirror", Boolean.toString(FabricTweaksConfig.enhancedSlabsVerticalSlabs)
                )
        );
        failedChecks += addCheck(
                checks,
                "building.stepsMirrorMatchesTweaks",
                FabricBuildingConfig.enhancedSlabsSteps == FabricTweaksConfig.enhancedSlabsSteps,
                Map.of(
                        "building", Boolean.toString(FabricBuildingConfig.enhancedSlabsSteps),
                        "tweaksMirror", Boolean.toString(FabricTweaksConfig.enhancedSlabsSteps)
                )
        );
        failedChecks += addCheck(
                checks,
                "building.stepsDisabledClearsDynamicStepRegistry",
                FabricBuildingConfig.enhancedSlabsSteps || dynamicStepBlockCount == 0,
                Map.of(
                        "building", Boolean.toString(FabricBuildingConfig.enhancedSlabsSteps),
                        "dynamicStepBlockCount", Integer.toString(dynamicStepBlockCount)
                )
        );
        failedChecks += addCheck(
                checks,
                "building.stepsDisabledClearsDynamicVerticalStepRegistry",
                FabricBuildingConfig.enhancedSlabsSteps || dynamicVerticalStepBlockCount == 0,
                Map.of(
                        "building", Boolean.toString(FabricBuildingConfig.enhancedSlabsSteps),
                        "dynamicVerticalStepBlockCount", Integer.toString(dynamicVerticalStepBlockCount)
                )
        );
        failedChecks += addCheck(
                checks,
                "building.verticalSlabsDisabledClearsDynamicVerticalStepRegistry",
                FabricBuildingConfig.enhancedSlabsVerticalSlabs || dynamicVerticalStepBlockCount == 0,
                Map.of(
                        "building", Boolean.toString(FabricBuildingConfig.enhancedSlabsVerticalSlabs),
                        "dynamicVerticalStepBlockCount", Integer.toString(dynamicVerticalStepBlockCount)
                )
        );
        failedChecks += addCheck(
                checks,
                "worldgen.oreVariantsMirrorMatchesCommon",
                FabricWorldgenConfig.isOreVariantsEnabled() == BitsAndBalanceCommon.isOreVariantsEnabled(),
                Map.of(
                        "config", Boolean.toString(FabricWorldgenConfig.isOreVariantsEnabled()),
                        "common", Boolean.toString(BitsAndBalanceCommon.isOreVariantsEnabled())
                )
        );
        failedChecks += addCheck(
                checks,
                "worldgen.parallelProcessingMirrorMatchesCommon",
                FabricWorldgenConfig.isParallelProcessingEnabled() == BitsAndBalanceCommon.isParallelWorldgenProcessingEnabled(),
                Map.of(
                        "config", Boolean.toString(FabricWorldgenConfig.isParallelProcessingEnabled()),
                        "common", Boolean.toString(BitsAndBalanceCommon.isParallelWorldgenProcessingEnabled())
                )
        );
        failedChecks += addCheck(
                checks,
                "worldgen.azaleaGenerationRequiresWoodset",
                !FabricWorldgenConfig.isAzaleaWoodGenerationEnabled() || FabricWorldgenConfig.isAzaleaWoodsetEnabled(),
                Map.of(
                        "generationEnabled", Boolean.toString(FabricWorldgenConfig.isAzaleaWoodGenerationEnabled()),
                        "woodsetEnabled", Boolean.toString(FabricWorldgenConfig.isAzaleaWoodsetEnabled())
                )
        );

        root.add("booleanFlags", booleanFlags);
        root.add("falseFeatureLikeFlags", falseFeatureLikeFlags);
        root.add("effectiveCapabilities", effectiveCapabilities);
        root.add("checks", checks);

        JsonObject summary = new JsonObject();
        summary.addProperty("booleanFlagCount", flagCount + 14);
        summary.addProperty("falseFeatureLikeFlagCount", falseFeatureLikeFlags.size());
        summary.addProperty("failedCheckCount", failedChecks);
        root.add("summary", summary);

        Path auditPath = FabricLoader.getInstance().getGameDir().resolve("logs").resolve(AUDIT_FILE_NAME);
        try {
            Files.createDirectories(auditPath.getParent());
            Files.writeString(
                    auditPath,
                    GSON.toJson(root),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
                        LOGGER.info(
                    "Fabric boot config audit wrote {} (booleanFlags={}, falseFeatureLikeFlags={}, failedChecks={})",
                    auditPath,
                    summary.get("booleanFlagCount").getAsInt(),
                    summary.get("falseFeatureLikeFlagCount").getAsInt(),
                    summary.get("failedCheckCount").getAsInt()
            );
                        List<String> falseFeatureLikeFlagList = toStringList(falseFeatureLikeFlags);
            if (!falseFeatureLikeFlags.isEmpty()) {
                                LOGGER.info("Fabric boot config audit false feature-like flags: {}", falseFeatureLikeFlagList);
            }
            if (failedChecks > 0) {
                LOGGER.warn("Fabric boot config audit found {} failed checks; see {}", failedChecks, auditPath);
            }
                        return new BootAuditResult(
                                        auditPath,
                                        summary.get("booleanFlagCount").getAsInt(),
                                        summary.get("falseFeatureLikeFlagCount").getAsInt(),
                                        summary.get("failedCheckCount").getAsInt(),
                                        falseFeatureLikeFlagList
                        );
        } catch (IOException exception) {
            LOGGER.warn("Failed to write Fabric boot config audit {}", auditPath, exception);
                        return new BootAuditResult(auditPath, summary.get("booleanFlagCount").getAsInt(), summary.get("falseFeatureLikeFlagCount").getAsInt(), summary.get("failedCheckCount").getAsInt(), toStringList(falseFeatureLikeFlags));
        }
    }

    private static List<Field> getAuditedFields(Class<?> configClass) {
        return Arrays.stream(configClass.getFields())
                .filter(field -> field.getType() == boolean.class)
                .filter(field -> Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()))
                .filter(field -> !field.isSynthetic())
                .sorted(Comparator.comparing(Field::getName))
                .toList();
    }

    private static boolean isFeatureLikeField(String fieldName) {
        return fieldName.startsWith("enable")
                || fieldName.startsWith("disable")
                || fieldName.startsWith("suppress");
    }

    private static void addBoolean(JsonObject booleanFlags, JsonArray falseFeatureLikeFlags, String key, boolean value) {
        booleanFlags.addProperty(key, value);
        if (isFeatureLikeField(extractFieldName(key)) && !value) {
            falseFeatureLikeFlags.add(key);
        }
    }

    private static String extractFieldName(String key) {
        int dot = key.lastIndexOf('.');
        return dot >= 0 ? key.substring(dot + 1) : key;
    }

    private static int addCheck(JsonArray checks, String name, boolean pass, Map<String, String> details) {
        JsonObject check = new JsonObject();
        check.addProperty("name", name);
        check.addProperty("pass", pass);

        JsonObject detailsObject = new JsonObject();
        List<Map.Entry<String, String>> entries = new ArrayList<>(details.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        for (Map.Entry<String, String> entry : entries) {
            detailsObject.addProperty(entry.getKey(), entry.getValue());
        }
        check.add("details", detailsObject);
        checks.add(check);
        return pass ? 0 : 1;
    }

    private static List<String> toStringList(JsonArray array) {
        List<String> values = new ArrayList<>(array.size());
        array.forEach(element -> values.add(element.getAsString()));
        return values;
    }

        public record BootAuditResult(
                        Path auditPath,
                        int booleanFlagCount,
                        int falseFeatureLikeFlagCount,
                        int failedCheckCount,
                        List<String> falseFeatureLikeFlags
        ) {
        }
}