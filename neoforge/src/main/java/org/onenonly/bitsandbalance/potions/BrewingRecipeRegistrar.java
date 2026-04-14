package org.onenonly.bitsandbalance.potions;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.onenonly.bitsandbalance.BitsAndBalance;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Registers custom brewing recipes with Minecraft's brewing system
 * so that JEI can automatically detect and display them.
 */
public class BrewingRecipeRegistrar {
    
    /**
     * Registers all custom brewing recipes with Minecraft's brewing system.
     * This allows JEI to automatically detect and display the recipes.
     */
    public static void registerWithMinecraftBrewing() {
        if (Boolean.getBoolean("bitsandbalance.disableReflectiveBrewingRegistration")) {
            BitsAndBalance.LOGGER.info("[{}] Reflective brewing registration disabled via system property; JEI may not show custom brewing recipes", BitsAndBalance.MODID);
            return;
        }

        BitsAndBalance.LOGGER.info("[{}] Starting brewing recipe registration...", BitsAndBalance.MODID);
        
        try {
            // Try multiple approaches for different MC versions
            boolean success = false;
            
        // Approach 1: Try bootstrap approach (likely what Apotheosis uses)
        success = tryBootstrapApproach();
        
        if (!success) {
            // Approach 2: Try direct addMix method (legacy)
            success = tryDirectAddMix();
        }
        
        if (!success) {
            // Approach 3: Try the new Builder pattern (MC 1.21.1+)
            success = tryBuilderPattern();
        }
        
        if (!success) {
            // Approach 4: Try using the mix method directly
            success = tryDirectMixMethod();
        }
        
        if (!success) {
            // Approach 5: Try accessing the brewing mixes list directly
            success = tryDirectListAccess();
        }
            
            if (!success) {
                BitsAndBalance.LOGGER.info("[{}] Reflective brewing registration unavailable; using data-driven potion_mixing only (expected on 1.21+)", BitsAndBalance.MODID);
            }
            
        } catch (Exception e) {
            BitsAndBalance.LOGGER.debug("[{}] Failed to register brewing recipes with Minecraft brewing system: {}", 
                BitsAndBalance.MODID, e.getMessage());
        }
    }
    
