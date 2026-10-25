package org.onenonly.bitsandbalance.potions;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Registers custom brewing recipes with Minecraft's brewing system
 * so that JEI can automatically detect and display them.
 */
public final class BrewingRecipeRegistrar {
    private static final Logger LOG = LogUtils.getLogger();

    private BrewingRecipeRegistrar() {
    }

    public static void registerWithMinecraftBrewing() {
        if (Boolean.getBoolean("bitsandbalance.disableReflectiveBrewingRegistration")) {
            LOG.info("[{}] Reflective brewing registration disabled via system property; JEI may not show custom brewing recipes", BitsAndBalanceCommon.MOD_ID);
            return;
        }

        LOG.info("[{}] Starting brewing recipe registration...", BitsAndBalanceCommon.MOD_ID);

        try {
            boolean success = false;
            success = tryBootstrapApproach();
            if (!success) {
                success = tryDirectAddMix();
            }
            if (!success) {
                success = tryBuilderPattern();
            }
            if (!success) {
                success = tryDirectMixMethod();
            }
            if (!success) {
                success = tryDirectListAccess();
            }

            if (!success) {
                LOG.info("[{}] Reflective brewing registration unavailable; using data-driven potion_mixing only (expected on 1.21+)", BitsAndBalanceCommon.MOD_ID);
            }
        } catch (Exception e) {
            LOG.debug("[{}] Failed to register brewing recipes with Minecraft brewing system: {}", BitsAndBalanceCommon.MOD_ID, e.getMessage());
        }
    }

    private static boolean tryBuilderPattern() {
        try {
            LOG.info("[{}] Trying Builder pattern approach for MC 1.21.1...", BitsAndBalanceCommon.MOD_ID);

            Class<?> builderClass;
            try {
                builderClass = Class.forName("net.minecraft.world.item.alchemy.PotionBrewing$Builder");
            } catch (ClassNotFoundException e) {
                LOG.debug("[{}] PotionBrewing$Builder class not found", BitsAndBalanceCommon.MOD_ID);
                return false;
            }

            Method addVanillaMixesMethod = PotionBrewing.class.getDeclaredMethod("addVanillaMixes", builderClass);
            addVanillaMixesMethod.setAccessible(true);

            Object builder = builderClass.getDeclaredConstructor().newInstance();

            Method builderAddMixMethod;
            try {
                builderAddMixMethod = builderClass.getDeclaredMethod("addMix", Potion.class, Item.class, Potion.class);
            } catch (NoSuchMethodException e) {
                try {
                    builderAddMixMethod = builderClass.getDeclaredMethod("addMix", Identifier.class, Item.class, Identifier.class);
                } catch (NoSuchMethodException e2) {
                    LOG.debug("[{}] No addMix method found in Builder", BitsAndBalanceCommon.MOD_ID);
                    return false;
                }
            }
            builderAddMixMethod.setAccessible(true);

            int registered = 0;
            int totalRules = CustomBrewing.rules().size();
            LOG.info("[{}] Processing {} custom brewing rules with Builder", BitsAndBalanceCommon.MOD_ID, totalRules);

            for (CustomBrewing.Rule rule : CustomBrewing.rules()) {
                try {
                    LOG.info("[{}] Processing rule: {} + {} -> {}", BitsAndBalanceCommon.MOD_ID, rule.inputPotion(), rule.reagentItem(), rule.resultPotion());

                    var inputPotionRef = BuiltInRegistries.POTION.get(rule.inputPotion()).orElse(null);
                    var reagentItemRef = BuiltInRegistries.ITEM.get(rule.reagentItem()).orElse(null);
                    var resultPotionRef = BuiltInRegistries.POTION.get(rule.resultPotion()).orElse(null);

                    Potion inputPotion = inputPotionRef != null ? inputPotionRef.value() : null;
                    Item reagentItem = reagentItemRef != null ? reagentItemRef.value() : null;
                    Potion resultPotion = resultPotionRef != null ? resultPotionRef.value() : null;

                    if (inputPotion == null) {
                        LOG.warn("[{}] Input potion not found: {}", BitsAndBalanceCommon.MOD_ID, rule.inputPotion());
                        continue;
                    }
                    if (reagentItem == null) {
                        LOG.warn("[{}] Reagent item not found: {}", BitsAndBalanceCommon.MOD_ID, rule.reagentItem());
                        continue;
                    }
                    if (resultPotion == null) {
                        LOG.warn("[{}] Result potion not found: {}", BitsAndBalanceCommon.MOD_ID, rule.resultPotion());
                        continue;
                    }

                    Class<?>[] paramTypes = builderAddMixMethod.getParameterTypes();
                    if (paramTypes.length == 3) {
                        if (paramTypes[0] == Potion.class && paramTypes[1] == Item.class && paramTypes[2] == Potion.class) {
                            builderAddMixMethod.invoke(builder, inputPotion, reagentItem, resultPotion);
                        } else if (paramTypes[0] == Identifier.class && paramTypes[1] == Item.class && paramTypes[2] == Identifier.class) {
                            builderAddMixMethod.invoke(builder, rule.inputPotion(), reagentItem, rule.resultPotion());
                        } else {
                            LOG.debug("[{}] Unknown Builder addMix signature: {}", BitsAndBalanceCommon.MOD_ID, java.util.Arrays.toString(paramTypes));
                            continue;
                        }
                    }

                    registered++;
                    LOG.debug("[{}] Added brewing recipe to builder: {} + {} -> {}", BitsAndBalanceCommon.MOD_ID, rule.inputPotion(), rule.reagentItem(), rule.resultPotion());
                } catch (Exception e) {
                    LOG.debug("[{}] Failed to add brewing recipe to builder {}: {}", BitsAndBalanceCommon.MOD_ID, rule, e.getMessage());
                }
            }

            if (registered > 0) {
                addVanillaMixesMethod.invoke(null, builder);
                LOG.info("[{}] Builder approach: Successfully registered {} out of {} custom brewing recipes", BitsAndBalanceCommon.MOD_ID, registered, totalRules);
                return true;
            }

            LOG.debug("[{}] No recipes were added to builder", BitsAndBalanceCommon.MOD_ID);
            return false;
        } catch (Exception e) {
            LOG.debug("[{}] Builder pattern approach failed: {}", BitsAndBalanceCommon.MOD_ID, e.getMessage());
            return false;
        }
    }

