package org.onenonly.bitsandbalance.fabric.client;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class FabricDynamicVerticalStepModelPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-dynamic-vertical-step-models");
    private static final String VERTICAL_STEP_BLOCKSTATE_JSON = """
            {
              \"multipart\": [
                {
                  \"apply\": {
                    \"model\": \"bitsandbalance:block/vertical_step\"
                  }
                }
              ]
            }
            """;
        private static final String STEP_BLOCKSTATE_JSON = """
                        {
                            \"multipart\": [
                                {
                                    \"apply\": {
                                        \"model\": \"bitsandbalance:block/step\"
                                    }
                                }
                            ]
                        }
                        """;
        private static final String VERTICAL_SLAB_BLOCKSTATE_JSON = """
                        {
                            \"multipart\": [
                                {
                                    \"apply\": {
                                        \"model\": \"bitsandbalance:block/vertical_slab\"
                                    }
                                }
                            ]
                        }
                        """;

    private FabricDynamicVerticalStepModelPlugin() {
    }

    public static void init() {
        ModelLoadingPlugin.register(pluginContext -> {
            BlockModelDefinition verticalStepDefinition = bitsandbalance$parseDefinition(
                    VERTICAL_STEP_BLOCKSTATE_JSON,
                    "dynamic vertical step"
            );
            BlockModelDefinition stepDefinition = bitsandbalance$parseDefinition(
                    STEP_BLOCKSTATE_JSON,
                    "dynamic step"
            );
                BlockModelDefinition verticalSlabDefinition = bitsandbalance$parseDefinition(
                    VERTICAL_SLAB_BLOCKSTATE_JSON,
                    "dynamic vertical slab"
                );
                if (verticalStepDefinition == null || stepDefinition == null || verticalSlabDefinition == null) {
                return;
            }

            for (Block block : BuiltInRegistries.BLOCK) {
                Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
                if (blockId == null || !BitsAndBalanceCommon.MOD_ID.equals(blockId.getNamespace())) {
                    continue;
                }
                if (bitsandbalance$hasPackagedBlockstate(blockId)) {
                    continue;
                }

                BlockModelDefinition modelDefinition;
                if (block instanceof FixedVerticalSlabBlock) {
                    modelDefinition = verticalSlabDefinition;
                } else if (block instanceof FixedVerticalStepBlock) {
                    modelDefinition = verticalStepDefinition;
                } else if (block instanceof FixedStepBlock) {
                    modelDefinition = stepDefinition;
                } else {
                    continue;
                }

                pluginContext.registerBlockStateResolver(block, context -> {
                    Map<BlockState, net.minecraft.client.renderer.block.model.BlockStateModel.UnbakedRoot> resolvedModels =
                            modelDefinition.instantiate(block.getStateDefinition(), blockId::toString);
                    for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                        net.minecraft.client.renderer.block.model.BlockStateModel.UnbakedRoot model = resolvedModels.get(state);
                        if (model != null) {
                            context.setModel(state, model);
                        }
                    }
                });
            }
        });
    }

    private static boolean bitsandbalance$hasPackagedBlockstate(Identifier blockId) {
        String resourcePath = "assets/" + blockId.getNamespace() + "/blockstates/" + blockId.getPath() + ".json";
        return FabricDynamicVerticalStepModelPlugin.class.getClassLoader().getResource(resourcePath) != null;
    }

    private static BlockModelDefinition bitsandbalance$parseDefinition(String json, String label) {
        return BlockModelDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .resultOrPartial(error -> LOGGER.error("Failed to parse {} blockstate template: {}", label, error))
                .orElse(null);
    }
}