package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(net.minecraft.stats.ServerRecipeBook.class)
public abstract class ServerRecipeBookMixin {
    private static final Logger BITSANDBALANCE$LOGGER = LogUtils.getLogger();
    private static final Identifier BITSANDBALANCE$REBORNCORE_PADDED_ID = Identifier.fromNamespaceAndPath("reborncore", "padded");
    private static final Set<Identifier> BITSANDBALANCE$WARNED_UNSAFE_RECIPE_BOOK_ENTRIES = ConcurrentHashMap.newKeySet();

    @Shadow @Final private Set<ResourceKey<?>> known;
    @Shadow @Final private Set<ResourceKey<?>> highlight;
    @Shadow @Final private ServerRecipeBook.DisplayResolver displayResolver;

    @Redirect(
        method = "loadRecipes",
        at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V"
        )
    )
    private void bitsandbalance$suppressRemovedRecipeWarning(Logger logger, String message, Object recipeKey) {
        if (org.onenonly.bitsandbalance.common.client.FarChunkErrorFilter.suppressesRemovedRecipeWarning(message, recipeKey)) {
            return;
        }
        logger.error(message, recipeKey);
    }

    @ModifyVariable(method = "addRecipes", at = @At("HEAD"), argsOnly = true)
    private Collection<?> bitsandbalance$filterUnsafeRecipeBookAddRecipes(Collection<?> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return recipes;
        }

        var filtered = recipes.stream()
                .filter(recipe -> !bitsandbalance$isUnsafeRecipeBookEntry(recipe))
                .toList();
        return filtered.size() == recipes.size() ? recipes : filtered;
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$skipUnsafeRecipeBookRecipe(ResourceKey<?> recipeKey, CallbackInfo ci) {
        if (bitsandbalance$isUnsafeRecipeBookKey(recipeKey)) {
            ci.cancel();
        }
    }

    @Inject(method = "sendInitialRecipeBook", at = @At("HEAD"))
    private void bitsandbalance$stripUnsafeRecipeBookRecipes(ServerPlayer player, CallbackInfo ci) {
        int removedKnown = bitsandbalance$removeUnsafeRecipeBookEntries(this.known);
        int removedHighlight = bitsandbalance$removeUnsafeRecipeBookEntries(this.highlight);
        int removedTotal = removedKnown + removedHighlight;
        if (removedTotal > 0) {
            BITSANDBALANCE$LOGGER.warn("[bitsandbalance] Removed {} unsafe recipe-book entries before syncing recipes to {}", removedTotal, player.getName().getString());
        }
    }

    private boolean bitsandbalance$isUnsafeRecipeBookEntry(Object recipe) {
        if (recipe instanceof ResourceKey<?> recipeKey) {
            return bitsandbalance$isUnsafeRecipeBookKey(recipeKey);
        }
        if (recipe instanceof RecipeHolder<?> holder) {
            return bitsandbalance$isUnsafeRecipeBookKey(holder.id());
        }
        return false;
    }

    private boolean bitsandbalance$isUnsafeRecipeBookKey(ResourceKey<?> recipeKey) {
        if (recipeKey == null) {
            return false;
        }
        if (bitsandbalance$shouldForceBlockRecipeBookKey(recipeKey)) {
            bitsandbalance$warnUnsafeRecipeBookKey(recipeKey, "known incompatible Reborn recipe-book entry");
            return true;
        }
        return !bitsandbalance$canSafelyResolveRecipeBookDisplays(recipeKey);
    }

    private boolean bitsandbalance$canSafelyResolveRecipeBookDisplays(ResourceKey<?> recipeKey) {
        List<RecipeDisplayEntry> entries = new ArrayList<>();
        try {
            this.displayResolver.displaysForRecipe(bitsandbalance$recipeKey(recipeKey), entries::add);
        } catch (Throwable throwable) {
            bitsandbalance$warnUnsafeRecipeBookKey(recipeKey, "failed to resolve recipe displays: " + throwable);
            return false;
        }

        for (RecipeDisplayEntry entry : entries) {
            if (!bitsandbalance$isSafeRecipeDisplayEntry(entry)) {
                bitsandbalance$warnUnsafeRecipeBookKey(recipeKey, "contains an unregistered recipe-book display");
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<Recipe<?>> bitsandbalance$recipeKey(ResourceKey<?> recipeKey) {
        return (ResourceKey<Recipe<?>>) recipeKey;
    }

    private static boolean bitsandbalance$isSafeRecipeDisplayEntry(RecipeDisplayEntry entry) {
        if (entry == null || !bitsandbalance$isRegisteredRecipeBookCategory(entry.category())) {
            return false;
        }
        return bitsandbalance$isSafeRecipeDisplay(entry.display());
    }

    private static boolean bitsandbalance$isRegisteredRecipeBookCategory(RecipeBookCategory category) {
        return category != null && BuiltInRegistries.RECIPE_BOOK_CATEGORY.getResourceKey(category).isPresent();
    }

    private static boolean bitsandbalance$isSafeRecipeDisplay(RecipeDisplay display) {
        if (display == null) {
            return false;
        }
        try {
            var type = display.type();
            if (type == null || BuiltInRegistries.RECIPE_DISPLAY.getResourceKey(type).isEmpty()) {
                return false;
            }
            if (!bitsandbalance$isSafeSlotDisplay(display.result(), 0) || !bitsandbalance$isSafeSlotDisplay(display.craftingStation(), 0)) {
                return false;
            }
            if (display instanceof ShapedCraftingRecipeDisplay shaped) {
                return bitsandbalance$areSafeSlotDisplays(shaped.ingredients());
            }
            if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
                return bitsandbalance$areSafeSlotDisplays(shapeless.ingredients());
            }
            if (display instanceof FurnaceRecipeDisplay furnace) {
                return bitsandbalance$isSafeSlotDisplay(furnace.ingredient(), 0)
                        && bitsandbalance$isSafeSlotDisplay(furnace.fuel(), 0);
            }
            if (display instanceof StonecutterRecipeDisplay stonecutter) {
                return bitsandbalance$isSafeSlotDisplay(stonecutter.input(), 0);
            }
            if (display instanceof SmithingRecipeDisplay smithing) {
                return bitsandbalance$isSafeSlotDisplay(smithing.template(), 0)
                        && bitsandbalance$isSafeSlotDisplay(smithing.base(), 0)
                        && bitsandbalance$isSafeSlotDisplay(smithing.addition(), 0);
            }
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static boolean bitsandbalance$areSafeSlotDisplays(List<SlotDisplay> slots) {
        if (slots == null) {
            return false;
        }
        for (SlotDisplay slot : slots) {
            if (!bitsandbalance$isSafeSlotDisplay(slot, 0)) {
                return false;
            }
        }
        return true;
    }

    private static boolean bitsandbalance$isSafeSlotDisplay(SlotDisplay slot, int depth) {
        if (slot == null || depth > 8) {
            return false;
        }
        try {
            var type = slot.type();
            if (type == null || BuiltInRegistries.SLOT_DISPLAY.getResourceKey(type).isEmpty()) {
                return false;
            }
            if (slot instanceof SlotDisplay.Composite composite) {
                return bitsandbalance$areSafeNestedSlotDisplays(composite.contents(), depth + 1);
            }
            if (slot instanceof SlotDisplay.WithRemainder withRemainder) {
                return bitsandbalance$isSafeSlotDisplay(withRemainder.input(), depth + 1)
                        && bitsandbalance$isSafeSlotDisplay(withRemainder.remainder(), depth + 1);
            }
            if (slot instanceof SlotDisplay.OnlyWithComponent onlyWithComponent) {
                return bitsandbalance$isSafeSlotDisplay(onlyWithComponent.source(), depth + 1);
            }
            if (slot instanceof SlotDisplay.DyedSlotDemo dyedSlotDemo) {
                return bitsandbalance$isSafeSlotDisplay(dyedSlotDemo.dye(), depth + 1)
                        && bitsandbalance$isSafeSlotDisplay(dyedSlotDemo.target(), depth + 1);
            }
            if (slot instanceof SlotDisplay.WithAnyPotion withAnyPotion) {
                return bitsandbalance$isSafeSlotDisplay(withAnyPotion.display(), depth + 1);
            }
            if (slot instanceof SlotDisplay.SmithingTrimDemoSlotDisplay trimDemo) {
                return bitsandbalance$isSafeSlotDisplay(trimDemo.base(), depth + 1)
                        && bitsandbalance$isSafeSlotDisplay(trimDemo.material(), depth + 1);
            }
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static boolean bitsandbalance$areSafeNestedSlotDisplays(List<SlotDisplay> slots, int depth) {
        if (slots == null) {
            return false;
        }
        for (SlotDisplay slot : slots) {
            if (!bitsandbalance$isSafeSlotDisplay(slot, depth)) {
                return false;
            }
        }
        return true;
    }

    private static boolean bitsandbalance$shouldFilterBrokenRebornRecipes() {
        return FabricLoader.getInstance().isModLoaded("techreborn")
                && FabricLoader.getInstance().isModLoaded("reborncore")
                && !BuiltInRegistries.RECIPE_SERIALIZER.containsKey(BITSANDBALANCE$REBORNCORE_PADDED_ID);
    }

    private static boolean bitsandbalance$shouldForceBlockRecipeBookKey(ResourceKey<?> recipeKey) {
        return bitsandbalance$shouldFilterBrokenRebornRecipes() && bitsandbalance$isBrokenRebornRecipe(recipeKey);
    }

    private static boolean bitsandbalance$isBrokenRebornRecipe(ResourceKey<?> recipeKey) {
        if (recipeKey == null) {
            return false;
        }

        String namespace = recipeKey.identifier().getNamespace();
        return namespace.equals("techreborn")
                || namespace.equals("reborncore")
                || namespace.equals("team_reborn_energy");
    }

    private int bitsandbalance$removeUnsafeRecipeBookEntries(Set<ResourceKey<?>> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return 0;
        }

        int before = recipes.size();
        recipes.removeIf(this::bitsandbalance$isUnsafeRecipeBookKey);
        return before - recipes.size();
    }

    private static void bitsandbalance$warnUnsafeRecipeBookKey(ResourceKey<?> recipeKey, String reason) {
        Identifier id = recipeKey.identifier();
        if (BITSANDBALANCE$WARNED_UNSAFE_RECIPE_BOOK_ENTRIES.add(id)) {
            BITSANDBALANCE$LOGGER.warn("[bitsandbalance] Filtering recipe-book entry {} because {}", id, reason);
        }
    }
}
