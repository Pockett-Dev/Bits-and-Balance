package org.onenonly.bitsandbalance;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.onenonly.bitsandbalance.blocks.AzaleaWoodBlock;
import org.onenonly.bitsandbalance.blocks.AzaleaRotatedPillarBlock;
import org.onenonly.bitsandbalance.blocks.AzaleaSlabBlock;
import org.onenonly.bitsandbalance.blocks.AzaleaStairBlock;
import org.onenonly.bitsandbalance.blocks.AzaleaButtonBlock;
import org.onenonly.bitsandbalance.blocks.AzaleaPressurePlateBlock;
import org.onenonly.bitsandbalance.blocks.AzaleaFenceBlock;
import org.onenonly.bitsandbalance.blocks.AzaleaFenceGateBlock;
import org.onenonly.bitsandbalance.blocks.AzaleaDoorBlock;
import org.onenonly.bitsandbalance.blocks.AzaleaTrapDoorBlock;
import org.onenonly.bitsandbalance.blocks.AzaleaStandingSignBlock;
import org.onenonly.bitsandbalance.blocks.AzaleaWallSignBlock;
import org.onenonly.bitsandbalance.blocks.AzaleaHangingSignBlock;
import org.onenonly.bitsandbalance.blocks.AzaleaWallHangingSignBlock;
import org.onenonly.bitsandbalance.registry.ModWoodTypes;
import org.onenonly.bitsandbalance.common.blocks.CandleBundleBlock;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepCrackProxyBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabCrackProxyBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepCrackProxyBlock;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;


