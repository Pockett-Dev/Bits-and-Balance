package org.onenonly.bitsandbalance.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport.ModelType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class NeoForgeGeneratedClientResources {
    private static final String PACK_NAME = "bitsandbalance_enhanced_slab_client_models";
    private static final String EMPTY_MODEL = "bitsandbalance:block/empty_dynamic_enhanced_slab";
    private static final Path PACK_ROOT = FMLPaths.GAMEDIR.get()
            .resolve(".runtime-packs")
            .resolve("client-resources")
            .resolve(PACK_NAME);
    private static final Path LEGACY_PACK_ROOT = FMLPaths.CONFIGDIR.get()
            .resolve("Bits and Balance")
            .resolve("runtime-packs")
            .resolve("client-resources")
            .resolve(PACK_NAME);
    private static final String PACK_MCMETA = """
            {
              \"pack\": {
                \"description\": \"Bits and Balance: Generated Enhanced Slab Client Models\",
                \"pack_format\": 34,
                \"supported_formats\": [34, 34]
              }
            }
            """;

    private NeoForgeGeneratedClientResources() {
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }

        syncPack();
        event.addRepositorySource(createSource());
    }

    private static RepositorySource createSource() {
        return acceptor -> {
            Pack pack = Pack.readMetaAndCreate(
                    new PackLocationInfo(
                            "bitsandbalance/generated/" + PACK_NAME,
                            Component.literal("Bits and Balance: Generated Enhanced Slab Client Models"),
                            PackSource.BUILT_IN,
                            Optional.empty()
                    ),
                    new PathPackResources.PathResourcesSupplier(PACK_ROOT),
                    PackType.CLIENT_RESOURCES,
                    new PackSelectionConfig(true, Pack.Position.TOP, false)
            );
            if (pack != null) {
                acceptor.accept(pack);
            }
        };
    }

    private static void syncPack() {
        try {
            resetDirectory(PACK_ROOT);
            write(PACK_ROOT.resolve("pack.mcmeta"), PACK_MCMETA);
            write(PACK_ROOT.resolve("assets/bitsandbalance/models/block/empty_dynamic_enhanced_slab.json"), """
                    {
                      \"textures\": {},
                      \"elements\": []
                    }
                    """);

            int blockstates = 0;
            for (Block block : BuiltInRegistries.BLOCK) {
                ModelType modelType = EnhancedSlabItemSupport.modelTypeForBlock(block);
                if (modelType == ModelType.NONE) {
                    continue;
                }

                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                if (blockId == null) {
                    continue;
                }

                write(blockstatePath(blockId), blockstateJson(block));
                blockstates++;
            }

            int itemModels = 0;
            for (Item item : BuiltInRegistries.ITEM) {
                ModelType modelType = EnhancedSlabItemSupport.modelTypeForItem(item);
                if (modelType == ModelType.NONE) {
                    continue;
                }

                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                if (itemId == null) {
                    continue;
                }

                ResourceLocation sourceItemId = sourceItemId(item);
                if (sourceItemId == null) {
                    continue;
                }

                write(itemModelPath(itemId), itemModelJson(sourceItemId));
                itemModels++;
            }

            BitsAndBalance.LOGGER.info("[{}] Generated {} dynamic slab-family blockstate stubs and {} item model stubs", BitsAndBalance.MODID, blockstates, itemModels);
            deleteLegacyPackRoot();
        } catch (IOException e) {
            BitsAndBalance.LOGGER.warn("[{}] Failed generating dynamic slab-family client resources: {}", BitsAndBalance.MODID, e.toString());
        } catch (UncheckedIOException e) {
            BitsAndBalance.LOGGER.warn("[{}] Failed cleaning dynamic slab-family client resources: {}", BitsAndBalance.MODID, e.getCause().toString());
        }
    }

    private static ResourceLocation sourceItemId(Item item) {
        Block sourceBlock = SlabFamilyModelSourceSelector.modelSourceBlockForItem(item);
        if (sourceBlock == null) {
            return null;
        }

        Item sourceItem = sourceBlock.asItem();
        if (sourceItem == null || sourceItem == Items.AIR) {
            return null;
        }

        ResourceLocation sourceItemId = BuiltInRegistries.ITEM.getKey(sourceItem);
        return sourceItemId == null ? null : sourceItemId;
    }

    private static Path blockstatePath(ResourceLocation id) {
        return PACK_ROOT.resolve("assets")
                .resolve(id.getNamespace())
                .resolve("blockstates")
                .resolve(id.getPath() + ".json");
    }

    private static Path itemModelPath(ResourceLocation id) {
        return PACK_ROOT.resolve("assets")
                .resolve(id.getNamespace())
                .resolve("models")
                .resolve("item")
                .resolve(id.getPath() + ".json");
    }

    private static String itemModelJson(ResourceLocation sourceItemId) {
        return """
                {
                  \"parent\": \"%s:item/%s\"
                }
                """.formatted(sourceItemId.getNamespace(), sourceItemId.getPath());
    }

    private static String blockstateJson(Block block) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"variants\": {\n");

        List<BlockState> states = block.getStateDefinition().getPossibleStates();
        for (int index = 0; index < states.size(); index++) {
            BlockState state = states.get(index);
            json.append("    \"")
                    .append(variantKey(state))
                    .append("\": { \"model\": \"")
                    .append(EMPTY_MODEL)
                    .append("\" }");
            if (index < states.size() - 1) {
                json.append(',');
            }
            json.append('\n');
        }

        json.append("  }\n}\n");
        return json.toString();
    }

    private static String variantKey(BlockState state) {
        List<Property<?>> properties = new ArrayList<>(state.getProperties());
        properties.sort(Comparator.comparing(Property::getName));

        StringBuilder key = new StringBuilder();
        for (int index = 0; index < properties.size(); index++) {
            Property<?> property = properties.get(index);
            if (index > 0) {
                key.append(',');
            }
            key.append(property.getName()).append('=').append(propertyValueName(state, property));
        }
        return key.toString();
    }

    private static <T extends Comparable<T>> String propertyValueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static void resetDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            deleteDirectoryContents(directory);
        }
        Files.createDirectories(directory);
    }

    private static void deleteLegacyPackRoot() throws IOException {
        if (!Files.exists(LEGACY_PACK_ROOT)) {
            return;
        }

        deleteRecursively(LEGACY_PACK_ROOT);
        deleteEmptyParent(LEGACY_PACK_ROOT.getParent());
        deleteEmptyParent(LEGACY_PACK_ROOT.getParent() == null ? null : LEGACY_PACK_ROOT.getParent().getParent());
    }

    private static void deleteDirectoryContents(Path directory) throws IOException {
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(directory))
                    .forEach(NeoForgeGeneratedClientResources::deleteQuietly);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(NeoForgeGeneratedClientResources::deleteQuietly);
        }
    }

    private static void deleteEmptyParent(Path directory) throws IOException {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }

        try (var entries = Files.list(directory)) {
            if (entries.findAny().isEmpty()) {
                Files.deleteIfExists(directory);
            }
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
