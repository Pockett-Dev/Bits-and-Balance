package org.onenonly.bitsandbalance.fabric.client;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabFabricRendererModelBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
        try {
            Class<?> pluginClass = Class.forName("net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin");
            Object plugin = Proxy.newProxyInstance(
                    FabricDynamicVerticalStepModelPlugin.class.getClassLoader(),
                    new Class<?>[]{pluginClass},
                    (proxy, method, args) -> {
                        if ("initialize".equals(method.getName()) && args != null && args.length == 1) {
                            bitsandbalance$registerFabricRendererModelWrapper(args[0]);
                            bitsandbalance$registerResolvers(args[0]);
                        }
                        return null;
                    }
            );
            pluginClass.getMethod("register", pluginClass).invoke(null, plugin);
        } catch (ClassNotFoundException ignored) {
            // The non-Loom 26.1 dev runtime omits this optional Fabric module; generated resources cover that path.
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("Unable to register dynamic slab-family blockstate resolvers", exception);
        }
    }

    private static void bitsandbalance$registerFabricRendererModelWrapper(Object pluginContext) {
        Method modifyBlockModelAfterBake = bitsandbalance$findMethod(pluginContext.getClass(), "modifyBlockModelAfterBake", 0);
        if (modifyBlockModelAfterBake == null) {
            LOGGER.warn("Unable to find Fabric model-loading after-bake event");
            return;
        }

        Class<?> listenerClass;
        try {
            listenerClass = Class.forName("net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier$AfterBakeBlock");
        } catch (ClassNotFoundException exception) {
            LOGGER.warn("Unable to find Fabric after-bake block model modifier API", exception);
            return;
        }

        Object listener = Proxy.newProxyInstance(
                FabricDynamicVerticalStepModelPlugin.class.getClassLoader(),
                new Class<?>[]{listenerClass},
                (proxy, method, args) -> {
                    if ("modifyModelAfterBake".equals(method.getName()) && args != null && args.length == 2) {
                        BlockState state = bitsandbalance$contextState(args[1]);
                        return EnhancedSlabFabricRendererModelBridge.wrap(args[0], state);
                    }
                    return args != null && args.length > 0 ? args[0] : null;
                }
        );

        try {
            Object event = modifyBlockModelAfterBake.invoke(pluginContext);
            Method register = bitsandbalance$findMethod(event.getClass(), "register", 1);
            if (register == null) {
                LOGGER.warn("Unable to find Fabric after-bake event register method");
                return;
            }
            register.invoke(event, listener);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("Unable to register enhanced slab-family Fabric renderer model wrapper", exception);
        }
    }

    private static BlockState bitsandbalance$contextState(Object context) {
        if (context == null) {
            return null;
        }
        Method state = bitsandbalance$findMethod(context.getClass(), "state", 0);
        if (state == null) {
            return null;
        }
        try {
            Object value = state.invoke(context);
            return value instanceof BlockState blockState ? blockState : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static void bitsandbalance$registerResolvers(Object pluginContext) {
        BlockStateModelDispatcher verticalStepDefinition = bitsandbalance$parseDefinition(
                VERTICAL_STEP_BLOCKSTATE_JSON,
                "dynamic vertical step"
        );
        BlockStateModelDispatcher stepDefinition = bitsandbalance$parseDefinition(
                STEP_BLOCKSTATE_JSON,
                "dynamic step"
        );
        BlockStateModelDispatcher verticalSlabDefinition = bitsandbalance$parseDefinition(
                VERTICAL_SLAB_BLOCKSTATE_JSON,
                "dynamic vertical slab"
        );
        if (verticalStepDefinition == null || stepDefinition == null || verticalSlabDefinition == null) {
            return;
        }

        Method registerBlockStateResolver = bitsandbalance$findMethod(
                pluginContext.getClass(),
                "registerBlockStateResolver",
                2
        );
        if (registerBlockStateResolver == null) {
            LOGGER.warn("Unable to find Fabric model-loading registerBlockStateResolver method");
            return;
        }

        Class<?> resolverClass;
        try {
            resolverClass = Class.forName("net.fabricmc.fabric.api.client.model.loading.v1.BlockStateResolver");
        } catch (ClassNotFoundException exception) {
            LOGGER.warn("Unable to find Fabric blockstate resolver API", exception);
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

            BlockStateModelDispatcher modelDefinition;
            if (block instanceof FixedVerticalSlabBlock || block instanceof VerticalSlabBlock || VerticalSlabDynamicRegistry.getSlabForVertical(block) != null) {
                modelDefinition = verticalSlabDefinition;
            } else if (block instanceof FixedVerticalStepBlock || block instanceof VerticalStepBlock || VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(block) != null) {
                modelDefinition = verticalStepDefinition;
            } else if (block instanceof FixedStepBlock || block instanceof StepBlock || StepDynamicRegistry.getSlabForStep(block) != null) {
                modelDefinition = stepDefinition;
            } else {
                continue;
            }

            Object resolver = Proxy.newProxyInstance(
                    FabricDynamicVerticalStepModelPlugin.class.getClassLoader(),
                    new Class<?>[]{resolverClass},
                    (proxy, method, args) -> {
                        if ("resolveBlockStates".equals(method.getName()) && args != null && args.length == 1) {
                            bitsandbalance$setResolvedModels(args[0], block, blockId, modelDefinition);
                        }
                        return null;
                    }
            );

            try {
                registerBlockStateResolver.invoke(pluginContext, block, resolver);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                LOGGER.warn("Unable to register dynamic blockstate resolver for {}", blockId, exception);
            }
        }
    }

    private static void bitsandbalance$setResolvedModels(
            Object resolverContext,
            Block block,
            Identifier blockId,
            BlockStateModelDispatcher modelDefinition
    ) {
        Method setModel = bitsandbalance$findMethod(resolverContext.getClass(), "setModel", 2);
        if (setModel == null) {
            LOGGER.warn("Unable to find Fabric blockstate resolver setModel method for {}", blockId);
            return;
        }

        Map<BlockState, BlockStateModel.UnbakedRoot> resolvedModels =
                modelDefinition.instantiate(block.getStateDefinition(), blockId::toString);
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            BlockStateModel.UnbakedRoot model = resolvedModels.get(state);
            if (model == null) {
                continue;
            }
            try {
                setModel.invoke(resolverContext, state, model);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                LOGGER.warn("Unable to assign dynamic blockstate model for {}", blockId, exception);
                return;
            }
        }
    }

    private static boolean bitsandbalance$hasPackagedBlockstate(Identifier blockId) {
        String resourcePath = "assets/" + blockId.getNamespace() + "/blockstates/" + blockId.getPath() + ".json";
        return FabricDynamicVerticalStepModelPlugin.class.getClassLoader().getResource(resourcePath) != null;
    }

    private static BlockStateModelDispatcher bitsandbalance$parseDefinition(String json, String label) {
        return BlockStateModelDispatcher.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .resultOrPartial(error -> LOGGER.error("Failed to parse {} blockstate template: {}", label, error))
                .orElse(null);
    }

    private static Method bitsandbalance$findMethod(Class<?> type, String name, int parameterCount) {
        for (Method method : type.getMethods()) {
            if (name.equals(method.getName()) && method.getParameterCount() == parameterCount) {
                try {
                    method.setAccessible(true);
                } catch (RuntimeException ignored) {
                }
                return method;
            }
        }
        return null;
    }
}