/**
 * Registers custom blocks for the Bits and Balance mod.
 */
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BitsAndBalance.MODID);

    public static final DeferredHolder<Block, CandleBundleBlock> BUNDLE_CANDLE = BLOCKS.register("bundle_candle",
            (Identifier key) -> new CandleBundleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CANDLE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
            ));

    public static final DeferredHolder<Block, MixedSlabBlock> MIXED_SLAB = BLOCKS.register("mixed_slab",
            (Identifier key) -> {
                MixedSlabBlock block = new MixedSlabBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(2.0F, 6.0F)
                        .sound(SoundType.STONE)
                        .setId(ResourceKey.create(Registries.BLOCK, key)));
                CommonBlocks.MIXED_SLAB = block;
                return block;
            });

    public static final DeferredHolder<Block, VerticalSlabBlock> VERTICAL_SLAB = BLOCKS.register("vertical_slab",
            (Identifier key) -> {
                VerticalSlabBlock block = new VerticalSlabBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(2.0F, 6.0F)
                        .sound(SoundType.STONE)
                        .setId(ResourceKey.create(Registries.BLOCK, key)));
                CommonBlocks.VERTICAL_SLAB = block;
                return block;
            });

    /** Proxy block that supplies the correct half-prism geometry for the crack-breaking animation. */
    public static final DeferredHolder<Block, VerticalSlabCrackProxyBlock> VERTICAL_SLAB_CRACK_PROXY = BLOCKS.register("vertical_slab_crack_proxy",
            (Identifier key) -> {
                VerticalSlabCrackProxyBlock block = new VerticalSlabCrackProxyBlock(
                        BlockBehaviour.Properties.of()
                                .noCollision()
                                .noOcclusion()
                                .strength(-1.0F)
                                .setId(ResourceKey.create(Registries.BLOCK, key)));
                CommonBlocks.VERTICAL_SLAB_CRACK_PROXY = block;
                return block;
            });

        /** Proxy block that supplies the correct quarter-step geometry for crack rendering. */
        public static final DeferredHolder<Block, StepCrackProxyBlock> STEP_CRACK_PROXY = BLOCKS.register("step_crack_proxy",
                        (Identifier key) -> {
                                StepCrackProxyBlock block = new StepCrackProxyBlock(
                                                BlockBehaviour.Properties.of()
                                                                .noCollision()
                                                                .noOcclusion()
                                                                .strength(-1.0F)
                                                                .setId(ResourceKey.create(Registries.BLOCK, key)));
                                CommonBlocks.STEP_CRACK_PROXY = block;
                                return block;
                        });

    /** Generic ¼-block step container (single step, runtime-registered per slab). */
    public static final DeferredHolder<Block, StepBlock> STEP = BLOCKS.register("step",
            (Identifier key) -> {
                StepBlock block = new StepBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(2.0F, 6.0F)
                        .noOcclusion()
                        .sound(SoundType.STONE)
                        .setId(ResourceKey.create(Registries.BLOCK, key)));
                CommonBlocks.STEP = block;
                return block;
            });

    /** Quad step container — holds 2–4 different step quadrants in one block position. */
    public static final DeferredHolder<Block, QuadStepBlock> QUAD_STEP = BLOCKS.register("quad_step",
            (Identifier key) -> {
                QuadStepBlock block = new QuadStepBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(2.0F, 6.0F)
                        .noOcclusion()
                        .sound(SoundType.STONE)
                        .setId(ResourceKey.create(Registries.BLOCK, key)));
                CommonBlocks.QUAD_STEP = block;
                return block;
            });

    /** Generic vertical step container (single vertical step, runtime-registered per vertical slab). */
    public static final DeferredHolder<Block, VerticalStepBlock> VERTICAL_STEP = BLOCKS.register("vertical_step",
            (Identifier key) -> {
                VerticalStepBlock block = new VerticalStepBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(2.0F, 6.0F)
                        .noOcclusion()
                        .sound(SoundType.STONE)
                        .setId(ResourceKey.create(Registries.BLOCK, key)));
                CommonBlocks.VERTICAL_STEP = block;
                return block;
            });

        /** Proxy block that supplies the correct quarter-vertical-step geometry for crack rendering. */
        public static final DeferredHolder<Block, VerticalStepCrackProxyBlock> VERTICAL_STEP_CRACK_PROXY = BLOCKS.register("vertical_step_crack_proxy",
                        (Identifier key) -> {
                                VerticalStepCrackProxyBlock block = new VerticalStepCrackProxyBlock(
                                                BlockBehaviour.Properties.of()
                                                                .noCollision()
                                                                .noOcclusion()
                                                                .strength(-1.0F)
                                                                .setId(ResourceKey.create(Registries.BLOCK, key)));
                                CommonBlocks.VERTICAL_STEP_CRACK_PROXY = block;
                                return block;
                        });

    /** Quad vertical step container — holds 2–4 different vertical step quadrants in one block position. */
    public static final DeferredHolder<Block, QuadVerticalStepBlock> QUAD_VERTICAL_STEP = BLOCKS.register("quad_vertical_step",
            (Identifier key) -> {
                QuadVerticalStepBlock block = new QuadVerticalStepBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(2.0F, 6.0F)
                        .noOcclusion()
                        .sound(SoundType.STONE)
                        .setId(ResourceKey.create(Registries.BLOCK, key)));
                CommonBlocks.QUAD_VERTICAL_STEP = block;
                return block;
            });

    // Azalea Wood Blocks
    public static final DeferredHolder<Block, AzaleaRotatedPillarBlock> AZALEA_LOG = BLOCKS.register("azalea_log",
            (Identifier key) -> {
                BitsAndBalance.LOGGER.info("Registering Azalea Log block");
                return new AzaleaRotatedPillarBlock(BlockBehaviour.Properties.of()
                        .setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_PINK)
                        .strength(2.0F)
                        .sound(SoundType.WOOD));
            });
    
    public static final DeferredHolder<Block, AzaleaRotatedPillarBlock> AZALEA_WOOD = BLOCKS.register("azalea_wood",
            (Identifier key) -> new AzaleaRotatedPillarBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));
    
    public static final DeferredHolder<Block, AzaleaRotatedPillarBlock> STRIPPED_AZALEA_LOG = BLOCKS.register("stripped_azalea_log",
            (Identifier key) -> new AzaleaRotatedPillarBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));
    
    public static final DeferredHolder<Block, AzaleaRotatedPillarBlock> STRIPPED_AZALEA_WOOD = BLOCKS.register("stripped_azalea_wood",
            (Identifier key) -> new AzaleaRotatedPillarBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));
    
    public static final DeferredHolder<Block, AzaleaWoodBlock> AZALEA_PLANKS = BLOCKS.register("azalea_planks",
            (Identifier key) -> new AzaleaWoodBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));

    // Azalea Wood Derivatives
    public static final DeferredHolder<Block, AzaleaSlabBlock> AZALEA_SLAB = BLOCKS.register("azalea_slab",
            (Identifier key) -> new AzaleaSlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)
                    .setId(ResourceKey.create(Registries.BLOCK, key))));
    
    public static final DeferredHolder<Block, AzaleaStairBlock> AZALEA_STAIRS = BLOCKS.register("azalea_stairs",
            (Identifier key) -> new AzaleaStairBlock(AZALEA_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS)
                    .setId(ResourceKey.create(Registries.BLOCK, key))));
    
    public static final DeferredHolder<Block, AzaleaButtonBlock> AZALEA_BUTTON = BLOCKS.register("azalea_button",
            (Identifier key) -> new AzaleaButtonBlock(ModWoodTypes.AZALEA_BLOCK_SET, 30, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)
                    .setId(ResourceKey.create(Registries.BLOCK, key))));
    
    public static final DeferredHolder<Block, AzaleaPressurePlateBlock> AZALEA_PRESSURE_PLATE = BLOCKS.register("azalea_pressure_plate",
            (Identifier key) -> new AzaleaPressurePlateBlock(ModWoodTypes.AZALEA_BLOCK_SET, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))));
    
    public static final DeferredHolder<Block, AzaleaFenceBlock> AZALEA_FENCE = BLOCKS.register("azalea_fence",
            (Identifier key) -> {
                BitsAndBalance.LOGGER.info("Registering Azalea Fence block");
                return new AzaleaFenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)
                        .setId(ResourceKey.create(Registries.BLOCK, key)));
            });
    
    public static final DeferredHolder<Block, AzaleaFenceGateBlock> AZALEA_FENCE_GATE = BLOCKS.register("azalea_fence_gate",
            (Identifier key) -> new AzaleaFenceGateBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))));

    public static final DeferredHolder<Block, ShelfBlock> AZALEA_SHELF = BLOCKS.register("azalea_shelf",
            (Identifier key) -> new ShelfBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_SHELF)
                    .overrideLootTable(java.util.Optional.of(ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "blocks/azalea_shelf"))))
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_PINK)));
    
    public static final DeferredHolder<Block, AzaleaDoorBlock> AZALEA_DOOR = BLOCKS.register("azalea_door",
            (Identifier key) -> new AzaleaDoorBlock(ModWoodTypes.AZALEA_BLOCK_SET, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR)
                    .setId(ResourceKey.create(Registries.BLOCK, key))));
    
    public static final DeferredHolder<Block, AzaleaTrapDoorBlock> AZALEA_TRAPDOOR = BLOCKS.register("azalea_trapdoor",
            (Identifier key) -> new AzaleaTrapDoorBlock(ModWoodTypes.AZALEA_BLOCK_SET, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR)
                    .setId(ResourceKey.create(Registries.BLOCK, key))));
    
    public static final DeferredHolder<Block, AzaleaStandingSignBlock> AZALEA_SIGN = BLOCKS.register("azalea_sign",
            (Identifier key) -> new AzaleaStandingSignBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN)
                    .setId(ResourceKey.create(Registries.BLOCK, key))));
    
    public static final DeferredHolder<Block, AzaleaWallSignBlock> AZALEA_WALL_SIGN = BLOCKS.register("azalea_wall_sign",
            (Identifier key) -> new AzaleaWallSignBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_SIGN)
                    .setId(ResourceKey.create(Registries.BLOCK, key))));
    
    public static final DeferredHolder<Block, AzaleaHangingSignBlock> AZALEA_HANGING_SIGN = BLOCKS.register("azalea_hanging_sign",
            (Identifier key) -> new AzaleaHangingSignBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN)
                    .setId(ResourceKey.create(Registries.BLOCK, key))));
    
    public static final DeferredHolder<Block, AzaleaWallHangingSignBlock> AZALEA_WALL_HANGING_SIGN = BLOCKS.register("azalea_wall_hanging_sign",
            (Identifier key) -> new AzaleaWallHangingSignBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_HANGING_SIGN)
                    .setId(ResourceKey.create(Registries.BLOCK, key))));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}