    private static boolean tryDirectAddMix() {
        try {
            LOG.info("[{}] Trying direct addMix approach...", BitsAndBalanceCommon.MOD_ID);
            Method[] methods = PotionBrewing.class.getDeclaredMethods();
            LOG.debug("[{}] PotionBrewing methods available:", BitsAndBalanceCommon.MOD_ID);
            for (Method method : methods) {
                LOG.debug("[{}]  - {} ({})", BitsAndBalanceCommon.MOD_ID, method.getName(), java.util.Arrays.toString(method.getParameterTypes()));
            }

            Method addMixMethod = null;
            for (Method method : methods) {
                if (method.getName().equals("addMix") && method.getParameterCount() == 3) {
                    addMixMethod = method;
                    break;
                }
            }

            if (addMixMethod == null) {
                LOG.debug("[{}] No addMix method found in PotionBrewing", BitsAndBalanceCommon.MOD_ID);
                for (Method method : methods) {
                    if (method.getName().toLowerCase().contains("add") || method.getName().toLowerCase().contains("register") || method.getName().toLowerCase().contains("mix")) {
                        LOG.debug("[{}] Potential brewing method: {} ({})", BitsAndBalanceCommon.MOD_ID, method.getName(), java.util.Arrays.toString(method.getParameterTypes()));
                    }
                }
                return false;
            }

            addMixMethod.setAccessible(true);
            LOG.debug("[{}] Found addMix method: {}", BitsAndBalanceCommon.MOD_ID, addMixMethod);

            int registered = 0;
            for (CustomBrewing.Rule rule : CustomBrewing.rules()) {
                try {
                    var inputPotion = BuiltInRegistries.POTION.get(rule.inputPotion());
                    var reagentItem = BuiltInRegistries.ITEM.get(rule.reagentItem());
                    var resultPotion = BuiltInRegistries.POTION.get(rule.resultPotion());

                    if (inputPotion != null && reagentItem != null && resultPotion != null) {
                        addMixMethod.invoke(null, inputPotion, reagentItem, resultPotion);
                        registered++;
                        LOG.debug("[{}] Registered brewing recipe via addMix: {} + {} -> {}", BitsAndBalanceCommon.MOD_ID, rule.inputPotion(), rule.reagentItem(), rule.resultPotion());
                    }
                } catch (Exception e) {
                    LOG.debug("[{}] Failed to register brewing recipe {}: {}", BitsAndBalanceCommon.MOD_ID, rule, e.getMessage());
                }
            }

            if (registered > 0) {
                LOG.info("[{}] Direct addMix approach: Successfully registered {} out of {} custom brewing recipes", BitsAndBalanceCommon.MOD_ID, registered, CustomBrewing.rules().size());
                return true;
            }

            LOG.debug("[{}] No recipes were registered via addMix", BitsAndBalanceCommon.MOD_ID);
            return false;
        } catch (Exception e) {
            LOG.debug("[{}] Direct addMix approach failed: {}", BitsAndBalanceCommon.MOD_ID, e.getMessage());
            return false;
        }
    }

