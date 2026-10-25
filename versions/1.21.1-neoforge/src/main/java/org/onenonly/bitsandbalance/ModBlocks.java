package org.onenonly.bitsandbalance;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import org.onenonly.bitsandbalance.common.blocks.EntityMixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepCrackProxyBlock;
import org.onenonly.bitsandbalance.common.blocks.StorageQuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StorageQuadVerticalStepBlock;
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
            (ResourceLocation key) -> new CandleBundleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CANDLE)
                    
            ));

    public static final DeferredHolder<Block, MixedSlabBlock> MIXED_SLAB = BLOCKS.register("mixed_slab",
            (ResourceLocation key) -> {
                MixedSlabBlock block = new EntityMixedSlabBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(2.0F, 6.0F)
                        .sound(SoundType.STONE)
                        .noOcclusion()
                        .dynamicShape()
                        );
                CommonBlocks.MIXED_SLAB = block;
                return block;
            });

    public static final DeferredHolder<Block, VerticalSlabBlock> VERTICAL_SLAB = BLOCKS.register("vertical_slab",
            (ResourceLocation key) -> {
                VerticalSlabBlock block = new VerticalSlabBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(2.0F, 6.0F)
                        .sound(SoundType.STONE)
                        );
                CommonBlocks.VERTICAL_SLAB = block;
                return block;
            });

    /** Proxy block that supplies the correct half-prism geometry for the crack-breaking animation. */
    public static final DeferredHolder<Block, VerticalSlabCrackProxyBlock> VERTICAL_SLAB_CRACK_PROXY = BLOCKS.register("vertical_slab_crack_proxy",
            (ResourceLocation key) -> {
                VerticalSlabCrackProxyBlock block = new VerticalSlabCrackProxyBlock(
                        BlockBehaviour.Properties.of()
                                .noCollission()
                                .noOcclusion()
                                .strength(-1.0F)
                                );
                CommonBlocks.VERTICAL_SLAB_CRACK_PROXY = block;
                return block;
            });

        /** Proxy block that supplies the correct quarter-step geometry for crack rendering. */
        public static final DeferredHolder<Block, StepCrackProxyBlock> STEP_CRACK_PROXY = BLOCKS.register("step_crack_proxy",
                        (ResourceLocation key) -> {
                                StepCrackProxyBlock block = new StepCrackProxyBlock(
                                                BlockBehaviour.Properties.of()
                                                                .noCollission()
                                                                .noOcclusion()
                                                                .strength(-1.0F)
                                                                );
                                CommonBlocks.STEP_CRACK_PROXY = block;
                                return block;
                        });

    /** Generic Â¼-block step container (single step, runtime-registered per slab). */
    public static final DeferredHolder<Block, StepBlock> STEP = BLOCKS.register("step",
            (ResourceLocation key) -> {
                StepBlock block = new StepBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(2.0F, 6.0F)
                        .noOcclusion()
                        .sound(SoundType.STONE)
                        );
                CommonBlocks.STEP = block;
                return block;
            });

    /** Quad step container â€” holds 2â€“4 different step quadrants in one block position. */
    public static final DeferredHolder<Block, QuadStepBlock> QUAD_STEP = BLOCKS.register("quad_step",
            (ResourceLocation key) -> {
                QuadStepBlock block = new StorageQuadStepBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(2.0F, 6.0F)
                        .noOcclusion()
                        .dynamicShape()
                        .sound(SoundType.STONE)
                        );
                CommonBlocks.QUAD_STEP = block;
                return block;
            });

    /** Generic vertical step container (single vertical step, runtime-registered per vertical slab). */
    public static final DeferredHolder<Block, VerticalStepBlock> VERTICAL_STEP = BLOCKS.register("vertical_step",
            (ResourceLocation key) -> {
                VerticalStepBlock block = new VerticalStepBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(2.0F, 6.0F)
                        .noOcclusion()
                        .sound(SoundType.STONE)
                        );
                CommonBlocks.VERTICAL_STEP = block;
                return block;
            });

        /** Proxy block that supplies the correct quarter-vertical-step geometry for crack rendering. */
        public static final DeferredHolder<Block, VerticalStepCrackProxyBlock> VERTICAL_STEP_CRACK_PROXY = BLOCKS.register("vertical_step_crack_proxy",
                        (ResourceLocation key) -> {
                                VerticalStepCrackProxyBlock block = new VerticalStepCrackProxyBlock(
                                                BlockBehaviour.Properties.of()
                                                                .noCollission()
                                                                .noOcclusion()
                                                                .strength(-1.0F)
                                                                );
                                CommonBlocks.VERTICAL_STEP_CRACK_PROXY = block;
                                return block;
                        });

    /** Quad vertical step container â€” holds 2â€“4 different vertical step quadrants in one block position. */
    public static final DeferredHolder<Block, QuadVerticalStepBlock> QUAD_VERTICAL_STEP = BLOCKS.register("quad_vertical_step",
            (ResourceLocation key) -> {
                QuadVerticalStepBlock block = new StorageQuadVerticalStepBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(2.0F, 6.0F)
                        .noOcclusion()
                        .dynamicShape()
                        .sound(SoundType.STONE)
                        );
                CommonBlocks.QUAD_VERTICAL_STEP = block;
                return block;
            });

    // Azalea Wood Blocks
    public static final DeferredHolder<Block, AzaleaRotatedPillarBlock> AZALEA_LOG = BLOCKS.register("azalea_log",
            (ResourceLocation key) -> {
                BitsAndBalance.LOGGER.info("Registering Azalea Log block");
                return new AzaleaRotatedPillarBlock(BlockBehaviour.Properties.of()
                        
                        .mapColor(MapColor.COLOR_PINK)
                        .strength(2.0F)
                        .sound(SoundType.WOOD));
            });
    
    public static final DeferredHolder<Block, AzaleaRotatedPillarBlock> AZALEA_WOOD = BLOCKS.register("azalea_wood",
            (ResourceLocation key) -> new AzaleaRotatedPillarBlock(BlockBehaviour.Properties.of()
                    
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));
    
    public static final DeferredHolder<Block, AzaleaRotatedPillarBlock> STRIPPED_AZALEA_LOG = BLOCKS.register("stripped_azalea_log",
            (ResourceLocation key) -> new AzaleaRotatedPillarBlock(BlockBehaviour.Properties.of()
                    
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));
    
    public static final DeferredHolder<Block, AzaleaRotatedPillarBlock> STRIPPED_AZALEA_WOOD = BLOCKS.register("stripped_azalea_wood",
            (ResourceLocation key) -> new AzaleaRotatedPillarBlock(BlockBehaviour.Properties.of()
                    
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));
    
    public static final DeferredHolder<Block, AzaleaWoodBlock> AZALEA_PLANKS = BLOCKS.register("azalea_planks",
            (ResourceLocation key) -> new AzaleaWoodBlock(BlockBehaviour.Properties.of()
                    
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));

    // Azalea Wood Derivatives
    public static final DeferredHolder<Block, AzaleaSlabBlock> AZALEA_SLAB = BLOCKS.register("azalea_slab",
            (ResourceLocation key) -> new AzaleaSlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)
                    ));
    
    public static final DeferredHolder<Block, AzaleaStairBlock> AZALEA_STAIRS = BLOCKS.register("azalea_stairs",
            (ResourceLocation key) -> new AzaleaStairBlock(AZALEA_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS)
                    ));
    
    public static final DeferredHolder<Block, AzaleaButtonBlock> AZALEA_BUTTON = BLOCKS.register("azalea_button",
            (ResourceLocation key) -> new AzaleaButtonBlock(ModWoodTypes.AZALEA_BLOCK_SET, 30, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)
                    ));
    
    public static final DeferredHolder<Block, AzaleaPressurePlateBlock> AZALEA_PRESSURE_PLATE = BLOCKS.register("azalea_pressure_plate",
            (ResourceLocation key) -> new AzaleaPressurePlateBlock(ModWoodTypes.AZALEA_BLOCK_SET, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)
                    ));
    
    public static final DeferredHolder<Block, AzaleaFenceBlock> AZALEA_FENCE = BLOCKS.register("azalea_fence",
            (ResourceLocation key) -> {
                BitsAndBalance.LOGGER.info("Registering Azalea Fence block");
                return new AzaleaFenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)
                        );
            });
    
    public static final DeferredHolder<Block, AzaleaFenceGateBlock> AZALEA_FENCE_GATE = BLOCKS.register("azalea_fence_gate",
            (ResourceLocation key) -> new AzaleaFenceGateBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE)
                    ));
    
    public static final DeferredHolder<Block, AzaleaDoorBlock> AZALEA_DOOR = BLOCKS.register("azalea_door",
            (ResourceLocation key) -> new AzaleaDoorBlock(ModWoodTypes.AZALEA_BLOCK_SET, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR)
                    ));
    
    public static final DeferredHolder<Block, AzaleaTrapDoorBlock> AZALEA_TRAPDOOR = BLOCKS.register("azalea_trapdoor",
            (ResourceLocation key) -> new AzaleaTrapDoorBlock(ModWoodTypes.AZALEA_BLOCK_SET, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR)
                    ));
    
    public static final DeferredHolder<Block, AzaleaStandingSignBlock> AZALEA_SIGN = BLOCKS.register("azalea_sign",
            (ResourceLocation key) -> new AzaleaStandingSignBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN)
                    ));
    
    public static final DeferredHolder<Block, AzaleaWallSignBlock> AZALEA_WALL_SIGN = BLOCKS.register("azalea_wall_sign",
            (ResourceLocation key) -> new AzaleaWallSignBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_SIGN)
                    ));
    
    public static final DeferredHolder<Block, AzaleaHangingSignBlock> AZALEA_HANGING_SIGN = BLOCKS.register("azalea_hanging_sign",
            (ResourceLocation key) -> new AzaleaHangingSignBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN)
                    ));
    
    public static final DeferredHolder<Block, AzaleaWallHangingSignBlock> AZALEA_WALL_HANGING_SIGN = BLOCKS.register("azalea_wall_hanging_sign",
            (ResourceLocation key) -> new AzaleaWallHangingSignBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_HANGING_SIGN)
                    ));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
