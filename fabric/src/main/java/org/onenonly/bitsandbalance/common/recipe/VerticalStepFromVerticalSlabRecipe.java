package org.onenonly.bitsandbalance.fabric.recipe.impl;

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
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.recipe.EnhancedSlabRecipeLookup;

import java.util.List;

public final class VerticalStepFromVerticalSlabRecipe extends CustomRecipe {
    public static final Identifier SERIALIZER_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_step_from_vertical_slab");

    private final CraftingBookCategory category;
    private final Direction direction;

    public VerticalStepFromVerticalSlabRecipe(CraftingBookCategory category) {
        this(category, Direction.VERTICAL_SLAB_TO_VERTICAL_STEP);
    }

    public VerticalStepFromVerticalSlabRecipe(CraftingBookCategory category, Direction direction) {
        super(category);
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
        if (!direction.matchesShape(input.width(), input.height())) return ItemStack.EMPTY;

        ItemStack first = input.getItem(0);
        ItemStack second = input.getItem(direction.secondSlotIndex(input.width()));
        if (first.isEmpty() || second.isEmpty()) return ItemStack.EMPTY;
        if (!ItemStack.isSameItemSameComponents(first, second)) return ItemStack.EMPTY;

        Item item = first.getItem();
        if (!(item instanceof BlockItem blockItem)) return ItemStack.EMPTY;

        Block block = blockItem.getBlock();
        if (block == null) return ItemStack.EMPTY;

        Item output = direction.outputFor(block);
        return output == null ? ItemStack.EMPTY : new ItemStack(output, direction.resultCount());
    }

    private static Item outputVerticalStepFor(Block block) {
        Block verticalStep = EnhancedSlabRecipeLookup.verticalStepForVerticalSlab(block);
        return verticalStep == null ? null : verticalStep.asItem();
    }

    private static Item outputVerticalSlabFor(Block block) {
        Block verticalSlab = EnhancedSlabRecipeLookup.verticalSlabForVerticalStep(block);
        return verticalSlab == null ? null : verticalSlab.asItem();
    }

    private Direction direction() {
        return direction;
    }

    private enum Direction {
        VERTICAL_SLAB_TO_VERTICAL_STEP("vertical_slab_to_vertical_step", 4) {
            @Override
            boolean matchesShape(int width, int height) {
                return width == 2 && height == 1;
            }

            @Override
            int secondSlotIndex(int width) {
                return 1;
            }

            @Override
            Item outputFor(Block inputBlock) {
                return outputVerticalStepFor(inputBlock);
            }
        },
        VERTICAL_STEP_TO_VERTICAL_SLAB("vertical_step_to_vertical_slab", 1) {
            @Override
            boolean matchesShape(int width, int height) {
                return width == 2 && height == 1;
            }

            @Override
            int secondSlotIndex(int width) {
                return 1;
            }

            @Override
            Item outputFor(Block inputBlock) {
                return outputVerticalSlabFor(inputBlock);
            }
        };

        private final String id;
        private final int resultCount;

        Direction(String id, int resultCount) {
            this.id = id;
            this.resultCount = resultCount;
        }

        abstract Item outputFor(Block inputBlock);

        abstract boolean matchesShape(int width, int height);

        abstract int secondSlotIndex(int width);

        private int resultCount() {
            return resultCount;
        }

        private String id() {
            return id;
        }

        private static DataResult<Direction> parse(String id) {
            for (Direction direction : values()) {
                if (direction.id.equals(id)) {
                    return DataResult.success(direction);
                }
            }
            return DataResult.error(() -> "Unknown vertical_step_from_vertical_slab direction: " + id);
        }

        private static Direction byName(String id) {
            return parse(id).result().orElse(VERTICAL_SLAB_TO_VERTICAL_STEP);
        }
    }

    public static final class Serializer {
        private static final Codec<Direction> DIRECTION_CODEC = Codec.STRING.comapFlatMap(Direction::parse, Direction::id);
        private static final MapCodec<VerticalStepFromVerticalSlabRecipe> CODEC = DIRECTION_CODEC
                .optionalFieldOf("direction", Direction.VERTICAL_SLAB_TO_VERTICAL_STEP)
                .xmap(direction -> new VerticalStepFromVerticalSlabRecipe(CraftingBookCategory.BUILDING, direction), VerticalStepFromVerticalSlabRecipe::direction);
        private static final StreamCodec<RegistryFriendlyByteBuf, VerticalStepFromVerticalSlabRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public VerticalStepFromVerticalSlabRecipe decode(RegistryFriendlyByteBuf buf) {
                return new VerticalStepFromVerticalSlabRecipe(CraftingBookCategory.BUILDING, Direction.byName(buf.readUtf()));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, VerticalStepFromVerticalSlabRecipe recipe) {
                buf.writeUtf(recipe.direction.id());
            }
        };

        public static final RecipeSerializer<VerticalStepFromVerticalSlabRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);

        private Serializer() {
        }
    }
}