    private static boolean tryDirectMixMethod() {
        try {
            LOG.info("[{}] Trying direct mix method approach...", BitsAndBalanceCommon.MOD_ID);
            Method mixMethod = PotionBrewing.class.getDeclaredMethod("mix", net.minecraft.world.item.ItemStack.class, net.minecraft.world.item.ItemStack.class);
            mixMethod.setAccessible(true);

            int registered = 0;
            for (CustomBrewing.Rule rule : CustomBrewing.rules()) {
                try {
                    var inputPotionHolder = BuiltInRegistries.POTION.get(rule.inputPotion()).orElse(null);
                    var reagentItemHolder = BuiltInRegistries.ITEM.get(rule.reagentItem()).orElse(null);
                    var resultPotionHolder = BuiltInRegistries.POTION.get(rule.resultPotion()).orElse(null);

                    if (inputPotionHolder != null && reagentItemHolder != null && resultPotionHolder != null) {
                        var inputStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION, inputPotionHolder);
                        var reagentStack = new net.minecraft.world.item.ItemStack(reagentItemHolder);
                        var resultStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION, resultPotionHolder);

                        var inputPotionContents = inputStack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY);
                        var resultPotionContents = resultStack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY);

                        LOG.debug("[{}] Created ItemStacks - Input: {} (contents: {}), Reagent: {}, Result: {} (contents: {})", BitsAndBalanceCommon.MOD_ID, inputStack, inputPotionContents, reagentStack, resultStack, resultPotionContents);
                        LOG.debug("[{}] Attempting to call mix method with input: {} and result: {}", BitsAndBalanceCommon.MOD_ID, inputStack, resultStack);
                        Object result = mixMethod.invoke(null, inputStack, resultStack);
                        LOG.debug("[{}] Mix method returned: {} (type: {})", BitsAndBalanceCommon.MOD_ID, result, result != null ? result.getClass().getSimpleName() : "null");

                        if (result != null) {
                            registered++;
                            LOG.debug("[{}] Added brewing recipe via mix method: {} + {} -> {}", BitsAndBalanceCommon.MOD_ID, rule.inputPotion(), rule.reagentItem(), rule.resultPotion());
                        } else {
                            LOG.debug("[{}] Mix method returned null for recipe: {} + {} -> {}", BitsAndBalanceCommon.MOD_ID, rule.inputPotion(), rule.reagentItem(), rule.resultPotion());
                        }
                    }
                } catch (Exception e) {
                    LOG.debug("[{}] Failed to add brewing recipe via mix method {}: {}", BitsAndBalanceCommon.MOD_ID, rule, e.getMessage());
                }
            }

            if (registered > 0) {
                LOG.info("[{}] Direct mix method approach: Successfully registered {} out of {} custom brewing recipes", BitsAndBalanceCommon.MOD_ID, registered, CustomBrewing.rules().size());
                return true;
            }

            LOG.debug("[{}] No recipes were registered via mix method", BitsAndBalanceCommon.MOD_ID);
            return false;
        } catch (Exception e) {
            LOG.debug("[{}] Direct mix method approach failed: {}", BitsAndBalanceCommon.MOD_ID, e.getMessage());
            return false;
        }
    }

    private static boolean tryDirectListAccess() {
        try {
            LOG.info("[{}] Trying direct list access approach...", BitsAndBalanceCommon.MOD_ID);

            Field[] fields = PotionBrewing.class.getDeclaredFields();
            Field mixesField = null;
            for (Field field : fields) {
                if (field.getType() == List.class || field.getName().toLowerCase().contains("mix")) {
                    mixesField = field;
                    break;
                }
            }

            if (mixesField == null) {
                LOG.warn("[{}] Could not find brewing mixes field", BitsAndBalanceCommon.MOD_ID);
                return false;
            }

            mixesField.setAccessible(true);
            List<?> mixes = (List<?>) mixesField.get(null);

            LOG.info("[{}] Found brewing mixes list with {} entries", BitsAndBalanceCommon.MOD_ID, mixes.size());
            LOG.info("[{}] Direct list access approach found brewing system, but mix creation is complex", BitsAndBalanceCommon.MOD_ID);
            return false;
        } catch (Exception e) {
            LOG.warn("[{}] Direct list access approach failed: {}", BitsAndBalanceCommon.MOD_ID, e.getMessage());
            return false;
        }
    }

    private static boolean tryBootstrapApproach() {
        try {
            LOG.info("[{}] Trying bootstrap approach...", BitsAndBalanceCommon.MOD_ID);

            Method bootstrapMethod = null;
            Method[] methods = PotionBrewing.class.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("bootstrap") && method.getParameterCount() == 1) {
                    bootstrapMethod = method;
                    break;
                }
            }

            if (bootstrapMethod == null) {
                LOG.warn("[{}] No bootstrap method found", BitsAndBalanceCommon.MOD_ID);
                return false;
            }

            bootstrapMethod.setAccessible(true);
            LOG.info("[{}] Found bootstrap method: {}", BitsAndBalanceCommon.MOD_ID, bootstrapMethod);

            Class<?> featureFlagSetClass = Class.forName("net.minecraft.world.flag.FeatureFlagSet");
            Object featureFlags = featureFlagSetClass.getDeclaredMethod("of").invoke(null);
            Object potionBrewingInstance = bootstrapMethod.invoke(null, featureFlags);

            LOG.info("[{}] Bootstrap method called successfully, got instance: {}", BitsAndBalanceCommon.MOD_ID, potionBrewingInstance);
            return tryBuilderPatternAfterBootstrap(potionBrewingInstance);
        } catch (Exception e) {
            LOG.warn("[{}] Bootstrap approach failed: {}", BitsAndBalanceCommon.MOD_ID, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean tryBuilderPatternAfterBootstrap(Object potionBrewingInstance) {
        try {
            LOG.info("[{}] Trying Builder pattern after bootstrap...", BitsAndBalanceCommon.MOD_ID);
            LOG.info("[{}] PotionBrewing instance: {}", BitsAndBalanceCommon.MOD_ID, potionBrewingInstance);

            if (potionBrewingInstance != null) {
                Method[] instanceMethods = potionBrewingInstance.getClass().getDeclaredMethods();
                LOG.info("[{}] PotionBrewing instance methods:", BitsAndBalanceCommon.MOD_ID);
                for (Method method : instanceMethods) {
                    LOG.info("[{}]  - {} ({})", BitsAndBalanceCommon.MOD_ID, method.getName(), java.util.Arrays.toString(method.getParameterTypes()));
                }

                try {
                    Method instanceMixMethod = potionBrewingInstance.getClass().getDeclaredMethod("mix", net.minecraft.world.item.ItemStack.class, net.minecraft.world.item.ItemStack.class);
                    instanceMixMethod.setAccessible(true);
                    LOG.info("[{}] Found instance mix method: {}", BitsAndBalanceCommon.MOD_ID, instanceMixMethod);

                    var awkwardHolder = BuiltInRegistries.POTION.get(Identifier.fromNamespaceAndPath("minecraft", "awkward")).orElse(null);
                    var enderEye = BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath("minecraft", "ender_eye")).map(net.minecraft.core.Holder::value).orElse(null);
                    var displacementHolder = BuiltInRegistries.POTION.get(Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "displacement")).orElse(null);

                    if (awkwardHolder != null && enderEye != null && displacementHolder != null) {
                        var inputStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION, awkwardHolder);
                        var resultStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION, displacementHolder);
                        LOG.info("[{}] Testing instance mix method with input: {} and result: {}", BitsAndBalanceCommon.MOD_ID, inputStack, resultStack);
                        Object result = instanceMixMethod.invoke(potionBrewingInstance, inputStack, resultStack);
                        LOG.info("[{}] Instance mix method returned: {}", BitsAndBalanceCommon.MOD_ID, result);
                    }
                } catch (Exception e) {
                    LOG.warn("[{}] Failed to test instance mix method: {}", BitsAndBalanceCommon.MOD_ID, e.getMessage());
                }
            }

            Class<?> builderClass = Class.forName("net.minecraft.world.item.alchemy.PotionBrewing$Builder");
            Object builder = null;

            try {
                var constructor = builderClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                builder = constructor.newInstance();
                LOG.info("[{}] Created builder via constructor", BitsAndBalanceCommon.MOD_ID);
            } catch (Exception e) {
                LOG.debug("[{}] Constructor approach failed: {}", BitsAndBalanceCommon.MOD_ID, e.getMessage());
            }

            if (builder == null) {
                Method[] builderMethods = builderClass.getDeclaredMethods();
                for (Method method : builderMethods) {
                    if (method.getName().equals("create") || method.getName().equals("builder") || method.getName().equals("newBuilder") || method.getName().equals("of")) {
                        try {
                            method.setAccessible(true);
                            builder = method.invoke(null);
                            LOG.info("[{}] Created builder via static method: {}", BitsAndBalanceCommon.MOD_ID, method.getName());
                            break;
                        } catch (Exception e) {
                            LOG.debug("[{}] Static method {} failed: {}", BitsAndBalanceCommon.MOD_ID, method.getName(), e.getMessage());
                        }
                    }
                }
            }

            if (builder == null && potionBrewingInstance != null) {
                Method[] instanceMethods = potionBrewingInstance.getClass().getDeclaredMethods();
                for (Method method : instanceMethods) {
                    if (method.getName().toLowerCase().contains("builder") || method.getName().toLowerCase().contains("create")) {
                        try {
                            method.setAccessible(true);
                            Object result = method.invoke(potionBrewingInstance);
                            if (result != null && builderClass.isInstance(result)) {
                                builder = result;
                                LOG.info("[{}] Created builder via instance method: {}", BitsAndBalanceCommon.MOD_ID, method.getName());
                                break;
                            }
                        } catch (Exception e) {
                            LOG.debug("[{}] Instance method {} failed: {}", BitsAndBalanceCommon.MOD_ID, method.getName(), e.getMessage());
                        }
                    }
                }
            }

            if (builder == null) {
                LOG.warn("[{}] No builder creation method found", BitsAndBalanceCommon.MOD_ID);
                return false;
            }

            Method addMixMethod = builderClass.getDeclaredMethod("addMix", net.minecraft.world.item.alchemy.Potion.class, net.minecraft.world.item.Item.class, net.minecraft.world.item.alchemy.Potion.class);
            addMixMethod.setAccessible(true);

            int registered = 0;
            for (CustomBrewing.Rule rule : CustomBrewing.rules()) {
                try {
                    var inputPotion = BuiltInRegistries.POTION.get(rule.inputPotion());
                    var reagentItem = BuiltInRegistries.ITEM.get(rule.reagentItem());
                    var resultPotion = BuiltInRegistries.POTION.get(rule.resultPotion());

                    if (inputPotion != null && reagentItem != null && resultPotion != null) {
                        addMixMethod.invoke(builder, inputPotion, reagentItem, resultPotion);
                        registered++;
                        LOG.info("[{}] Added brewing recipe to builder: {} + {} -> {}", BitsAndBalanceCommon.MOD_ID, rule.inputPotion(), rule.reagentItem(), rule.resultPotion());
                    }
                } catch (Exception e) {
                    LOG.debug("[{}] Failed to add brewing recipe to builder {}: {}", BitsAndBalanceCommon.MOD_ID, rule, e.getMessage());
                }
            }

            if (registered > 0) {
                Method addVanillaMixesMethod = PotionBrewing.class.getDeclaredMethod("addVanillaMixes", builderClass);
                addVanillaMixesMethod.setAccessible(true);
                addVanillaMixesMethod.invoke(null, builder);
                LOG.info("[{}] Builder approach after bootstrap: Successfully registered {} out of {} custom brewing recipes", BitsAndBalanceCommon.MOD_ID, registered, CustomBrewing.rules().size());
                return true;
            }

            LOG.warn("[{}] No recipes were added to builder", BitsAndBalanceCommon.MOD_ID);
            return false;
        } catch (Exception e) {
            LOG.warn("[{}] Builder pattern after bootstrap failed: {}", BitsAndBalanceCommon.MOD_ID, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}