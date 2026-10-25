package org.onenonly.bitsandbalance.common.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport;
import org.onenonly.bitsandbalance.common.recipe.SlabFamilyBlockLookup;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SlabFamilyModelSourceSelector {
    private static final Map<Identifier, Boolean> CUSTOM_SLAB_MODEL_CACHE = new HashMap<>();
    private static Object cachedResourceManager;

    private SlabFamilyModelSourceSelector() {
    }

    public static @Nullable BlockState verticalPrismRenderState(@Nullable BlockState sourceState) {
        if (sourceState == null) {
            return null;
        }

        Block sourceBlock = verticalPrismSourceBlock(sourceState.getBlock());
        return sourceBlock == sourceState.getBlock() ? sourceState : sourceBlock.defaultBlockState();
    }

    public static @Nullable BlockState fullBlockRenderStateForCompleteFamily(@Nullable BlockState first,
                                                                              @Nullable BlockState second) {
        Block sourceSlab = sourceSlabBlock(first);
        return sourceSlab != null && sourceSlab == sourceSlabBlock(second)
                ? fullBlockRenderState(sourceSlab)
                : null;
    }

    public static @Nullable BlockState fullBlockRenderStateForCompleteFamily(@Nullable BlockState first,
                                                                              @Nullable BlockState second,
                                                                              @Nullable BlockState third,
                                                                              @Nullable BlockState fourth) {
        Block sourceSlab = sourceSlabBlock(first);
        return sourceSlab != null
                && sourceSlab == sourceSlabBlock(second)
                && sourceSlab == sourceSlabBlock(third)
                && sourceSlab == sourceSlabBlock(fourth)
                ? fullBlockRenderState(sourceSlab)
                : null;
    }

    public static @Nullable BlockState fullBlockRenderStateForCompleteFamily(@Nullable BlockState... sourceStates) {
        if (sourceStates == null || sourceStates.length == 0) {
            return null;
        }

        Block sourceSlab = null;
        for (BlockState sourceState : sourceStates) {
            Block nextSourceSlab = sourceSlabBlock(sourceState);
            if (nextSourceSlab == null) {
                return null;
            }
            if (sourceSlab == null) {
                sourceSlab = nextSourceSlab;
            } else if (sourceSlab != nextSourceSlab) {
                return null;
            }
        }

        return fullBlockRenderState(sourceSlab);
    }

    private static @Nullable BlockState fullBlockRenderState(Block sourceSlab) {
        Block baseBlock = SlabFamilyBlockLookup.getBaseBlockForSlab(sourceSlab);
        if (baseBlock == null || baseBlock.asItem() == Items.AIR) {
            return null;
        }
        return baseBlock.defaultBlockState();
    }

    public static @Nullable BlockState thatchFullBlockRenderStateForCompleteFamily(@Nullable BlockState... sourceStates) {
        if (sourceStates == null || sourceStates.length == 0) {
            return null;
        }
        for (BlockState state : sourceStates) {
            if (!isUpgradeAquaticThatchSlab(sourceSlabBlock(state))) {
                return null;
            }
        }
        return fullBlockRenderStateForCompleteFamily(sourceStates);
    }

    public static @Nullable BlockState thatchFullBlockRenderStateForCompleteFamily(@Nullable BlockState first,
                                                                                    @Nullable BlockState second) {
        return isUpgradeAquaticThatchSlab(sourceSlabBlock(first))
                && isUpgradeAquaticThatchSlab(sourceSlabBlock(second))
                ? fullBlockRenderStateForCompleteFamily(first, second)
                : null;
    }

    public static @Nullable BlockState thatchFullBlockRenderStateForCompleteFamily(@Nullable BlockState first,
                                                                                    @Nullable BlockState second,
                                                                                    @Nullable BlockState third,
                                                                                    @Nullable BlockState fourth) {
        return isUpgradeAquaticThatchSlab(sourceSlabBlock(first))
                && isUpgradeAquaticThatchSlab(sourceSlabBlock(second))
                && isUpgradeAquaticThatchSlab(sourceSlabBlock(third))
                && isUpgradeAquaticThatchSlab(sourceSlabBlock(fourth))
                ? fullBlockRenderStateForCompleteFamily(first, second, third, fourth)
                : null;
    }

    public static @Nullable Block modelSourceBlockForItem(Item item) {
        if (!(item instanceof BlockItem blockItem)) return null;
        return modelSourceBlockForBlock(blockItem.getBlock());
    }

    public static @Nullable Block modelSourceBlockForBlock(Block block) {
        Block sourceSlab = EnhancedSlabItemSupport.sourceSlabForBlock(block);
        if (sourceSlab == null) return null;

        EnhancedSlabItemSupport.ModelType modelType = EnhancedSlabItemSupport.modelTypeForBlock(block);
        if (modelType == EnhancedSlabItemSupport.ModelType.VERTICAL_SLAB || modelType == EnhancedSlabItemSupport.ModelType.VERTICAL_STEP) {
            if (isUpgradeAquaticThatchSlab(sourceSlab)) {
                Block baseBlock = SlabFamilyBlockLookup.getBaseBlockForSlab(sourceSlab);
                if (baseBlock != null && baseBlock.asItem() != Items.AIR) {
                    return baseBlock;
                }
            }
            return verticalPrismSourceBlock(sourceSlab);
        }

        return sourceSlab;
    }

    private static @Nullable Block sourceSlabBlock(@Nullable BlockState sourceState) {
        if (sourceState == null) {
            return null;
        }

        Block block = sourceState.getBlock();
        Block sourceSlab = EnhancedSlabItemSupport.sourceSlabForBlock(block);
        if (sourceSlab != null) {
            return sourceSlab;
        }
        return SlabFamilyBlockLookup.getBaseBlockForSlab(block) == null ? null : block;
    }

    private static Block verticalPrismSourceBlock(Block sourceSlab) {
        Block baseBlock = SlabFamilyBlockLookup.getBaseBlockForSlab(sourceSlab);
        if (baseBlock == null || baseBlock.asItem() == Items.AIR) {
            return sourceSlab;
        }
        return shouldPreserveSourceSlabModel(sourceSlab) ? sourceSlab : baseBlock;
    }

    /** Only this verified model family uses the custom-quad rendering path. */
    public static boolean isUpgradeAquaticThatchSlab(Block sourceSlab) {
        return sourceSlab != null && Identifier.fromNamespaceAndPath("upgrade_aquatic", "beachgrass_thatch_slab")
                .equals(BuiltInRegistries.BLOCK.getKey(sourceSlab));
    }

    public static @Nullable BlockState thatchGeometrySourceState(@Nullable BlockState sourceSlabState,
                                                                  EnhancedSlabItemSupport.ModelType modelType) {
        if (sourceSlabState == null || modelType == null
                || modelType == EnhancedSlabItemSupport.ModelType.NONE
                || modelType == EnhancedSlabItemSupport.ModelType.STEP
                || !isUpgradeAquaticThatchSlab(sourceSlabBlock(sourceSlabState))) {
            return sourceSlabState;
        }
        BlockState fullState = thatchFullBlockRenderStateForCompleteFamily(sourceSlabState);
        return fullState == null ? sourceSlabState : fullState;
    }

    private static boolean shouldPreserveSourceSlabModel(Block sourceSlab) {
        try {
            Object resourceManager = currentResourceManager();
            synchronized (CUSTOM_SLAB_MODEL_CACHE) {
                if (resourceManager != cachedResourceManager) {
                    CUSTOM_SLAB_MODEL_CACHE.clear();
                    cachedResourceManager = resourceManager;
                }

                Identifier id = BuiltInRegistries.BLOCK.getKey(sourceSlab);
                if (id == null) return false;

                Boolean cached = CUSTOM_SLAB_MODEL_CACHE.get(id);
                if (cached != null) {
                    return cached;
                }

                boolean custom = hasCustomSlabModel(id);
                CUSTOM_SLAB_MODEL_CACHE.put(id, custom);
                return custom;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasCustomSlabModel(Identifier blockId) {
        Identifier modelId = modelIdFromBlockState(blockId);
        if (modelId == null) {
            modelId = Identifier.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath());
        }
        return hasCustomSlabModel(modelId, new HashSet<>(), blockId.getNamespace());
    }

    private static boolean hasCustomSlabModel(Identifier modelId, Set<Identifier> visited, String defaultNamespace) {
        if (!visited.add(modelId)) {
            return false;
        }

        JsonObject model = readJsonResource(Identifier.fromNamespaceAndPath(modelId.getNamespace(), "models/" + modelId.getPath() + ".json"));
        if (model == null) {
            return false;
        }

        JsonArray elements = model.getAsJsonArray("elements");
        if (elements != null) {
            return !isStandardBottomSlabElements(elements);
        }

        JsonElement parent = model.get("parent");
        if (parent == null || !parent.isJsonPrimitive() || !parent.getAsJsonPrimitive().isString()) {
            return false;
        }

        String parentId = parent.getAsString();
        if (isStandardSlabParent(parentId)) {
            return false;
        }

        Identifier parsedParent = parseModelId(parentId, defaultNamespace);
        return parsedParent != null && hasCustomSlabModel(parsedParent, visited, parsedParent.getNamespace());
    }

    private static boolean isStandardBottomSlabElements(JsonArray elements) {
        if (elements.size() != 1 || !elements.get(0).isJsonObject()) {
            return false;
        }

        JsonObject element = elements.get(0).getAsJsonObject();
        float[] from = vector(element.getAsJsonArray("from"));
        float[] to = vector(element.getAsJsonArray("to"));
        return from != null
                && to != null
                && close(from[0], 0.0F)
                && close(from[1], 0.0F)
                && close(from[2], 0.0F)
                && close(to[0], 16.0F)
                && close(to[1], 8.0F)
                && close(to[2], 16.0F);
    }

    private static @Nullable Identifier modelIdFromBlockState(Identifier blockId) {
        JsonObject blockState = readJsonResource(Identifier.fromNamespaceAndPath(blockId.getNamespace(), "blockstates/" + blockId.getPath() + ".json"));
        if (blockState == null) {
            return null;
        }

        String preferred = findBlockStateModel(blockState, true);
        if (preferred == null) {
            preferred = findBlockStateModel(blockState, false);
        }
        return preferred == null ? null : parseModelId(preferred, blockId.getNamespace());
    }

    private static @Nullable String findBlockStateModel(JsonElement element, boolean preferBottomSlab) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (preferBottomSlab && object.has("variants")) {
                JsonObject variants = object.getAsJsonObject("variants");
                for (Map.Entry<String, JsonElement> entry : variants.entrySet()) {
                    if (entry.getKey().contains("type=bottom")) {
                        String model = findBlockStateModel(entry.getValue(), false);
                        if (model != null) {
                            return model;
                        }
                    }
                }
            }

            JsonElement model = object.get("model");
            if (model != null && model.isJsonPrimitive() && model.getAsJsonPrimitive().isString()) {
                return model.getAsString();
            }

            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String found = findBlockStateModel(entry.getValue(), false);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                String found = findBlockStateModel(child, false);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private static boolean isStandardSlabParent(String parentId) {
        return "minecraft:block/slab".equals(parentId)
                || "block/slab".equals(parentId)
                || "minecraft:block/slab_top".equals(parentId)
                || "block/slab_top".equals(parentId);
    }

    private static @Nullable Identifier parseModelId(String id, String defaultNamespace) {
        try {
            if (id.indexOf(':') >= 0) {
                return Identifier.tryParse(id);
            }
            return Identifier.fromNamespaceAndPath(defaultNamespace, id);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable float[] vector(@Nullable JsonArray array) {
        if (array == null || array.size() != 3) {
            return null;
        }
        try {
            return new float[]{
                    array.get(0).getAsFloat(),
                    array.get(1).getAsFloat(),
                    array.get(2).getAsFloat()
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean close(float a, float b) {
        return Math.abs(a - b) < 0.001F;
    }

    private static @Nullable JsonObject readJsonResource(Identifier resourceId) {
        try (InputStream input = openResource(resourceId)) {
            if (input == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable InputStream openResource(Identifier resourceId) throws IOException {
        InputStream fromManager = openMinecraftResource(resourceId);
        if (fromManager != null) {
            return fromManager;
        }

        String classpathPath = "assets/" + resourceId.getNamespace() + "/" + resourceId.getPath();
        InputStream fromClasspath = SlabFamilyModelSourceSelector.class.getClassLoader().getResourceAsStream(classpathPath);
        if (fromClasspath != null) {
            return fromClasspath;
        }

        return openFabricModResource(classpathPath);
    }

    private static @Nullable InputStream openMinecraftResource(Identifier resourceId) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getResourceManager() == null) {
                return null;
            }

            Optional<?> resource = minecraft.getResourceManager().getResource(resourceId);
            if (resource.isEmpty()) {
                return null;
            }

            Object value = resource.get();
            Method open = value.getClass().getMethod("open");
            return (InputStream) open.invoke(value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable InputStream openFabricModResource(String classpathPath) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            Iterable<?> mods = (Iterable<?>) loaderClass.getMethod("getAllMods").invoke(loader);
            for (Object mod : mods) {
                Optional<?> path = (Optional<?>) mod.getClass().getMethod("findPath", String.class).invoke(mod, classpathPath);
                if (path.isEmpty()) {
                    continue;
                }
                Path resolved = (Path) path.get();
                if (Files.isRegularFile(resolved)) {
                    return Files.newInputStream(resolved);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static @Nullable Object currentResourceManager() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft == null ? null : minecraft.getResourceManager();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
