package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabBlockBakedModel;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport.ModelType;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class FabricDynamicVerticalStepModelPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-dynamic-vertical-step-models");

    private FabricDynamicVerticalStepModelPlugin() {
    }

    public static void init() {
        ModelLoadingPlugin.register(pluginContext -> {
            Map<ResourceLocation, ResourceLocation> itemModelAliases = itemModelAliases();
            if (!itemModelAliases.isEmpty()) {
                pluginContext.resolveModel().register(context -> {
                    ResourceLocation sourceModelId = itemModelAliases.get(context.id());
                    return sourceModelId == null ? null : context.getOrLoadModel(sourceModelId);
                });
                LOGGER.info("Registered dynamic slab-family item model aliases for {} generated items", itemModelAliases.size());
            }

            int registered = 0;

            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                if (blockId == null || !BitsAndBalanceCommon.MOD_ID.equals(blockId.getNamespace())) {
                    continue;
                }
                ModelType modelType = EnhancedSlabItemSupport.modelTypeForBlock(block);
                if (modelType == ModelType.NONE) {
                    continue;
                }

                ResourceLocation placeholderModelId = placeholderBlockModelId(modelType);
                ResourceLocation sourceModelId = availableBlockModelId(SlabFamilyModelSourceSelector.modelSourceBlockForBlock(block));

                pluginContext.registerBlockStateResolver(block, context -> {
                    UnbakedModel model = sourceModelId == null
                            ? new PlaceholderBlockUnbakedModel(placeholderModelId)
                            : new GeneratedSlabBlockUnbakedModel(sourceModelId, modelType, placeholderModelId);
                    for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                        context.setModel(state, model);
                    }
                });
                registered++;
            }

            if (registered > 0) {
                LOGGER.info("Registered dynamic slab-family blockstate resolvers for {} generated blocks", registered);
            }

            registerQuadResolver(pluginContext, CommonBlocks.QUAD_STEP,
                    ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "block/quad_step"),
                    false);
            registerQuadResolver(pluginContext, CommonBlocks.QUAD_VERTICAL_STEP,
                    ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "block/quad_vertical_step"),
                    true);

            registerStorageResolver(pluginContext, CommonBlocks.VERTICAL_SLAB,
                    ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "block/vertical_slab"),
                    ModelType.VERTICAL_SLAB);
        });
    }

    private static void registerQuadResolver(ModelLoadingPlugin.Context pluginContext, Block block, ResourceLocation placeholderModelId, boolean vertical) {
        if (block == null) {
            return;
        }
        pluginContext.registerBlockStateResolver(block, context -> {
            UnbakedModel model = new QuadContainerBlockUnbakedModel(placeholderModelId, vertical);
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                context.setModel(state, model);
            }
        });
    }

    private static void registerStorageResolver(ModelLoadingPlugin.Context pluginContext, Block block, ResourceLocation fallbackModelId, ModelType modelType) {
        if (block == null) {
            return;
        }
        pluginContext.registerBlockStateResolver(block, context -> {
            UnbakedModel model = new StorageContainerBlockUnbakedModel(fallbackModelId, modelType);
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                context.setModel(state, model);
            }
        });
    }

    private static Map<ResourceLocation, ResourceLocation> itemModelAliases() {
        Map<ResourceLocation, ResourceLocation> aliases = new HashMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            ModelType modelType = EnhancedSlabItemSupport.modelTypeForItem(item);
            if (modelType == ModelType.NONE) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null || hasPackagedItemModel(itemId)) {
                continue;
            }

            Block sourceBlock = SlabFamilyModelSourceSelector.modelSourceBlockForItem(item);
            if (sourceBlock == null) {
                continue;
            }

            Item sourceItem = sourceBlock.asItem();
            if (sourceItem == null || sourceItem == Items.AIR) {
                continue;
            }

            ResourceLocation sourceItemId = BuiltInRegistries.ITEM.getKey(sourceItem);
            if (sourceItemId == null) {
                continue;
            }

            aliases.put(itemModelId(itemId), availableItemModelId(sourceItemId));
        }

        return aliases;
    }

    private static ResourceLocation availableItemModelId(ResourceLocation itemId) {
        if ("minecraft".equals(itemId.getNamespace())) {
            ResourceLocation remappedModelId = findModItemModelWithBlockParent(itemId);
            if (remappedModelId != null) {
                return remappedModelId;
            }
        }

        if (hasPackagedItemModel(itemId) && hasPackagedBlockModel(itemId)) {
            return itemModelId(itemId);
        }

        ResourceLocation remappedModelId = findModItemModelWithBlockParent(itemId);
        if (remappedModelId != null) {
            return remappedModelId;
        }

        if (hasPackagedItemModel(itemId)) {
            return itemModelId(itemId);
        }

        for (var mod : FabricLoader.getInstance().getAllMods()) {
            ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(mod.getMetadata().getId(), itemId.getPath());
            if (hasPackagedItemModel(candidate)) {
                return itemModelId(candidate);
            }
        }

        return itemModelId(itemId);
    }

    private static ResourceLocation findModItemModelWithBlockParent(ResourceLocation itemId) {
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            String namespace = mod.getMetadata().getId();
            if (namespace.equals(itemId.getNamespace())) {
                continue;
            }

            ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(namespace, itemId.getPath());
            if (hasPackagedItemModel(candidate) && hasPackagedBlockModel(candidate)) {
                return itemModelId(candidate);
            }
        }

        return null;
    }

    private static ResourceLocation itemModelId(ResourceLocation itemId) {
        return ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "item/" + itemId.getPath());
    }

    private static ResourceLocation placeholderBlockModelId(ModelType modelType) {
        return switch (modelType) {
            case VERTICAL_SLAB -> ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "block/vertical_slab");
            case STEP -> ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "block/step");
            case VERTICAL_STEP -> ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "block/vertical_step");
            case NONE -> null;
        };
    }

    private static ResourceLocation availableBlockModelId(Block block) {
        if (block == null) {
            return null;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (blockId == null) {
            return null;
        }

        if ("minecraft".equals(blockId.getNamespace())) {
            ResourceLocation remappedModelId = findModBlockModel(blockId);
            if (remappedModelId != null) {
                return remappedModelId;
            }
        }

        if (hasPackagedBlockModel(blockId)) {
            return blockModelId(blockId);
        }

        ResourceLocation remappedModelId = findModBlockModel(blockId);
        return remappedModelId != null ? remappedModelId : blockModelId(blockId);
    }

    private static ResourceLocation findModBlockModel(ResourceLocation blockId) {
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            String namespace = mod.getMetadata().getId();
            if (namespace.equals(blockId.getNamespace())) {
                continue;
            }

            ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(namespace, blockId.getPath());
            if (hasPackagedBlockModel(candidate)) {
                return blockModelId(candidate);
            }
        }

        return null;
    }

    private static ResourceLocation blockModelId(ResourceLocation blockId) {
        return ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath());
    }

    private static boolean hasPackagedBlockstate(ResourceLocation blockId) {
        String resourcePath = "assets/" + blockId.getNamespace() + "/blockstates/" + blockId.getPath() + ".json";
        return FabricDynamicVerticalStepModelPlugin.class.getClassLoader().getResource(resourcePath) != null;
    }

    private static boolean hasPackagedItemModel(ResourceLocation itemId) {
        String resourcePath = "assets/" + itemId.getNamespace() + "/models/item/" + itemId.getPath() + ".json";
        return hasPackagedResource(resourcePath);
    }

    private static boolean hasPackagedBlockModel(ResourceLocation blockId) {
        String resourcePath = "assets/" + blockId.getNamespace() + "/models/block/" + blockId.getPath() + ".json";
        return hasPackagedResource(resourcePath);
    }

    private static boolean hasPackagedResource(String resourcePath) {
        if (FabricDynamicVerticalStepModelPlugin.class.getClassLoader().getResource(resourcePath) != null) {
            return true;
        }
        return FabricLoader.getInstance().getAllMods().stream().anyMatch(mod -> mod.findPath(resourcePath).isPresent());
    }

    private record PlaceholderBlockUnbakedModel(ResourceLocation modelId) implements UnbakedModel {
        @Override
        public Collection<ResourceLocation> getDependencies() {
            return List.of(this.modelId);
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
            UnbakedModel model = resolver.apply(this.modelId);
            if (model != null) {
                model.resolveParents(resolver);
            }
        }

        @Override
        public BakedModel bake(ModelBaker baker,
                               Function<Material, TextureAtlasSprite> textureGetter,
                               ModelState modelState) {
            return baker.bake(this.modelId, modelState);
        }
    }

    private record GeneratedSlabBlockUnbakedModel(ResourceLocation sourceModelId,
                                                  ModelType modelType,
                                                  ResourceLocation fallbackModelId) implements UnbakedModel {
        @Override
        public Collection<ResourceLocation> getDependencies() {
            return List.of(this.sourceModelId, this.fallbackModelId);
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
            UnbakedModel sourceModel = resolver.apply(this.sourceModelId);
            if (sourceModel != null) {
                sourceModel.resolveParents(resolver);
            }
            UnbakedModel fallbackModel = resolver.apply(this.fallbackModelId);
            if (fallbackModel != null) {
                fallbackModel.resolveParents(resolver);
            }
        }

        @Override
        public BakedModel bake(ModelBaker baker,
                               Function<Material, TextureAtlasSprite> textureGetter,
                               ModelState modelState) {
            BakedModel sourceModel = baker.bake(this.sourceModelId, modelState);
            if (sourceModel != null) {
                return EnhancedSlabBlockBakedModel.wrap(sourceModel, this.modelType);
            }
            return baker.bake(this.fallbackModelId, modelState);
        }
    }

    private record QuadContainerBlockUnbakedModel(ResourceLocation fallbackModelId, boolean vertical) implements UnbakedModel {
        @Override
        public Collection<ResourceLocation> getDependencies() {
            return List.of(this.fallbackModelId);
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
            UnbakedModel fallbackModel = resolver.apply(this.fallbackModelId);
            if (fallbackModel != null) {
                fallbackModel.resolveParents(resolver);
            }
        }

        @Override
        public BakedModel bake(ModelBaker baker,
                               Function<Material, TextureAtlasSprite> textureGetter,
                               ModelState modelState) {
            return FabricEnhancedSlabQuadBlockBakedModel.wrap(baker.bake(this.fallbackModelId, modelState), this.vertical);
        }
    }

    private record StorageContainerBlockUnbakedModel(ResourceLocation fallbackModelId, ModelType modelType) implements UnbakedModel {
        @Override
        public Collection<ResourceLocation> getDependencies() {
            return List.of(this.fallbackModelId);
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
            UnbakedModel fallbackModel = resolver.apply(this.fallbackModelId);
            if (fallbackModel != null) {
                fallbackModel.resolveParents(resolver);
            }
        }

        @Override
        public BakedModel bake(ModelBaker baker,
                               Function<Material, TextureAtlasSprite> textureGetter,
                               ModelState modelState) {
            return FabricEnhancedSlabStorageBlockBakedModel.wrap(baker.bake(this.fallbackModelId, modelState), this.modelType);
        }
    }
}
