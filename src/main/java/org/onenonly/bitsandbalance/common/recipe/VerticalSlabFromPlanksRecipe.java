package org.onenonly.bitsandbalance.common.recipe;

import com.mojang.serialization.MapCodec;
//? if mc != 26.1 && mc != 26.2 && mc != 26.3
import net.minecraft.core.HolderLookup;
//? endif
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.world.item.crafting.Ingredient;
//? endif
//? if mc != 1.21.1
import net.minecraft.world.item.crafting.PlacementInfo;
//? endif
import net.minecraft.world.item.crafting.RecipeSerializer;
//? if mc != 1.21.1
import net.minecraft.world.item.crafting.display.RecipeDisplay;
//? endif
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import java.util.LinkedHashSet;
//? endif
//? if mc != 1.21.1
import java.util.List;
//? endif

/**
 * 3 matching planks/stone-like base items in a vertical column -> 6 matching vertical slabs.
 */
public final class VerticalSlabFromPlanksRecipe extends CustomRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_slab_from_planks");
    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private final CraftingBookCategory category;
    //? endif

    public VerticalSlabFromPlanksRecipe(CraftingBookCategory category) {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        this.category = category;
        //? else
        super(category);
        //? endif
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !resultFor(input).isEmpty();
    }

    @Override
    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    public ItemStack assemble(CraftingInput input) {
        return resultFor(input);
    }
    //? else
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return resultFor(input);
    }
    //? endif

    @Override
    //? if mc == 1.21.1
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 1 && height >= 3;
    }
    //? else
    public PlacementInfo placementInfo() {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        Ingredient ingredient = placementIngredient();
        return ingredient == null ? PlacementInfo.create(List.of()) : RecipeBookDisplayHelper.repeatedPlacement(ingredient, 3);
        //? else
        return PlacementInfo.create(List.of());
        //? endif
    }

    @Override
    public List<RecipeDisplay> display() {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        Ingredient ingredient = placementIngredient();
        ItemStack result = representativeVerticalSlabResult();
        return ingredient == null || result.isEmpty()
                ? List.of()
                : List.of(RecipeBookDisplayHelper.shapedFilledDisplay(1, 3, ingredient, result));
        //? else
        return List.of();
        //? endif
    }
    //? endif

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    @Override
    public CraftingBookCategory category() {
        return this.category;
    }
    //? endif

    @Override
    public boolean isSpecial() {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        return false;
        //? else
        return true;
        //? endif
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    private static ItemStack resultFor(CraftingInput input) {
        if (input == null || input.height() != 3) {
            return ItemStack.EMPTY;
        }

        int width = input.width();
        int height = input.height();
        for (int col = 0; col < width; col++) {
            ItemStack first = input.getItem(col);
            ItemStack second = input.getItem(col + width);
            ItemStack third = input.getItem(col + 2 * width);

            if (first.isEmpty() || second.isEmpty() || third.isEmpty()) {
                continue;
            }
            if (!ItemStack.isSameItem(first, second) || !ItemStack.isSameItem(first, third)) {
                continue;
            }

            boolean othersClear = true;
            for (int i = 0; i < width * height; i++) {
                if (i == col || i == col + width || i == col + 2 * width) {
                    continue;
                }
                if (!input.getItem(i).isEmpty()) {
                    othersClear = false;
                    break;
                }
            }
            if (!othersClear) {
                continue;
            }

            Item verticalItem = findVerticalSlabItemFor(first.getItem());
            if (isRealItem(verticalItem)) {
                return new ItemStack(verticalItem, 6);
            }
            break;
        }

        return ItemStack.EMPTY;
    }

    static @Nullable Item bitsandbalance$displayInputForSlab(Block slabBlock) {
        if (slabBlock == null) {
            return null;
        }

        Identifier slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);
        if (slabId == null) {
            return null;
        }
        String namespace = slabId.getNamespace();
        String path = slabId.getPath();
        if (!path.endsWith("_slab")) {
            return null;
        }

        String basePath = path.substring(0, path.length() - "_slab".length());
        java.util.Set<Identifier> candidates = new java.util.LinkedHashSet<>();
        addCandidate(candidates, namespace, basePath + "_planks");
        if (basePath.endsWith("_brick")) {
            addCandidate(candidates, namespace, basePath.substring(0, basePath.length() - "_brick".length()) + "_bricks");
        }
        if (basePath.endsWith("_tile")) {
            addCandidate(candidates, namespace, basePath.substring(0, basePath.length() - "_tile".length()) + "_tiles");
        }
        addCandidate(candidates, namespace, basePath + "_block");
        addCandidate(candidates, namespace, basePath);

        if (!"minecraft".equals(namespace)) {
            addCandidate(candidates, "minecraft", basePath + "_planks");
            if (basePath.endsWith("_brick")) {
                addCandidate(candidates, "minecraft", basePath.substring(0, basePath.length() - "_brick".length()) + "_bricks");
            }
            if (basePath.endsWith("_tile")) {
                addCandidate(candidates, "minecraft", basePath.substring(0, basePath.length() - "_tile".length()) + "_tiles");
            }
            addCandidate(candidates, "minecraft", basePath + "_block");
            addCandidate(candidates, "minecraft", basePath);
        }

        for (Identifier candidate : candidates) {
            Item item = BuiltInRegistries.ITEM.getOptional(candidate).orElse(null);
            if (item != null && item != Items.AIR) {
                return item;
            }
        }

        return null;
    }

    private static @Nullable Item findVerticalSlabItemFor(Item inputItem) {
        Identifier id = BuiltInRegistries.ITEM.getKey(inputItem);
        if (id == null) {
            return null;
        }
        String namespace = id.getNamespace();
        String path = id.getPath();

        java.util.List<Identifier> candidates = new java.util.ArrayList<>();
        if (path.endsWith("_planks")) {
            candidates.add(Identifier.fromNamespaceAndPath(namespace, path.substring(0, path.length() - "_planks".length()) + "_slab"));
        }
        if (path.endsWith("_bricks")) {
            candidates.add(Identifier.fromNamespaceAndPath(namespace, path.substring(0, path.length() - "_bricks".length()) + "_brick_slab"));
        }
        if (path.endsWith("_tiles")) {
            candidates.add(Identifier.fromNamespaceAndPath(namespace, path.substring(0, path.length() - "_tiles".length()) + "_tile_slab"));
        }
        if (path.endsWith("_block")) {
            candidates.add(Identifier.fromNamespaceAndPath(namespace, path.substring(0, path.length() - "_block".length()) + "_slab"));
        }
        candidates.add(Identifier.fromNamespaceAndPath(namespace, path + "_slab"));

        for (Identifier candidate : candidates) {
            Block slabBlock = BuiltInRegistries.BLOCK.getOptional(candidate).orElse(null);
            if (slabBlock == null) {
                continue;
            }
            Block verticalBlock = EnhancedSlabRecipeLookup.verticalForSlab(slabBlock);
            if (verticalBlock == null) {
                continue;
            }
            Item verticalItem = verticalBlock.asItem();
            if (isRealItem(verticalItem)) {
                return verticalItem;
            }
        }

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            if (VerticalSlabRecipeInputs.displayInputForSlab(slabBlock) != inputItem) {
                continue;
            }
            Block verticalBlock = EnhancedSlabRecipeLookup.verticalForSlab(slabBlock);
            if (verticalBlock == null) {
                continue;
            }
            Item verticalItem = verticalBlock.asItem();
            if (isRealItem(verticalItem)) {
                return verticalItem;
            }
        }
        return null;
    }

    private static void addCandidate(java.util.Set<Identifier> candidates, String namespace, String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        candidates.add(Identifier.fromNamespaceAndPath(namespace, path));
    }

    private static boolean isRealItem(Item item) {
        return item != null && item != Items.AIR;
    }

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private static ItemStack stack(String id, int count) {
        Identifier itemId = Identifier.parse(id);
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    private static Ingredient placementIngredient() {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            if (EnhancedSlabRecipeLookup.verticalForSlab(slabBlock) == null) {
                continue;
            }
            Item item = bitsandbalance$displayInputForSlab(slabBlock);
            if (isRealItem(item)) {
                items.add(item);
            }
        }
        if (items.isEmpty()) return null;
        return Ingredient.of(items.toArray(Item[]::new));
    }

    private static ItemStack representativeVerticalSlabResult() {
        ItemStack result = stack("bitsandbalance:vertical_oak_slab", 6);
        if (!result.isEmpty()) {
            return result;
        }

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == null || item == Items.AIR) {
                continue;
            }
            Item vertical = findVerticalSlabItemFor(item);
            if (isRealItem(vertical)) {
                return new ItemStack(vertical, 6);
            }
        }

        return ItemStack.EMPTY;
    }
    //? endif

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    public static final class Serializer {
    //? else
    public static final class Serializer implements RecipeSerializer<VerticalSlabFromPlanksRecipe> {
    //? endif
        //? if mc != 26.1 && mc != 26.2 && mc != 26.3
        public static final Serializer INSTANCE = new Serializer();
        //? endif

        private static final VerticalSlabFromPlanksRecipe TEMPLATE = new VerticalSlabFromPlanksRecipe(CraftingBookCategory.BUILDING);
        private static final MapCodec<VerticalSlabFromPlanksRecipe> CODEC = MapCodec.unit(TEMPLATE);
        private static final StreamCodec<RegistryFriendlyByteBuf, VerticalSlabFromPlanksRecipe> STREAM_CODEC = StreamCodec.unit(TEMPLATE);

        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        public static final RecipeSerializer<VerticalSlabFromPlanksRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);
        //? endif

        private Serializer() {
        }

        //? if mc != 26.1 && mc != 26.2 && mc != 26.3
        @Override
        public MapCodec<VerticalSlabFromPlanksRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VerticalSlabFromPlanksRecipe> streamCodec() {
            return STREAM_CODEC;
        }
        //? endif
    }
}
