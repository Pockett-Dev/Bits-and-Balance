package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

public final class RecipeCompat {
    private RecipeCompat() {
    }

    public static ItemStack assemble(Object recipe, Object input, Object registries) {
        Method oneArgMethod = null;

        for (Method method : recipe.getClass().getMethods()) {
            if (!"assemble".equals(method.getName())) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 2
                    && parameterTypes[0].isInstance(input)
                    && registries != null
                    && parameterTypes[1].isInstance(registries)) {
                try {
                    return (ItemStack) method.invoke(recipe, input, registries);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Failed to invoke two-argument assemble() on " + recipe.getClass().getName(), exception);
                }
            }

            if (parameterTypes.length == 1 && parameterTypes[0].isInstance(input)) {
                oneArgMethod = method;
            }
        }

        if (oneArgMethod != null) {
            try {
                return (ItemStack) oneArgMethod.invoke(recipe, input);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to invoke one-argument assemble() on " + recipe.getClass().getName(), exception);
            }
        }

        throw new IllegalStateException("No compatible assemble() method found on " + recipe.getClass().getName());
    }
}