    /**
     * Try the new Builder pattern approach for MC 1.21.1.
     */
    private static boolean tryBuilderPattern() {
        try {
            BitsAndBalance.LOGGER.info("[{}] Trying Builder pattern approach for MC 1.21.1...", BitsAndBalance.MODID);
            
            // Get the Builder class
            Class<?> builderClass = null;
            try {
                builderClass = Class.forName("net.minecraft.world.item.alchemy.PotionBrewing$Builder");
            } catch (ClassNotFoundException e) {
                BitsAndBalance.LOGGER.debug("[{}] PotionBrewing$Builder class not found", BitsAndBalance.MODID);
                return false;
            }
            
            // Get the addVanillaMixes method
            Method addVanillaMixesMethod = PotionBrewing.class.getDeclaredMethod("addVanillaMixes", builderClass);
            addVanillaMixesMethod.setAccessible(true);
            
            // Create a new Builder instance
            Object builder = builderClass.getDeclaredConstructor().newInstance();
            
            // Get the addMix method from the Builder
            Method builderAddMixMethod = null;
            try {
                builderAddMixMethod = builderClass.getDeclaredMethod("addMix", Potion.class, Item.class, Potion.class);
            } catch (NoSuchMethodException e) {
                try {
                    builderAddMixMethod = builderClass.getDeclaredMethod("addMix", Identifier.class, Item.class, Identifier.class);
                } catch (NoSuchMethodException e2) {
                    BitsAndBalance.LOGGER.debug("[{}] No addMix method found in Builder", BitsAndBalance.MODID);
                    return false;
                }
            }
            builderAddMixMethod.setAccessible(true);
            
            int registered = 0;
            int totalRules = CustomBrewing.rules().size();
            BitsAndBalance.LOGGER.info("[{}] Processing {} custom brewing rules with Builder", BitsAndBalance.MODID, totalRules);
            
            for (CustomBrewing.Rule rule : CustomBrewing.rules()) {
                try {
                    BitsAndBalance.LOGGER.info("[{}] Processing rule: {} + {} -> {}", 
                        BitsAndBalance.MODID, rule.inputPotion(), rule.reagentItem(), rule.resultPotion());
                    
                    // Get the potions and items
                    var inputPotionRef = BuiltInRegistries.POTION.get(rule.inputPotion()).orElse(null);
                    var reagentItemRef = BuiltInRegistries.ITEM.get(rule.reagentItem()).orElse(null);
                    var resultPotionRef = BuiltInRegistries.POTION.get(rule.resultPotion()).orElse(null);

                    Potion inputPotion = inputPotionRef != null ? inputPotionRef.value() : null;
                    Item reagentItem = reagentItemRef != null ? reagentItemRef.value() : null;
                    Potion resultPotion = resultPotionRef != null ? resultPotionRef.value() : null;
                    
                    if (inputPotion == null) {
                        BitsAndBalance.LOGGER.warn("[{}] Input potion not found: {}", BitsAndBalance.MODID, rule.inputPotion());
                        continue;
                    }
                    if (reagentItem == null) {
                        BitsAndBalance.LOGGER.warn("[{}] Reagent item not found: {}", BitsAndBalance.MODID, rule.reagentItem());
                        continue;
                    }
                    if (resultPotion == null) {
                        BitsAndBalance.LOGGER.warn("[{}] Result potion not found: {}", BitsAndBalance.MODID, rule.resultPotion());
                        continue;
                    }
                    
                    // Add the mix to the builder
                    Class<?>[] paramTypes = builderAddMixMethod.getParameterTypes();
                    if (paramTypes.length == 3) {
                        if (paramTypes[0] == Potion.class && paramTypes[1] == Item.class && paramTypes[2] == Potion.class) {
                            builderAddMixMethod.invoke(builder, inputPotion, reagentItem, resultPotion);
                        } else if (paramTypes[0] == Identifier.class && paramTypes[1] == Item.class && paramTypes[2] == Identifier.class) {
                            builderAddMixMethod.invoke(builder, rule.inputPotion(), reagentItem, rule.resultPotion());
                        } else {
                            BitsAndBalance.LOGGER.debug("[{}] Unknown Builder addMix signature: {}", BitsAndBalance.MODID, 
                                java.util.Arrays.toString(paramTypes));
                            continue;
                        }
                    }
                    
                    registered++;
                    BitsAndBalance.LOGGER.debug("[{}] Added brewing recipe to builder: {} + {} -> {}", 
                        BitsAndBalance.MODID, rule.inputPotion(), rule.reagentItem(), rule.resultPotion());
                        
                } catch (Exception e) {
                    BitsAndBalance.LOGGER.debug("[{}] Failed to add brewing recipe to builder {}: {}", 
                        BitsAndBalance.MODID, rule, e.getMessage());
                }
            }
            
            if (registered > 0) {
                // Apply the builder to register all recipes
                addVanillaMixesMethod.invoke(null, builder);
                BitsAndBalance.LOGGER.info("[{}] Builder approach: Successfully registered {} out of {} custom brewing recipes", 
                    BitsAndBalance.MODID, registered, totalRules);
                return true;
            } else {
                BitsAndBalance.LOGGER.debug("[{}] No recipes were added to builder", BitsAndBalance.MODID);
                return false;
            }
            
        } catch (Exception e) {
            BitsAndBalance.LOGGER.debug("[{}] Builder pattern approach failed: {}", BitsAndBalance.MODID, e.getMessage());
            return false;
        }
    }
    
