package org.onenonly.bitsandbalance.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
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
import org.onenonly.bitsandbalance.common.blockentity.TemporaryCloudBlockEntity;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;

/**
 * Registers custom block entities for signs, hanging signs, and candle bundles.
 */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BitsAndBalance.MODID);

    // Custom sign block entity that supports azalea signs
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SignBlockEntity>> AZALEA_SIGN =
            BLOCK_ENTITIES.register("azalea_sign", () ->
                    new BlockEntityType<>(
                            SignBlockEntity::new,
                            java.util.Set.of(
                                    ModBlocks.AZALEA_SIGN.get(),
                                    ModBlocks.AZALEA_WALL_SIGN.get()
                            )
                    ));

    // Hanging sign block entity that supports azalea hanging signs
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AzaleaHangingSignBlockEntity>> AZALEA_HANGING_SIGN =
            BLOCK_ENTITIES.register("azalea_hanging_sign", () ->
                    new BlockEntityType<>(
                            AzaleaHangingSignBlockEntity::new,
                            java.util.Set.of(
                                    ModBlocks.AZALEA_HANGING_SIGN.get(),
                                    ModBlocks.AZALEA_WALL_HANGING_SIGN.get()
                            )
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CandleBundleBlockEntity>> CANDLE_BUNDLE =
            BLOCK_ENTITIES.register("candle_bundle", () -> {
                BlockEntityType<CandleBundleBlockEntity> type = new BlockEntityType<>(
                        CandleBundleBlockEntity::new,
                        java.util.Set.of(ModBlocks.BUNDLE_CANDLE.get())
                );
                CommonBlockEntities.CANDLE_BUNDLE = type;
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TemporaryCloudBlockEntity>> TEMPORARY_CLOUD =
            BLOCK_ENTITIES.register("temporary_cloud", () -> {
                BlockEntityType<TemporaryCloudBlockEntity> type = new BlockEntityType<>(
                        TemporaryCloudBlockEntity::new,
                        java.util.Set.of(ModBottleOfCloud.cloudBlock())
                );
                CommonBlockEntities.TEMPORARY_CLOUD = type;
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MixedSlabBlockEntity>> MIXED_SLAB =
            BLOCK_ENTITIES.register("mixed_slab", () -> {
                BlockEntityType<MixedSlabBlockEntity> type = new BlockEntityType<>(
                        MixedSlabBlockEntity::new,
                        java.util.Set.of(ModBlocks.MIXED_SLAB.get())
                );
                CommonBlockEntities.MIXED_SLAB = type;
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VerticalSlabBlockEntity>> VERTICAL_SLAB =
            BLOCK_ENTITIES.register("vertical_slab", () -> {
                BlockEntityType<VerticalSlabBlockEntity> type = new BlockEntityType<>(
                        VerticalSlabBlockEntity::new,
                        java.util.Set.of(ModBlocks.VERTICAL_SLAB.get())
                );
                CommonBlockEntities.VERTICAL_SLAB = type;
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StepBlockEntity>> STEP =
            BLOCK_ENTITIES.register("step", () -> {
                BlockEntityType<StepBlockEntity> type = new BlockEntityType<>(
                        StepBlockEntity::new,
                        java.util.Set.of(ModBlocks.STEP.get())
                );
                CommonBlockEntities.STEP = type;
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuadStepBlockEntity>> QUAD_STEP =
            BLOCK_ENTITIES.register("quad_step", () -> {
                BlockEntityType<QuadStepBlockEntity> type = new BlockEntityType<>(
                        QuadStepBlockEntity::new,
                        java.util.Set.of(ModBlocks.QUAD_STEP.get())
                );
                CommonBlockEntities.QUAD_STEP = type;
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VerticalStepBlockEntity>> VERTICAL_STEP =
            BLOCK_ENTITIES.register("vertical_step", () -> {
                BlockEntityType<VerticalStepBlockEntity> type = new BlockEntityType<>(
                        VerticalStepBlockEntity::new,
                        java.util.Set.of(ModBlocks.VERTICAL_STEP.get())
                );
                CommonBlockEntities.VERTICAL_STEP = type;
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuadVerticalStepBlockEntity>> QUAD_VERTICAL_STEP =
            BLOCK_ENTITIES.register("quad_vertical_step", () -> {
                BlockEntityType<QuadVerticalStepBlockEntity> type = new BlockEntityType<>(
                        QuadVerticalStepBlockEntity::new,
                        java.util.Set.of(ModBlocks.QUAD_VERTICAL_STEP.get())
                );
                CommonBlockEntities.QUAD_VERTICAL_STEP = type;
                return type;
            });

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
        BitsAndBalance.LOGGER.info("Registered Azalea sign and hanging sign block entities");
    }
}

