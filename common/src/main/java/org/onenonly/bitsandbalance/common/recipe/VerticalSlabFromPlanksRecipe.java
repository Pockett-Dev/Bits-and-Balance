package org.onenonly.bitsandbalance.common.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 3 planks in a vertical column (crafting table) -> 6 matching vertical slabs.
 */
public final class VerticalSlabFromPlanksRecipe extends CustomRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_slab_from_planks");
    private final CraftingBookCategory category;

    public VerticalSlabFromPlanksRecipe(CraftingBookCategory category) {
        this.category = category;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !resultFor(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return resultFor(input);
    }

    @Override
    public CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(List.of());
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    private static ItemStack resultFor(CraftingInput input) {
        if (input == null) return ItemStack.EMPTY;
        // Minecraft compacts CraftingInput to the bounding box of filled slots.
        int width = input.width();
        int height = input.height();

        // Vertical column (height == 3): 3 planks in a column → 6 vertical slabs.
        // Any width 1–3; all slots outside the filled column must be empty.
        if (height != 3) return ItemStack.EMPTY;

        // Find which column has 3 matching items; all other slots must be empty.
        for (int col = 0; col < width; col++) {
            ItemStack a = input.getItem(col);
            ItemStack b = input.getItem(col + width);
            ItemStack c = input.getItem(col + 2 * width);

            if (a.isEmpty() || b.isEmpty() || c.isEmpty()) continue;
            if (!ItemStack.isSameItem(a, b) || !ItemStack.isSameItem(a, c)) continue;

            // All other slots must be empty.
            boolean othersClear = true;
            for (int i = 0; i < width * height; i++) {
                if (i == col || i == col + width || i == col + 2 * width) continue;
                if (!input.getItem(i).isEmpty()) {
                    othersClear = false;
                    break;
                }
            }
            if (!othersClear) continue;

            Item verticalItem = findVerticalSlabItemFor(a.getItem());
            if (verticalItem != null) return new ItemStack(verticalItem, 6);
            break; // column found but no registerd VS — stop searching
        }

        return ItemStack.EMPTY;
    }

    /**
     * Given any input item, tries several naming conventions to locate a matching slab block.
     * Returns the slab item directly (no vertical slab lookup required), or null.
     *
     * <p>Patterns tried (in order):
     * <ol>
     *   <li>{@code *_planks} → {@code *_slab}</li>
     *   <li>{@code *_bricks} → {@code *_brick_slab}</li>
     *   <li>{@code *_block}  → {@code *_slab}</li>
     *   <li>{@code *}        → {@code *_slab}</li>
     * </ol>
     */
    static @Nullable Item bitsandbalance$displayInputForSlab(Block slabBlock) {
        return VerticalSlabRecipeInputs.displayInputForSlab(slabBlock);
    }

    private static @Nullable Item findSlabItemFor(Item inputItem) {
        Identifier id = BuiltInRegistries.ITEM.getKey(inputItem);
        if (id == null) return null;
        String ns = id.getNamespace();
        String path = id.getPath();

        java.util.List<Identifier> candidates = new java.util.ArrayList<>();
        if (path.endsWith("_planks")) {
            candidates.add(Identifier.fromNamespaceAndPath(ns, path.substring(0, path.length() - "_planks".length()) + "_slab"));
        }
        if (path.endsWith("_bricks")) {
            candidates.add(Identifier.fromNamespaceAndPath(ns, path.substring(0, path.length() - "_bricks".length()) + "_brick_slab"));
        }
        if (path.endsWith("_block")) {
            candidates.add(Identifier.fromNamespaceAndPath(ns, path.substring(0, path.length() - "_block".length()) + "_slab"));
        }
        candidates.add(Identifier.fromNamespaceAndPath(ns, path + "_slab"));

        for (Identifier candidate : candidates) {
            Block slabBlock = BuiltInRegistries.BLOCK.getOptional(candidate).orElse(null);
            if (slabBlock == null) continue;
            Item item = slabBlock.asItem();
            if (item != null && item != Items.AIR) return item;
        }
        return null;
    }

    private static Ingredient placementIngredient() {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            if (EnhancedSlabRecipeLookup.verticalForSlab(slabBlock) == null) continue;
            Item item = bitsandbalance$displayInputForSlab(slabBlock);
            if (isRealItem(item)) {
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            return Ingredient.of(Items.OAK_PLANKS);
        }
        return Ingredient.of(items.toArray(Item[]::new));
    }

    private static RecipeDisplay shapedDisplay(int width, int height, List<SlotDisplay> grid, ItemStack result) {
        SlotDisplay resultSlot = new SlotDisplay.ItemStackSlotDisplay(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(result));
        SlotDisplay station = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);
        return new ShapedCraftingRecipeDisplay(width, height, grid, resultSlot, station);
    }

    private static boolean isRealItem(@Nullable Item item) {
        return item != null && item != Items.AIR;
    }

    /**
     * Given any input item, tries several naming conventions to locate a matching slab block
     * that has a registered vertical slab counterpart. Returns the vertical slab item, or null.
     *
     * <p>Patterns tried (in order):
     * <ol>
     *   <li>{@code *_planks} → {@code *_slab} (e.g. oak_planks → oak_slab)</li>
     *   <li>{@code *_bricks} → {@code *_brick_slab} (e.g. stone_bricks → stone_brick_slab)</li>
     *   <li>{@code *_block} → {@code *_slab} (e.g. quartz_block → quartz_slab)</li>
     *   <li>{@code *} → {@code *_slab} (e.g. stone → stone_slab, cobblestone → cobblestone_slab)</li>
     * </ol>
     */
    private static @Nullable Item findVerticalSlabItemFor(Item inputItem) {
        Identifier id = BuiltInRegistries.ITEM.getKey(inputItem);
        if (id == null) return null;
        String ns = id.getNamespace();
        String path = id.getPath();

        java.util.List<Identifier> candidates = new java.util.ArrayList<>();

        // planks: oak_planks -> oak_slab
        if (path.endsWith("_planks")) {
            candidates.add(Identifier.fromNamespaceAndPath(ns, path.substring(0, path.length() - "_planks".length()) + "_slab"));
        }
        // plural bricks: stone_bricks -> stone_brick_slab
        if (path.endsWith("_bricks")) {
            candidates.add(Identifier.fromNamespaceAndPath(ns, path.substring(0, path.length() - "_bricks".length()) + "_brick_slab"));
        }
        // block: quartz_block -> quartz_slab
        if (path.endsWith("_block")) {
            candidates.add(Identifier.fromNamespaceAndPath(ns, path.substring(0, path.length() - "_block".length()) + "_slab"));
        }
        // general: stone -> stone_slab
        candidates.add(Identifier.fromNamespaceAndPath(ns, path + "_slab"));

        for (Identifier candidate : candidates) {
            Block slabBlock = BuiltInRegistries.BLOCK.getOptional(candidate).orElse(null);
            if (slabBlock == null) continue;
            Block verticalBlock = EnhancedSlabRecipeLookup.verticalForSlab(slabBlock);
            if (verticalBlock == null) continue;
            Item vItem = verticalBlock.asItem();
            if (vItem != null) return vItem;
        }
        return null;
    }

    private static Ingredient bitsandbalance$verticalSlabInputIngredient() {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == null || item == Items.AIR) continue;
            if (findVerticalSlabItemFor(item) != null) {
                items.add(item);
            }
        }

        if (items.isEmpty()) {
            return Ingredient.of(Items.OAK_PLANKS);
        }
        return Ingredient.of(items.toArray(Item[]::new));
    }

    public static final class Serializer {
        private static final VerticalSlabFromPlanksRecipe TEMPLATE = new VerticalSlabFromPlanksRecipe(CraftingBookCategory.BUILDING);
        private static final MapCodec<VerticalSlabFromPlanksRecipe> CODEC = MapCodec.unit(TEMPLATE);
        private static final StreamCodec<RegistryFriendlyByteBuf, VerticalSlabFromPlanksRecipe> STREAM_CODEC = StreamCodec.unit(TEMPLATE);

        public static final RecipeSerializer<VerticalSlabFromPlanksRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);

        private Serializer() {
        }
    }
}