    /**
     * Try the direct addMix approach for MC 1.21.1.
     * This is the method that Apotheosis uses - direct registration with PotionBrewing.
     */
    private static boolean tryDirectAddMix() {
        try {
            BitsAndBalance.LOGGER.info("[{}] Trying direct addMix approach...", BitsAndBalance.MODID);
            
            // Debug: List all methods in PotionBrewing to see what's available
            Method[] methods = PotionBrewing.class.getDeclaredMethods();
            BitsAndBalance.LOGGER.debug("[{}] PotionBrewing methods available:", BitsAndBalance.MODID);
            for (Method method : methods) {
                BitsAndBalance.LOGGER.debug("[{}]  - {} ({})", BitsAndBalance.MODID, method.getName(), 
                    java.util.Arrays.toString(method.getParameterTypes()));
            }
            
            // Try to find the addMix method in PotionBrewing
            Method addMixMethod = null;
            
            for (Method method : methods) {
                if (method.getName().equals("addMix") && method.getParameterCount() == 3) {
                    addMixMethod = method;
                    break;
                }
            }
            
            if (addMixMethod == null) {
                BitsAndBalance.LOGGER.debug("[{}] No addMix method found in PotionBrewing", BitsAndBalance.MODID);
                
                // Try to find any method that might be used for adding brewing recipes
                for (Method method : methods) {
                    if (method.getName().toLowerCase().contains("add") || 
                        method.getName().toLowerCase().contains("register") ||
                        method.getName().toLowerCase().contains("mix")) {
                        BitsAndBalance.LOGGER.debug("[{}] Potential brewing method: {} ({})", BitsAndBalance.MODID, 
                            method.getName(), java.util.Arrays.toString(method.getParameterTypes()));
                    }
                }
                
                return false;
            }
            
            addMixMethod.setAccessible(true);
            BitsAndBalance.LOGGER.debug("[{}] Found addMix method: {}", BitsAndBalance.MODID, addMixMethod);
            
            int registered = 0;
            for (CustomBrewing.Rule rule : CustomBrewing.rules()) {
                try {
                    var inputPotion = BuiltInRegistries.POTION.get(rule.inputPotion());
                    var reagentItem = BuiltInRegistries.ITEM.get(rule.reagentItem());
                    var resultPotion = BuiltInRegistries.POTION.get(rule.resultPotion());
                    
                    if (inputPotion != null && reagentItem != null && resultPotion != null) {
                        addMixMethod.invoke(null, inputPotion, reagentItem, resultPotion);
                        registered++;
                        BitsAndBalance.LOGGER.debug("[{}] Registered brewing recipe via addMix: {} + {} -> {}", 
                            BitsAndBalance.MODID, rule.inputPotion(), rule.reagentItem(), rule.resultPotion());
                    }
                } catch (Exception e) {
                    BitsAndBalance.LOGGER.debug("[{}] Failed to register brewing recipe {}: {}", 
                        BitsAndBalance.MODID, rule, e.getMessage());
                }
            }
            
            if (registered > 0) {
                BitsAndBalance.LOGGER.info("[{}] Direct addMix approach: Successfully registered {} out of {} custom brewing recipes", 
                    BitsAndBalance.MODID, registered, CustomBrewing.rules().size());
                return true;
            } else {
                BitsAndBalance.LOGGER.debug("[{}] No recipes were registered via addMix", BitsAndBalance.MODID);
                return false;
            }
            
        } catch (Exception e) {
            BitsAndBalance.LOGGER.debug("[{}] Direct addMix approach failed: {}", BitsAndBalance.MODID, e.getMessage());
            return false;
        }
    }
    
