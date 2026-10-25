package org.onenonly.bitsandbalance.common.registry;

import com.google.common.collect.BiMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
//? if mc == 26.3
import net.minecraft.core.component.DataComponents;
//? endif
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
//? if mc == 26.3
import net.minecraft.tags.ItemTags;
//? else
import net.minecraft.world.item.AxeItem;
//? endif
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
//? if mc == 26.3
import net.minecraft.world.item.Items;
//? endif
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;

import java.lang.reflect.Field;

public final class DynamicVariantMaterialHelper {

    public static final int VERTICAL_SLAB_BURN_TIME = 150;
    public static final int STEP_BURN_TIME = 75;
    private static final float VANILLA_CHANGE_OVER_TIME_TICK_CHANCE = 0.05688889F;

    private static final Block DEFAULT_SLAB = Blocks.OAK_SLAB;
    private static final Field FIRE_IGNITE_ODDS_FIELD;
    private static final Field FIRE_BURN_ODDS_FIELD;

    static {
        Field igniteField = null;
        Field burnField = null;

        for (Field field : FireBlock.class.getDeclaredFields()) {
            if (!Object2IntMap.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
            } catch (Throwable ignored) {
                continue;
            }

            String name = field.getName().toLowerCase();
            if (igniteField == null && name.contains("ignite")) {
                igniteField = field;
                continue;
            }
            if (burnField == null && name.contains("burn")) {
                burnField = field;
                continue;
            }
            if (igniteField == null) {
                igniteField = field;
                continue;
            }
            if (burnField == null) {
                burnField = field;
            }
        }

        FIRE_IGNITE_ODDS_FIELD = igniteField;
        FIRE_BURN_ODDS_FIELD = burnField;
    }

    private DynamicVariantMaterialHelper() {
    }

    public static @Nullable Block getFuelSourceSlab(@Nullable Block block) {
        if (block instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            return fixedVerticalSlab.getSourceSlab();
        }
        if (block instanceof FixedStepBlock fixedStep) {
            return fixedStep.getSourceSlab();
        }
        if (block instanceof FixedVerticalStepBlock fixedVerticalStep) {
            Block sourceVerticalSlab = fixedVerticalStep.getSourceVerticalSlab();
            Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
            return sourceSlab != null ? sourceSlab : sourceVerticalSlab;
        }
        return null;
    }

    public static @Nullable Block getFuelSourceSlab(ItemStack stack) {
        return getFuelSourceSlab(Block.byItem(stack.getItem()));
    }

    public static boolean isFuelSourceSlab(@Nullable Block slabBlock) {
        return isWoodenSlab(slabBlock) || (getIgniteOdds(slabBlock) > 0 && getBurnOdds(slabBlock) > 0);
    }

    public static boolean isWoodenSlab(@Nullable Block block) {
        //? if mc == 26.3
        return block != null
                && block.asItem() != Items.AIR
                && block.asItem().components().has(DataComponents.COOKING_FUEL);
        //? else
        return block != null && block.builtInRegistryHolder().is(BlockTags.WOODEN_SLABS);
        //? endif
    }

    public static int getFuelBurnTime(@Nullable Block block) {
        Block sourceSlab = getFuelSourceSlab(block);
        if (!isFuelSourceSlab(sourceSlab)) {
            return 0;
        }
        if (block instanceof FixedVerticalSlabBlock) {
            return VERTICAL_SLAB_BURN_TIME;
        }
        if (block instanceof FixedStepBlock || block instanceof FixedVerticalStepBlock) {
            return STEP_BURN_TIME;
        }
        return 0;
    }

    public static int getDerivedIgniteOdds(@Nullable Block sourceSlab) {
        int igniteOdds = getIgniteOdds(sourceSlab);
        if (igniteOdds > 0) {
            return igniteOdds;
        }
        return isWoodenSlab(sourceSlab) ? 5 : 0;
    }

    public static int getDerivedBurnOdds(@Nullable Block sourceSlab) {
        int burnOdds = getBurnOdds(sourceSlab);
        if (burnOdds > 0) {
            return burnOdds;
        }
        return isWoodenSlab(sourceSlab) ? 20 : 0;
    }

    public static int getIgniteOdds(@Nullable Block block) {
        return bitsandbalance$getFireOdds(FIRE_IGNITE_ODDS_FIELD, block);
    }

    public static int getBurnOdds(@Nullable Block block) {
        return bitsandbalance$getFireOdds(FIRE_BURN_ODDS_FIELD, block);
    }

