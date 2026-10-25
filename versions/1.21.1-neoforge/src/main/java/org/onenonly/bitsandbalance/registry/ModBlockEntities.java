package org.onenonly.bitsandbalance.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.ModBlocks;
import org.onenonly.bitsandbalance.blocks.entity.AzaleaHangingSignBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.CandleBundleBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;

/**
 * Registers custom block entities for hanging signs and other mod blocks.
 */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BitsAndBalance.MODID);

    // Hanging sign block entity that supports azalea hanging signs
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AzaleaHangingSignBlockEntity>> AZALEA_HANGING_SIGN =
            BLOCK_ENTITIES.register("azalea_hanging_sign", () ->
                    BlockEntityType.Builder.of(
                            AzaleaHangingSignBlockEntity::new,
                            ModBlocks.AZALEA_HANGING_SIGN.get(),
                            ModBlocks.AZALEA_WALL_HANGING_SIGN.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CandleBundleBlockEntity>> CANDLE_BUNDLE =
            BLOCK_ENTITIES.register("candle_bundle", () -> {
                BlockEntityType<CandleBundleBlockEntity> type = BlockEntityType.Builder.of(
                        CandleBundleBlockEntity::new,
                        ModBlocks.BUNDLE_CANDLE.get()
                ).build(null);
                CommonBlockEntities.CANDLE_BUNDLE = type;
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MixedSlabBlockEntity>> MIXED_SLAB =
            BLOCK_ENTITIES.register("mixed_slab", () -> {
                BlockEntityType<MixedSlabBlockEntity> type = BlockEntityType.Builder.of(
                        MixedSlabBlockEntity::new,
                        ModBlocks.MIXED_SLAB.get()
                ).build(null);
                CommonBlockEntities.MIXED_SLAB = type;
                return type;
            });


    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VerticalSlabBlockEntity>> VERTICAL_SLAB =
            BLOCK_ENTITIES.register("vertical_slab", () -> {
                BlockEntityType<VerticalSlabBlockEntity> type = BlockEntityType.Builder.of(
                        VerticalSlabBlockEntity::new,
                        ModBlocks.VERTICAL_SLAB.get()
                ).build(null);
                CommonBlockEntities.VERTICAL_SLAB = type;
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StepBlockEntity>> STEP =
            BLOCK_ENTITIES.register("step", () -> {
                BlockEntityType<StepBlockEntity> type = BlockEntityType.Builder.of(
                        StepBlockEntity::new,
                        ModBlocks.STEP.get()
                ).build(null);
                CommonBlockEntities.STEP = type;
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuadStepBlockEntity>> QUAD_STEP =
            BLOCK_ENTITIES.register("quad_step", () -> {
                BlockEntityType<QuadStepBlockEntity> type = BlockEntityType.Builder.of(
                        QuadStepBlockEntity::new,
                        ModBlocks.QUAD_STEP.get()
                ).build(null);
                CommonBlockEntities.QUAD_STEP = type;
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VerticalStepBlockEntity>> VERTICAL_STEP =
            BLOCK_ENTITIES.register("vertical_step", () -> {
                BlockEntityType<VerticalStepBlockEntity> type = BlockEntityType.Builder.of(
                        VerticalStepBlockEntity::new,
                        ModBlocks.VERTICAL_STEP.get()
                ).build(null);
                CommonBlockEntities.VERTICAL_STEP = type;
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuadVerticalStepBlockEntity>> QUAD_VERTICAL_STEP =
            BLOCK_ENTITIES.register("quad_vertical_step", () -> {
                BlockEntityType<QuadVerticalStepBlockEntity> type = BlockEntityType.Builder.of(
                        QuadVerticalStepBlockEntity::new,
                        ModBlocks.QUAD_VERTICAL_STEP.get()
                ).build(null);
                CommonBlockEntities.QUAD_VERTICAL_STEP = type;
                return type;
            });

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
                BitsAndBalance.LOGGER.info("Registered custom Azalea hanging sign and mod block entities");
    }
}

