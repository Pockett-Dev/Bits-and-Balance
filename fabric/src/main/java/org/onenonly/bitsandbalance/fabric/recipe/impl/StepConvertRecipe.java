package org.onenonly.bitsandbalance.fabric.recipe.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.fabric.recipe.FabricRecipeSerializerFactory;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.recipe.EnhancedSlabRecipeLookup;

import java.util.List;

public final class StepConvertRecipe implements CraftingRecipe {
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
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return resultFor(input, direction);
    }

    public ItemStack assemble(RecipeInput input) {
        return input instanceof CraftingInput craftingInput ? resultFor(craftingInput, direction) : ItemStack.EMPTY;
    }

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
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeType<CraftingRecipe> getType() {
        return RecipeType.CRAFTING;
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
        if (slab == null) return null;
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

    private Direction direction() {
        return direction;
    }

    private enum Direction {
        STEP_TO_VERTICAL_STEP("step_to_vertical_step") {
            @Override
            Item outputFor(Block inputBlock) {
                return outputVerticalStepFor(inputBlock);
            }
        },
        VERTICAL_STEP_TO_STEP("vertical_step_to_step") {
            @Override
            Item outputFor(Block inputBlock) {
                return outputStepFor(inputBlock);
            }
        };

        private final String id;

        Direction(String id) {
            this.id = id;
        }

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

        public static final RecipeSerializer<StepConvertRecipe> INSTANCE = FabricRecipeSerializerFactory.create(CODEC, STREAM_CODEC);

        private Serializer() {
        }
    }
}