    /**
     * Try using the mix method directly on PotionBrewing.
     * This method might be used to add recipes directly.
     */
    private static boolean tryDirectMixMethod() {
        try {
            BitsAndBalance.LOGGER.info("[{}] Trying direct mix method approach...", BitsAndBalance.MODID);
            
            // Get the mix method
            Method mixMethod = PotionBrewing.class.getDeclaredMethod("mix", 
                net.minecraft.world.item.ItemStack.class, 
                net.minecraft.world.item.ItemStack.class);
            mixMethod.setAccessible(true);
            
            int registered = 0;
            for (CustomBrewing.Rule rule : CustomBrewing.rules()) {
                try {
                    var inputPotionHolder = BuiltInRegistries.POTION.get(rule.inputPotion()).orElse(null);
                    var reagentItemHolder = BuiltInRegistries.ITEM.get(rule.reagentItem()).orElse(null);
                    var resultPotionHolder = BuiltInRegistries.POTION.get(rule.resultPotion()).orElse(null);
                    
                    if (inputPotionHolder != null && reagentItemHolder != null && resultPotionHolder != null) {
                        // Create ItemStacks for the mix method
                        var inputStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                            net.minecraft.world.item.Items.POTION, inputPotionHolder);
                        var reagentStack = new net.minecraft.world.item.ItemStack(reagentItemHolder);
                        var resultStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                            net.minecraft.world.item.Items.POTION, resultPotionHolder);
                        
                        // Debug: Check if the ItemStacks contain potion data
                        var inputPotionContents = inputStack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY);
                        var resultPotionContents = resultStack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY);
                        
                        BitsAndBalance.LOGGER.debug("[{}] Created ItemStacks - Input: {} (contents: {}), Reagent: {}, Result: {} (contents: {})", 
                            BitsAndBalance.MODID, inputStack, inputPotionContents, reagentStack, resultStack, resultPotionContents);
                        
                        // Try to call the mix method
                        BitsAndBalance.LOGGER.debug("[{}] Attempting to call mix method with input: {} and result: {}", 
                            BitsAndBalance.MODID, inputStack, resultStack);
                        
                        Object result = mixMethod.invoke(null, inputStack, resultStack);
                        BitsAndBalance.LOGGER.debug("[{}] Mix method returned: {} (type: {})", BitsAndBalance.MODID, result, result != null ? result.getClass().getSimpleName() : "null");
                        
                        // Check if the result indicates success
                        if (result != null) {
                            registered++;
                            BitsAndBalance.LOGGER.debug("[{}] Added brewing recipe via mix method: {} + {} -> {}", 
                                BitsAndBalance.MODID, rule.inputPotion(), rule.reagentItem(), rule.resultPotion());
                        } else {
                            BitsAndBalance.LOGGER.debug("[{}] Mix method returned null for recipe: {} + {} -> {}", 
                                BitsAndBalance.MODID, rule.inputPotion(), rule.reagentItem(), rule.resultPotion());
                        }
                    }
                } catch (Exception e) {
                    BitsAndBalance.LOGGER.debug("[{}] Failed to add brewing recipe via mix method {}: {}", 
                        BitsAndBalance.MODID, rule, e.getMessage());
                }
            }
            
            if (registered > 0) {
                BitsAndBalance.LOGGER.info("[{}] Direct mix method approach: Successfully registered {} out of {} custom brewing recipes", 
                    BitsAndBalance.MODID, registered, CustomBrewing.rules().size());
                return true;
            } else {
                BitsAndBalance.LOGGER.debug("[{}] No recipes were registered via mix method", BitsAndBalance.MODID);
                return false;
            }
            
        } catch (Exception e) {
            BitsAndBalance.LOGGER.debug("[{}] Direct mix method approach failed: {}", BitsAndBalance.MODID, e.getMessage());
            return false;
        }
    }
    
    /**
     * Try accessing the brewing mixes list directly.
     */
    private static boolean tryDirectListAccess() {
        try {
            BitsAndBalance.LOGGER.info("[{}] Trying direct list access approach...", BitsAndBalance.MODID);
            
            // Try to find the brewing mixes list field
            Field[] fields = PotionBrewing.class.getDeclaredFields();
            Field mixesField = null;
            
            for (Field field : fields) {
                if (field.getType() == List.class || field.getName().toLowerCase().contains("mix")) {
                    mixesField = field;
                    break;
                }
            }
            
            if (mixesField == null) {
                BitsAndBalance.LOGGER.warn("[{}] Could not find brewing mixes field", BitsAndBalance.MODID);
                return false;
            }
            
            mixesField.setAccessible(true);
            List<?> mixes = (List<?>) mixesField.get(null);
            
            BitsAndBalance.LOGGER.info("[{}] Found brewing mixes list with {} entries", BitsAndBalance.MODID, mixes.size());
            
            // This approach is more complex and would require creating the proper mix objects
            // For now, just log that we found the list
            BitsAndBalance.LOGGER.info("[{}] Direct list access approach found brewing system, but mix creation is complex", BitsAndBalance.MODID);
            
            return false; // Not fully implemented yet
            
        } catch (Exception e) {
            BitsAndBalance.LOGGER.warn("[{}] Direct list access approach failed: {}", BitsAndBalance.MODID, e.getMessage());
            return false;
        }
    }
    
    /**
     * Try registering brewing recipes via NeoForge's event system.
     */
    public static void registerViaNeoForge(net.neoforged.bus.api.Event event) {
        try {
            BitsAndBalance.LOGGER.info("[{}] Attempting to register brewing recipes via NeoForge event system...", BitsAndBalance.MODID);
            
            // Try to find NeoForge's brewing recipe registration event
            Class<?> brewingEventClass = null;
            try {
                brewingEventClass = Class.forName("net.neoforged.neoforge.event.RegisterBrewingRecipesEvent");
            } catch (ClassNotFoundException e) {
                BitsAndBalance.LOGGER.debug("[{}] RegisterBrewingRecipesEvent not found, trying alternative...", BitsAndBalance.MODID);
                try {
                    brewingEventClass = Class.forName("net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent");
                } catch (ClassNotFoundException e2) {
                    BitsAndBalance.LOGGER.debug("[{}] Alternative brewing event not found", BitsAndBalance.MODID);
                }
            }
            
            if (brewingEventClass != null && brewingEventClass.isInstance(event)) {
                BitsAndBalance.LOGGER.info("[{}] Found NeoForge brewing event, attempting registration...", BitsAndBalance.MODID);
                
                // Get the register method
                Method registerMethod = brewingEventClass.getMethod("register", 
                    net.minecraft.world.item.alchemy.Potion.class,
                    net.minecraft.world.item.Item.class, 
                    net.minecraft.world.item.alchemy.Potion.class);
                registerMethod.setAccessible(true);
                
                int registered = 0;
                for (CustomBrewing.Rule rule : CustomBrewing.rules()) {
                    try {
                        var inputPotion = BuiltInRegistries.POTION.get(rule.inputPotion());
                        var reagentItem = BuiltInRegistries.ITEM.get(rule.reagentItem());
                        var resultPotion = BuiltInRegistries.POTION.get(rule.resultPotion());
                        
                        if (inputPotion != null && reagentItem != null && resultPotion != null) {
                            registerMethod.invoke(event, inputPotion, reagentItem, resultPotion);
                            registered++;
                            BitsAndBalance.LOGGER.info("[{}] Registered brewing recipe via NeoForge: {} + {} -> {}", 
                                BitsAndBalance.MODID, rule.inputPotion(), rule.reagentItem(), rule.resultPotion());
                        }
                    } catch (Exception e) {
                        BitsAndBalance.LOGGER.debug("[{}] Failed to register brewing recipe via NeoForge {}: {}", 
                            BitsAndBalance.MODID, rule, e.getMessage());
                    }
                }
                
                BitsAndBalance.LOGGER.info("[{}] NeoForge brewing registration: Successfully registered {} out of {} custom brewing recipes", 
                    BitsAndBalance.MODID, registered, CustomBrewing.rules().size());
                return;
            }
            
            BitsAndBalance.LOGGER.info("[{}] NeoForge brewing event not found or not applicable", BitsAndBalance.MODID);
            
        } catch (Exception e) {
            BitsAndBalance.LOGGER.warn("[{}] NeoForge brewing registration failed: {}", BitsAndBalance.MODID, e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Try the bootstrap approach for MC 1.21.1.
     * This uses the bootstrap methods that are likely what Apotheosis uses.
     */
    private static boolean tryBootstrapApproach() {
        try {
            BitsAndBalance.LOGGER.info("[{}] Trying bootstrap approach...", BitsAndBalance.MODID);
            
            // Get the bootstrap method
            Method bootstrapMethod = null;
            Method[] methods = PotionBrewing.class.getDeclaredMethods();
            
            for (Method method : methods) {
                if (method.getName().equals("bootstrap") && method.getParameterCount() == 1) {
                    bootstrapMethod = method;
                    break;
                }
            }
            
            if (bootstrapMethod == null) {
                BitsAndBalance.LOGGER.warn("[{}] No bootstrap method found", BitsAndBalance.MODID);
                return false;
            }
            
            bootstrapMethod.setAccessible(true);
            BitsAndBalance.LOGGER.info("[{}] Found bootstrap method: {}", BitsAndBalance.MODID, bootstrapMethod);
            
            // Try to create a FeatureFlagSet (required parameter)
            Class<?> featureFlagSetClass = Class.forName("net.minecraft.world.flag.FeatureFlagSet");
            Object featureFlags = featureFlagSetClass.getDeclaredMethod("of").invoke(null);
            
            // Call bootstrap to initialize the brewing system and get the PotionBrewing instance
            Object potionBrewingInstance = bootstrapMethod.invoke(null, featureFlags);
            
            BitsAndBalance.LOGGER.info("[{}] Bootstrap method called successfully, got instance: {}", BitsAndBalance.MODID, potionBrewingInstance);
            
            // Now try to add our recipes using the Builder pattern
            return tryBuilderPatternAfterBootstrap(potionBrewingInstance);
            
        } catch (Exception e) {
            BitsAndBalance.LOGGER.warn("[{}] Bootstrap approach failed: {}", BitsAndBalance.MODID, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Try the Builder pattern after bootstrap initialization.
     */
    private static boolean tryBuilderPatternAfterBootstrap(Object potionBrewingInstance) {
        try {
            BitsAndBalance.LOGGER.info("[{}] Trying Builder pattern after bootstrap...", BitsAndBalance.MODID);
            BitsAndBalance.LOGGER.info("[{}] PotionBrewing instance: {}", BitsAndBalance.MODID, potionBrewingInstance);
            
            // Debug: List methods available on the PotionBrewing instance
            if (potionBrewingInstance != null) {
                Method[] instanceMethods = potionBrewingInstance.getClass().getDeclaredMethods();
                BitsAndBalance.LOGGER.info("[{}] PotionBrewing instance methods:", BitsAndBalance.MODID);
                for (Method method : instanceMethods) {
                    BitsAndBalance.LOGGER.info("[{}]  - {} ({})", BitsAndBalance.MODID, method.getName(), 
                        java.util.Arrays.toString(method.getParameterTypes()));
                }
                
                // Try to call the mix method on the instance instead of statically
                try {
                    Method instanceMixMethod = potionBrewingInstance.getClass().getDeclaredMethod("mix", 
                        net.minecraft.world.item.ItemStack.class, 
                        net.minecraft.world.item.ItemStack.class);
                    instanceMixMethod.setAccessible(true);
                    BitsAndBalance.LOGGER.info("[{}] Found instance mix method: {}", BitsAndBalance.MODID, instanceMixMethod);
                    
                    // Test with a simple recipe
                    var awkwardHolder = BuiltInRegistries.POTION.get(Identifier.fromNamespaceAndPath("minecraft", "awkward")).orElse(null);
                    var enderEye = BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath("minecraft", "ender_eye"))
                        .map(net.minecraft.core.Holder::value)
                        .orElse(null);
                    var displacementHolder = BuiltInRegistries.POTION.get(Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "displacement")).orElse(null);
                    
                    if (awkwardHolder != null && enderEye != null && displacementHolder != null) {
                        
                        if (awkwardHolder != null && displacementHolder != null) {
                            var inputStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                                net.minecraft.world.item.Items.POTION, awkwardHolder);
                            var resultStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                                net.minecraft.world.item.Items.POTION, displacementHolder);
                            
                            BitsAndBalance.LOGGER.info("[{}] Testing instance mix method with input: {} and result: {}", 
                                BitsAndBalance.MODID, inputStack, resultStack);
                            
                            Object result = instanceMixMethod.invoke(potionBrewingInstance, inputStack, resultStack);
                            BitsAndBalance.LOGGER.info("[{}] Instance mix method returned: {}", BitsAndBalance.MODID, result);
                        } else {
                            BitsAndBalance.LOGGER.warn("[{}] Failed to get potion holders for testing", BitsAndBalance.MODID);
                        }
                    }
                } catch (Exception e) {
                    BitsAndBalance.LOGGER.warn("[{}] Failed to test instance mix method: {}", BitsAndBalance.MODID, e.getMessage());
                }
            }
            
            // Get the Builder class
            Class<?> builderClass = Class.forName("net.minecraft.world.item.alchemy.PotionBrewing$Builder");
            
            // Try multiple approaches to create a builder
            Object builder = null;
            
            // Approach 1: Try constructor with reflection
            try {
                java.lang.reflect.Constructor<?> constructor = builderClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                builder = constructor.newInstance();
                BitsAndBalance.LOGGER.info("[{}] Created builder via constructor", BitsAndBalance.MODID);
            } catch (Exception e) {
                BitsAndBalance.LOGGER.debug("[{}] Constructor approach failed: {}", BitsAndBalance.MODID, e.getMessage());
            }
            
            // Approach 2: Try static factory methods
            if (builder == null) {
                Method[] builderMethods = builderClass.getDeclaredMethods();
                for (Method method : builderMethods) {
                    if (method.getName().equals("create") || method.getName().equals("builder") || 
                        method.getName().equals("newBuilder") || method.getName().equals("of")) {
                        try {
                            method.setAccessible(true);
                            builder = method.invoke(null);
                            BitsAndBalance.LOGGER.info("[{}] Created builder via static method: {}", BitsAndBalance.MODID, method.getName());
                            break;
                        } catch (Exception e) {
                            BitsAndBalance.LOGGER.debug("[{}] Static method {} failed: {}", BitsAndBalance.MODID, method.getName(), e.getMessage());
                        }
                    }
                }
            }
            
            // Approach 3: Try instance methods on PotionBrewing
            if (builder == null && potionBrewingInstance != null) {
                Method[] instanceMethods = potionBrewingInstance.getClass().getDeclaredMethods();
                for (Method method : instanceMethods) {
                    if (method.getName().toLowerCase().contains("builder") || 
                        method.getName().toLowerCase().contains("create")) {
                        try {
                            method.setAccessible(true);
                            Object result = method.invoke(potionBrewingInstance);
                            if (result != null && builderClass.isInstance(result)) {
                                builder = result;
                                BitsAndBalance.LOGGER.info("[{}] Created builder via instance method: {}", BitsAndBalance.MODID, method.getName());
                                break;
                            }
                        } catch (Exception e) {
                            BitsAndBalance.LOGGER.debug("[{}] Instance method {} failed: {}", BitsAndBalance.MODID, method.getName(), e.getMessage());
                        }
                    }
                }
            }
            
            if (builder == null) {
                BitsAndBalance.LOGGER.warn("[{}] No builder creation method found", BitsAndBalance.MODID);
                return false;
            }
            
            // Get the addMix method from the Builder
            Method addMixMethod = builderClass.getDeclaredMethod("addMix", 
                net.minecraft.world.item.alchemy.Potion.class,
                net.minecraft.world.item.Item.class, 
                net.minecraft.world.item.alchemy.Potion.class);
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
                        BitsAndBalance.LOGGER.info("[{}] Added brewing recipe to builder: {} + {} -> {}", 
                            BitsAndBalance.MODID, rule.inputPotion(), rule.reagentItem(), rule.resultPotion());
                    }
                } catch (Exception e) {
                    BitsAndBalance.LOGGER.debug("[{}] Failed to add brewing recipe to builder {}: {}", 
                        BitsAndBalance.MODID, rule, e.getMessage());
                }
            }
            
            if (registered > 0) {
                // Apply the builder using addVanillaMixes
                Method addVanillaMixesMethod = PotionBrewing.class.getDeclaredMethod("addVanillaMixes", builderClass);
                addVanillaMixesMethod.setAccessible(true);
                addVanillaMixesMethod.invoke(null, builder);
                
                BitsAndBalance.LOGGER.info("[{}] Builder approach after bootstrap: Successfully registered {} out of {} custom brewing recipes", 
                    BitsAndBalance.MODID, registered, CustomBrewing.rules().size());
                return true;
            } else {
                BitsAndBalance.LOGGER.warn("[{}] No recipes were added to builder", BitsAndBalance.MODID);
                return false;
            }
            
        } catch (Exception e) {
            BitsAndBalance.LOGGER.warn("[{}] Builder pattern after bootstrap failed: {}", BitsAndBalance.MODID, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}