package org.onenonly.bitsandbalance.fabric.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blockentity.CandleBundleBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.TemporaryCloudBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.compat.BlockEntityTypeExt;

public final class FabricBlockEntities {
    public static final Identifier CANDLE_BUNDLE_ID  = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "candle_bundle");
    public static final Identifier TEMPORARY_CLOUD_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "temporary_cloud");
    public static final Identifier MIXED_SLAB_ID     = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "mixed_slab");
    public static final Identifier VERTICAL_SLAB_ID  = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_slab");
    public static final Identifier STEP_ID                = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "step");
    public static final Identifier QUAD_STEP_ID           = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "quad_step");
    public static final Identifier VERTICAL_STEP_ID       = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "vertical_step");
    public static final Identifier QUAD_VERTICAL_STEP_ID  = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "quad_vertical_step");

    public static BlockEntityType<CandleBundleBlockEntity> CANDLE_BUNDLE;
    public static BlockEntityType<TemporaryCloudBlockEntity> TEMPORARY_CLOUD;
    public static BlockEntityType<MixedSlabBlockEntity>    MIXED_SLAB;
    public static BlockEntityType<VerticalSlabBlockEntity> VERTICAL_SLAB;
    public static BlockEntityType<StepBlockEntity>              STEP;
    public static BlockEntityType<QuadStepBlockEntity>          QUAD_STEP;
    public static BlockEntityType<VerticalStepBlockEntity>      VERTICAL_STEP;
    public static BlockEntityType<QuadVerticalStepBlockEntity>  QUAD_VERTICAL_STEP;

    private static boolean registered;

    private FabricBlockEntities() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        CANDLE_BUNDLE = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                CANDLE_BUNDLE_ID,
            FabricBlockEntityTypeBuilder.create(CandleBundleBlockEntity::new, FabricCandleBundleContent.BUNDLE_CANDLE).build()
        );
        CommonBlockEntities.CANDLE_BUNDLE = CANDLE_BUNDLE;

        TEMPORARY_CLOUD = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            TEMPORARY_CLOUD_ID,
            FabricBlockEntityTypeBuilder.create(TemporaryCloudBlockEntity::new, ModBottleOfCloud.cloudBlock()).build()
        );
        CommonBlockEntities.TEMPORARY_CLOUD = TEMPORARY_CLOUD;

        MIXED_SLAB = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                MIXED_SLAB_ID,
            FabricBlockEntityTypeBuilder.create(MixedSlabBlockEntity::new, FabricMixedSlabContent.MIXED_SLAB).build()
        );
        CommonBlockEntities.MIXED_SLAB = MIXED_SLAB;

        VERTICAL_SLAB = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                VERTICAL_SLAB_ID,
            FabricBlockEntityTypeBuilder.create(VerticalSlabBlockEntity::new, FabricVerticalSlabContent.VERTICAL_SLAB).build()
        );
        CommonBlockEntities.VERTICAL_SLAB = VERTICAL_SLAB;

        try {
            java.util.Collection<net.minecraft.world.level.block.Block> dynamic = VerticalSlabDynamicRegistry.getAllVerticalBlocksSnapshot();
            if (!dynamic.isEmpty()) {
                ((BlockEntityTypeExt) (Object) VERTICAL_SLAB)
                        .bitsandbalance$addValidBlocks(dynamic.toArray(new net.minecraft.world.level.block.Block[0]));
            }
        } catch (Throwable ignored) {
        }

        STEP = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                STEP_ID,
                FabricBlockEntityTypeBuilder.create(StepBlockEntity::new, FabricStepContent.STEP).build()
        );
        CommonBlockEntities.STEP = STEP;

        QUAD_STEP = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                QUAD_STEP_ID,
                FabricBlockEntityTypeBuilder.create(QuadStepBlockEntity::new, FabricStepContent.QUAD_STEP).build()
        );
        CommonBlockEntities.QUAD_STEP = QUAD_STEP;

        try {
            java.util.Collection<net.minecraft.world.level.block.Block> dynamicSteps = StepDynamicRegistry.getAllStepBlocksSnapshot();
            if (!dynamicSteps.isEmpty()) {
                ((BlockEntityTypeExt) (Object) STEP)
                        .bitsandbalance$addValidBlocks(dynamicSteps.toArray(new net.minecraft.world.level.block.Block[0]));
            }
        } catch (Throwable ignored) {
        }

        VERTICAL_STEP = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                VERTICAL_STEP_ID,
                FabricBlockEntityTypeBuilder.create(VerticalStepBlockEntity::new,
                        FabricVerticalStepContent.VERTICAL_STEP).build()
        );
        CommonBlockEntities.VERTICAL_STEP = VERTICAL_STEP;

        QUAD_VERTICAL_STEP = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                QUAD_VERTICAL_STEP_ID,
                FabricBlockEntityTypeBuilder.create(QuadVerticalStepBlockEntity::new,
                        FabricVerticalStepContent.QUAD_VERTICAL_STEP).build()
        );
        CommonBlockEntities.QUAD_VERTICAL_STEP = QUAD_VERTICAL_STEP;

        try {
            java.util.Collection<net.minecraft.world.level.block.Block> dynamicVerticalSteps =
                    VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot();
            if (!dynamicVerticalSteps.isEmpty()) {
                ((BlockEntityTypeExt) (Object) VERTICAL_STEP)
                        .bitsandbalance$addValidBlocks(
                                dynamicVerticalSteps.toArray(new net.minecraft.world.level.block.Block[0]));
            }
        } catch (Throwable ignored) {
        }
    }
}
