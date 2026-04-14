package org.onenonly.bitsandbalance.common.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
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
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * One-way conversion between steps and vertical steps.
 */
public final class StepConvertRecipe extends CustomRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "step_convert");

    private final CraftingBookCategory category;
    private final Direction direction;

    public StepConvertRecipe(CraftingBookCategory category) {
        this(category, Direction.STEP_TO_VERTICAL_STEP);
    }

    public StepConvertRecipe(CraftingBookCategory category, Direction direction) {
        this.category = category;
        this.direction = direction;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !resultFor(input, direction).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return resultFor(input, direction);
    }

    @Override
    public CraftingBookCategory category() {
        return this.category;
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

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    private static ItemStack resultFor(CraftingInput input, Direction direction) {
        if (input == null || input.isEmpty()) return ItemStack.EMPTY;

        ItemStack found = ItemStack.EMPTY;
        int nonEmpty = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            nonEmpty++;
            if (nonEmpty > 1) return ItemStack.EMPTY;
            found = stack;
        }
        if (found.isEmpty()) return ItemStack.EMPTY;

        Item item = found.getItem();
        if (!(item instanceof BlockItem blockItem)) return ItemStack.EMPTY;

        Block block = blockItem.getBlock();
        if (block == null) return ItemStack.EMPTY;

        Item output = direction.outputFor(block);
        return output == null ? ItemStack.EMPTY : new ItemStack(output, 1);
    }

    private static Item outputVerticalStepFor(Block block) {
        if (block instanceof FixedStepBlock fixedStep) {
            Block verticalSlabForStep = EnhancedSlabRecipeLookup.verticalForSlab(fixedStep.getSourceSlab());
            if (verticalSlabForStep == null) return null;
            Block verticalStepForStep = EnhancedSlabRecipeLookup.verticalStepForVerticalSlab(verticalSlabForStep);
            return verticalStepForStep == null ? null : verticalStepForStep.asItem();
        }

        Block slab = EnhancedSlabRecipeLookup.slabForStep(block);
        if (slab == null) {
            return null;
        }

        Block verticalSlab = EnhancedSlabRecipeLookup.verticalForSlab(slab);
        if (verticalSlab == null) return null;
        Block verticalStep = EnhancedSlabRecipeLookup.verticalStepForVerticalSlab(verticalSlab);
        return verticalStep == null ? null : verticalStep.asItem();
    }

    private static Item outputStepFor(Block block) {
        if (block instanceof FixedVerticalStepBlock fixedVerticalStep) {
            Block sourceVerticalSlab = fixedVerticalStep.getSourceVerticalSlab();
            Block sourceSlab = sourceVerticalSlab instanceof FixedVerticalSlabBlock fixedVerticalSlab
                    ? fixedVerticalSlab.getSourceSlab()
                    : EnhancedSlabRecipeLookup.slabForVertical(sourceVerticalSlab);
            if (sourceSlab == null) return null;
            Block stepForVerticalStep = EnhancedSlabRecipeLookup.stepForSlab(sourceSlab);
            return stepForVerticalStep == null ? null : stepForVerticalStep.asItem();
        }

        Block verticalSlab = EnhancedSlabRecipeLookup.verticalSlabForVerticalStep(block);
        if (verticalSlab == null) return null;
        Block slabForVertical = EnhancedSlabRecipeLookup.slabForVertical(verticalSlab);
        if (slabForVertical == null) return null;
        Block step = EnhancedSlabRecipeLookup.stepForSlab(slabForVertical);
        return step == null ? null : step.asItem();
    }

    private static Ingredient stepPlacementIngredient() {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Block stepBlock = EnhancedSlabRecipeLookup.stepForSlab(block);
            if (stepBlock == null) continue;
            Item item = stepBlock.asItem();
            if (item != null && item != Items.AIR) {
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            return Ingredient.of(Items.OAK_SLAB);
        }
        return Ingredient.of(items.toArray(Item[]::new));
    }

    private static Ingredient verticalStepPlacementIngredient() {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Block verticalStepBlock = EnhancedSlabRecipeLookup.verticalStepForVerticalSlab(block);
            if (verticalStepBlock == null) continue;
            Item item = verticalStepBlock.asItem();
            if (item != null && item != Items.AIR) {
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            return Ingredient.of(Items.OAK_SLAB);
        }
        return Ingredient.of(items.toArray(Item[]::new));
    }

    private static ItemStack representativeVerticalStepResult() {
        ItemStack result = stack("bitsandbalance:vertical_oak_step");
        if (!result.isEmpty()) {
            return result;
        }

        for (Block block : BuiltInRegistries.BLOCK) {
            Block verticalStepBlock = EnhancedSlabRecipeLookup.verticalStepForVerticalSlab(block);
            if (verticalStepBlock == null) continue;
            Item item = verticalStepBlock.asItem();
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        }

        return new ItemStack(Items.OAK_SLAB);
    }

    private static ItemStack representativeStepResult() {
        ItemStack result = stack("bitsandbalance:oak_step");
        if (!result.isEmpty()) {
            return result;
        }

        for (Block block : BuiltInRegistries.BLOCK) {
            Block stepBlock = EnhancedSlabRecipeLookup.stepForSlab(block);
            if (stepBlock == null) continue;
            Item item = stepBlock.asItem();
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        }

        return new ItemStack(Items.OAK_SLAB);
    }

    private static ItemStack stack(String id) {
        Identifier itemId = Identifier.parse(id);
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    private Direction direction() {
        return direction;
    }

    private static RecipeDisplay shapelessDisplay(SlotDisplay ingredient, ItemStack result) {
        SlotDisplay resultSlot = new SlotDisplay.ItemStackSlotDisplay(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(result));
        SlotDisplay station = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);
        return new ShapelessCraftingRecipeDisplay(List.of(ingredient), resultSlot, station);
    }

    private enum Direction {
        STEP_TO_VERTICAL_STEP("step_to_vertical_step") {
            @Override
            Ingredient inputIngredient() {
                return stepPlacementIngredient();
            }

            @Override
            ItemStack resultDisplay() {
                return representativeVerticalStepResult();
            }

            @Override
            Item outputFor(Block inputBlock) {
                return outputVerticalStepFor(inputBlock);
            }
        },
        VERTICAL_STEP_TO_STEP("vertical_step_to_step") {
            @Override
            Ingredient inputIngredient() {
                return verticalStepPlacementIngredient();
            }

            @Override
            ItemStack resultDisplay() {
                return representativeStepResult();
            }

            @Override
            Item outputFor(Block inputBlock) {
                return outputStepFor(inputBlock);
            }
        };

        private final String id;

        Direction(String id) {
            this.id = id;
        }

        abstract Ingredient inputIngredient();

        abstract ItemStack resultDisplay();

        abstract Item outputFor(Block inputBlock);

        private String id() {
            return id;
        }

        private static DataResult<Direction> parse(String id) {
            for (Direction direction : values()) {
                if (direction.id.equals(id)) {
                    return DataResult.success(direction);
                }
            }
            return DataResult.error(() -> "Unknown step_convert direction: " + id);
        }

        private static Direction byName(String id) {
            return parse(id).result().orElse(STEP_TO_VERTICAL_STEP);
        }
    }

    public static final class Serializer {
        private static final Codec<Direction> DIRECTION_CODEC = Codec.STRING.comapFlatMap(Direction::parse, Direction::id);
        private static final MapCodec<StepConvertRecipe> CODEC = DIRECTION_CODEC
                .optionalFieldOf("direction", Direction.STEP_TO_VERTICAL_STEP)
                .xmap(direction -> new StepConvertRecipe(CraftingBookCategory.BUILDING, direction), StepConvertRecipe::direction);
        private static final StreamCodec<RegistryFriendlyByteBuf, StepConvertRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public StepConvertRecipe decode(RegistryFriendlyByteBuf buf) {
                return new StepConvertRecipe(CraftingBookCategory.BUILDING, Direction.byName(buf.readUtf()));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, StepConvertRecipe recipe) {
                buf.writeUtf(recipe.direction.id());
            }
        };

        public static final RecipeSerializer<StepConvertRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);

        private Serializer() {
        }
    }
}
