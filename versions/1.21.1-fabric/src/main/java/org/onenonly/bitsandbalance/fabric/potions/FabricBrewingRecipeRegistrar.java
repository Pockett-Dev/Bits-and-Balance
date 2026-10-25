package org.onenonly.bitsandbalance.fabric.potions;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Registers custom brewing rules with vanilla PotionBrewing so JEI can discover them.
 *
 * Fabric's actual brewing logic is handled via mixins + {@link FabricCustomBrewing}, but JEI
 * typically reads vanilla's registered brewing mixes.
 */
public final class FabricBrewingRecipeRegistrar {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitsAndBalanceCommon.MOD_ID);

    private FabricBrewingRecipeRegistrar() {
    }

    public static void registerWithMinecraftBrewing() {
        int total = FabricCustomBrewing.rules().size();
        if (total == 0) {
            return;
        }

        boolean success = tryBuilderPattern() || tryDirectAddMix();
        if (!success) {
            LOGGER.info("[{}] Unable to register custom brewing mixes into vanilla PotionBrewing; JEI may not show custom brewing recipes", BitsAndBalanceCommon.MOD_ID);
        }
    }

    private static boolean tryBuilderPattern() {
        try {
            Class<?> builderClass = Class.forName("net.minecraft.world.item.alchemy.PotionBrewing$Builder");
            Method addVanillaMixesMethod = PotionBrewing.class.getDeclaredMethod("addVanillaMixes", builderClass);
            addVanillaMixesMethod.setAccessible(true);

            Object builder = builderClass.getDeclaredConstructor().newInstance();

            Method builderAddMix = findAddMixMethod(builderClass);
            if (builderAddMix == null) {
                return false;
            }
            builderAddMix.setAccessible(true);

            int registered = 0;
            for (FabricCustomBrewing.Rule rule : FabricCustomBrewing.rules()) {
                if (tryInvokeAddMix(builderAddMix, builder, rule)) {
                    registered++;
                }
            }

            if (registered == 0) {
                return false;
            }

            addVanillaMixesMethod.invoke(null, builder);
            LOGGER.info("[{}] Registered {} custom brewing mixes via PotionBrewing.Builder", BitsAndBalanceCommon.MOD_ID, registered);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method findAddMixMethod(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals("addMix") && method.getParameterCount() == 3) {
                return method;
            }
        }
        return null;
    }

    private static boolean tryDirectAddMix() {
        try {
            Method addMix = null;
            for (Method method : PotionBrewing.class.getDeclaredMethods()) {
                if (method.getName().equals("addMix") && method.getParameterCount() == 3) {
                    addMix = method;
                    break;
                }
            }
            if (addMix == null) {
                return false;
            }
            addMix.setAccessible(true);

            int registered = 0;
            for (FabricCustomBrewing.Rule rule : FabricCustomBrewing.rules()) {
                if (tryInvokeAddMix(addMix, null, rule)) {
                    registered++;
                }
            }

            if (registered == 0) {
                return false;
            }

            LOGGER.info("[{}] Registered {} custom brewing mixes via PotionBrewing.addMix", BitsAndBalanceCommon.MOD_ID, registered);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryInvokeAddMix(Method addMix, Object receiverOrNull, FabricCustomBrewing.Rule rule) {
        try {
            Class<?>[] params = addMix.getParameterTypes();
            if (params.length != 3) {
                return false;
            }

            Object inPotionArg = resolvePotionArg(params[0], rule.inputPotion());
            Object reagentArg = resolveItemArg(params[1], rule.reagentItem());
            Object outPotionArg = resolvePotionArg(params[2], rule.resultPotion());

            if (inPotionArg == null || reagentArg == null || outPotionArg == null) {
                return false;
            }

            addMix.invoke(receiverOrNull, inPotionArg, reagentArg, outPotionArg);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object resolvePotionArg(Class<?> paramType, ResourceLocation potionId) {
        if (paramType == ResourceLocation.class) {
            return potionId;
        }

        Potion potion = BuiltInRegistries.POTION.get(potionId);
        if (potion == null) {
            return null;
        }

        if (paramType == Potion.class || paramType.isInstance(potion)) {
            return potion;
        }

        Holder<Potion> holder = BuiltInRegistries.POTION.wrapAsHolder(potion);
        if (paramType == Holder.class || Holder.class.isAssignableFrom(paramType) || paramType.isInstance(holder)) {
            return holder;
        }

        return null;
    }

    private static Object resolveItemArg(Class<?> paramType, ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null) {
            return null;
        }

        if (paramType == Item.class || paramType.isInstance(item)) {
            return item;
        }

        return null;
    }
}