    public static BlockBehaviour.Properties copySafeDynamicProperties(@Nullable Block sourceBlock) {
        BlockState sourceState = sourceBlock == null ? null : normalizeSlabState(sourceBlock.defaultBlockState());
        if (sourceState == null && sourceBlock != null) {
            sourceState = sourceBlock.defaultBlockState();
        }

        MapColor mapColor = sourceBlock == null ? MapColor.STONE : sourceBlock.defaultMapColor();
        int lightEmission = bitsandbalance$safeLightEmission(sourceState);

        return (sourceBlock == null ? BlockBehaviour.Properties.of() : BlockBehaviour.Properties.ofFullCopy(sourceBlock))
                .mapColor(mapColor)
                .lightLevel(state -> lightEmission)
                .isValidSpawn((state, level, pos, entityType) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
//? if mc == 26.3
                .isViewBlocking((state, level, pos, bounds) -> false)
//? else
                .isViewBlocking((state, level, pos) -> false)
//? endif
//? if mc != 26.1 && mc != 26.2 && mc != 26.3
                .hasPostProcess((state, level, pos) -> false)
//? endif
//? if mc == 26.2 || mc == 26.3
                .emissiveRendering(state -> lightEmission > 0)
//? else
                .emissiveRendering((state, level, pos) -> lightEmission > 0)
//? endif
                .offsetType(BlockBehaviour.OffsetType.NONE);
    }

    private static int bitsandbalance$safeLightEmission(@Nullable BlockState sourceState) {
        try {
            return sourceState == null ? 0 : sourceState.getLightEmission();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static @Nullable BlockState normalizeSlabState(@Nullable BlockState state) {
        BlockState normalized = (state != null && state.getBlock() instanceof SlabBlock)
                ? state.getBlock().defaultBlockState()
                : null;
        if (normalized == null) {
            return null;
        }
        if (normalized.hasProperty(SlabBlock.TYPE)) {
            normalized = normalized.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (normalized.hasProperty(SlabBlock.WATERLOGGED)) {
            normalized = normalized.setValue(SlabBlock.WATERLOGGED, false);
        }
        return normalized;
    }

    public static boolean isCopperRandomlyTicking(@Nullable BlockState slabState) {
        BlockState normalized = normalizeSlabState(slabState);
        if (normalized == null) {
            return false;
        }
        if (!(normalized.getBlock() instanceof ChangeOverTimeBlock<?> changeOverTimeBlock)) {
            return false;
        }
        return changeOverTimeBlock.getNext(normalized).isPresent();
    }

    public static @Nullable BlockState getCopperRandomTickNextState(@Nullable BlockState slabState,
                                                                    ServerLevel level,
                                                                    BlockPos pos,
                                                                    RandomSource random) {
        BlockState normalized = normalizeSlabState(slabState);
        if (normalized == null) {
            return null;
        }
        if (!(normalized.getBlock() instanceof ChangeOverTimeBlock<?> changeOverTimeBlock)) {
            return null;
        }
        if (random.nextFloat() >= VANILLA_CHANGE_OVER_TIME_TICK_CHANCE) {
            return null;
        }
        return normalizeSlabState(changeOverTimeBlock.getNextState(normalized, level, pos, random).orElse(null));
    }

    public static @Nullable CopperUseResult getCopperUseResult(ItemStack stack, @Nullable BlockState slabState) {
        BlockState normalized = normalizeSlabState(slabState);
        if (normalized == null) {
            return null;
        }

        if (stack.getItem() instanceof HoneycombItem) {
            BlockState waxed = normalizeSlabState(HoneycombItem.getWaxed(normalized).orElse(null));
            if (waxed != null) {
//? if mc == 26.3
                return new CopperUseResult(waxed, SoundEvents.HONEYCOMB_WAX_ON, LevelEvent.PARTICLES_WAX_ON, true, false);
//? else
                return new CopperUseResult(waxed, SoundEvents.HONEYCOMB_WAX_ON, LevelEvent.PARTICLES_AND_SOUND_WAX_ON, true, false);
//? endif
            }
            return null;
        }

//? if mc == 26.3
        if (!stack.is(ItemTags.AXES)) {
//? else
        if (!(stack.getItem() instanceof AxeItem)) {
//? endif
            return null;
        }

        BlockState unwaxed = bitsandbalance$getUnwaxedState(normalized);
        if (unwaxed != null) {
//? if mc == 26.3
            return new CopperUseResult(unwaxed, SoundEvents.AXE_WAX_OFF.value(), LevelEvent.PARTICLES_WAX_OFF, false, true);
//? else
            return new CopperUseResult(unwaxed, SoundEvents.AXE_WAX_OFF, LevelEvent.PARTICLES_WAX_OFF, false, true);
//? endif
        }

        BlockState scraped = normalizeSlabState(WeatheringCopper.getPrevious(normalized).orElse(null));
        if (scraped != null) {
//? if mc == 26.3
            return new CopperUseResult(scraped, SoundEvents.AXE_SCRAPE.value(), LevelEvent.PARTICLES_SCRAPE, false, true);
//? else
            return new CopperUseResult(scraped, SoundEvents.AXE_SCRAPE, LevelEvent.PARTICLES_SCRAPE, false, true);
//? endif
        }

        return null;
    }

    public static BlockState fallbackSlabState(@Nullable BlockState preferred) {
        BlockState normalized = normalizeSlabState(preferred);
        if (normalized != null) {
            return normalized;
        }
        BlockState fallback = DEFAULT_SLAB.defaultBlockState();
        if (fallback.hasProperty(SlabBlock.TYPE)) {
            fallback = fallback.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (fallback.hasProperty(SlabBlock.WATERLOGGED)) {
            fallback = fallback.setValue(SlabBlock.WATERLOGGED, false);
        }
        return fallback;
    }

    private static int bitsandbalance$getFireOdds(@Nullable Field field, @Nullable Block block) {
        if (field == null || block == null || !(Blocks.FIRE instanceof FireBlock fireBlock)) {
            return 0;
        }
        try {
            @SuppressWarnings("unchecked")
            Object2IntMap<Block> odds = (Object2IntMap<Block>) field.get(fireBlock);
            return odds != null ? odds.getInt(block) : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static @Nullable BlockState bitsandbalance$getUnwaxedState(BlockState state) {
        try {
            BiMap<Block, Block> waxOffByBlock = HoneycombItem.WAX_OFF_BY_BLOCK.get();
            Block unwaxedBlock = waxOffByBlock.get(state.getBlock());
            return normalizeSlabState(unwaxedBlock == null ? null : unwaxedBlock.defaultBlockState());
        } catch (Throwable ignored) {
            return null;
        }
    }

    public record CopperUseResult(BlockState slabState,
                                  SoundEvent sound,
                                  int levelEvent,
                                  boolean consumeItem,
                                  boolean damageTool) {
    }
}
