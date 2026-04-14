package org.onenonly.bitsandbalance.fabric.worldgen;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.fabric.config.FabricWorldgenConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fabric equivalent of NeoForge's OreVariantReplacer.
 *
 * Replaces vanilla ore blocks with BitsAndBalance variant ores when touching variant stones.
 */
public final class FabricOreVariantReplacer {
    private static final int CHUNK_SIZE = 16;
    private static final int CHUNK_AREA = CHUNK_SIZE * CHUNK_SIZE;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Block[] STONE_TYPES = {Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.TUFF};

    private static final Set<Block> ORE_BLOCKS = new HashSet<>();
    private static final Map<Block, Map<Block, Block>> ORE_VARIANT_MAP = new HashMap<>();

    private static volatile boolean initialized = false;

    private FabricOreVariantReplacer() {
    }

    public static void init() {
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newChunk) -> onChunkLoad(level, chunk));
    }

    private static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        initializeOreVariantMap();

        if (!FabricWorldgenConfig.isOreVariantsEnabled() || !BitsAndBalanceCommon.isOreVariantsEnabled()) {
            return;
        }

        processChunk(level, chunk);
    }

    private static void initializeOreVariantMap() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Coal ore variants
        Map<Block, Block> coalVariants = new HashMap<>();
        coalVariants.put(Blocks.ANDESITE, variant("andesite_coal_ore"));
        coalVariants.put(Blocks.DIORITE, variant("diorite_coal_ore"));
        coalVariants.put(Blocks.GRANITE, variant("granite_coal_ore"));
        coalVariants.put(Blocks.TUFF, variant("tuff_coal_ore"));
        ORE_VARIANT_MAP.put(Blocks.COAL_ORE, coalVariants);
        ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_COAL_ORE, coalVariants);

        // Iron ore variants
        Map<Block, Block> ironVariants = new HashMap<>();
        ironVariants.put(Blocks.ANDESITE, variant("andesite_iron_ore"));
        ironVariants.put(Blocks.DIORITE, variant("diorite_iron_ore"));
        ironVariants.put(Blocks.GRANITE, variant("granite_iron_ore"));
        ironVariants.put(Blocks.TUFF, variant("tuff_iron_ore"));
        ORE_VARIANT_MAP.put(Blocks.IRON_ORE, ironVariants);
        ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_IRON_ORE, ironVariants);

        // Copper ore variants
        Map<Block, Block> copperVariants = new HashMap<>();
        copperVariants.put(Blocks.ANDESITE, variant("andesite_copper_ore"));
        copperVariants.put(Blocks.DIORITE, variant("diorite_copper_ore"));
        copperVariants.put(Blocks.GRANITE, variant("granite_copper_ore"));
        copperVariants.put(Blocks.TUFF, variant("tuff_copper_ore"));
        ORE_VARIANT_MAP.put(Blocks.COPPER_ORE, copperVariants);
        ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_COPPER_ORE, copperVariants);

        // Gold ore variants
        Map<Block, Block> goldVariants = new HashMap<>();
        goldVariants.put(Blocks.ANDESITE, variant("andesite_gold_ore"));
        goldVariants.put(Blocks.DIORITE, variant("diorite_gold_ore"));
        goldVariants.put(Blocks.GRANITE, variant("granite_gold_ore"));
        goldVariants.put(Blocks.TUFF, variant("tuff_gold_ore"));
        ORE_VARIANT_MAP.put(Blocks.GOLD_ORE, goldVariants);
        ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_GOLD_ORE, goldVariants);

        // Diamond ore variants
        Map<Block, Block> diamondVariants = new HashMap<>();
        diamondVariants.put(Blocks.ANDESITE, variant("andesite_diamond_ore"));
        diamondVariants.put(Blocks.DIORITE, variant("diorite_diamond_ore"));
        diamondVariants.put(Blocks.GRANITE, variant("granite_diamond_ore"));
        diamondVariants.put(Blocks.TUFF, variant("tuff_diamond_ore"));
        ORE_VARIANT_MAP.put(Blocks.DIAMOND_ORE, diamondVariants);
        ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_DIAMOND_ORE, diamondVariants);

        // Emerald ore variants
        Map<Block, Block> emeraldVariants = new HashMap<>();
        emeraldVariants.put(Blocks.ANDESITE, variant("andesite_emerald_ore"));
        emeraldVariants.put(Blocks.DIORITE, variant("diorite_emerald_ore"));
        emeraldVariants.put(Blocks.GRANITE, variant("granite_emerald_ore"));
        emeraldVariants.put(Blocks.TUFF, variant("tuff_emerald_ore"));
        ORE_VARIANT_MAP.put(Blocks.EMERALD_ORE, emeraldVariants);
        ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_EMERALD_ORE, emeraldVariants);

        // Lapis ore variants
        Map<Block, Block> lapisVariants = new HashMap<>();
        lapisVariants.put(Blocks.ANDESITE, variant("andesite_lapis_ore"));
        lapisVariants.put(Blocks.DIORITE, variant("diorite_lapis_ore"));
        lapisVariants.put(Blocks.GRANITE, variant("granite_lapis_ore"));
        lapisVariants.put(Blocks.TUFF, variant("tuff_lapis_ore"));
        ORE_VARIANT_MAP.put(Blocks.LAPIS_ORE, lapisVariants);
        ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_LAPIS_ORE, lapisVariants);

        // Redstone ore variants
        Map<Block, Block> redstoneVariants = new HashMap<>();
        redstoneVariants.put(Blocks.ANDESITE, variant("andesite_redstone_ore"));
        redstoneVariants.put(Blocks.DIORITE, variant("diorite_redstone_ore"));
        redstoneVariants.put(Blocks.GRANITE, variant("granite_redstone_ore"));
        redstoneVariants.put(Blocks.TUFF, variant("tuff_redstone_ore"));
        ORE_VARIANT_MAP.put(Blocks.REDSTONE_ORE, redstoneVariants);
        ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_REDSTONE_ORE, redstoneVariants);

        // Create zinc ore variants (soft dependency)
        Block zincOre = BuiltInRegistries.BLOCK.getOptional(Identifier.parse("create:zinc_ore")).orElse(Blocks.AIR);
        Block deepslateZincOre = BuiltInRegistries.BLOCK.getOptional(Identifier.parse("create:deepslate_zinc_ore")).orElse(Blocks.AIR);
        if (zincOre != Blocks.AIR) {
            Map<Block, Block> zincVariants = new HashMap<>();
            zincVariants.put(Blocks.ANDESITE, variant("andesite_zinc_ore"));
            zincVariants.put(Blocks.DIORITE, variant("diorite_zinc_ore"));
            zincVariants.put(Blocks.GRANITE, variant("granite_zinc_ore"));
            zincVariants.put(Blocks.TUFF, variant("tuff_zinc_ore"));
            ORE_VARIANT_MAP.put(zincOre, zincVariants);
            if (deepslateZincOre != Blocks.AIR) {
                ORE_VARIANT_MAP.put(deepslateZincOre, zincVariants);
            }
        }

        // Cache ore blocks for fast lookup
        ORE_BLOCKS.addAll(ORE_VARIANT_MAP.keySet());
    }

    private static Block variant(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, path);
        return BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.AIR);
    }

    private static void processChunk(ServerLevel level, LevelChunk chunk) {
        int minY = Math.max(level.getMinY(), -64);
        int maxY = computeMaxYExclusive(level, chunk);

        if (BitsAndBalanceCommon.isParallelWorldgenProcessingEnabled()) {
            processChunkParallel(level, chunk, minY, maxY);
            return;
        }

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();

        for (int y = minY; y < maxY; y++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    mutablePos.set(chunkX + x, y, chunkZ + z);
                    BlockState state = chunk.getBlockState(mutablePos);
                    Block block = state.getBlock();
                    if (ORE_BLOCKS.contains(block)) {
                        tryReplaceOre(level, chunk, mutablePos, block);
                    }
                }
            }
        }
    }

    private static void processChunkParallel(ServerLevel level, LevelChunk chunk, int minY, int maxY) {
        int baseHeight = maxY - minY;
        if (baseHeight <= 0) {
            return;
        }

        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int snapshotMinY = Math.max(level.getMinY(), minY - 1);
        int snapshotMaxY = Math.min(level.getMaxY(), maxY + 1);
        int snapshotHeight = snapshotMaxY - snapshotMinY;

        BlockState[] snapshot = new BlockState[snapshotHeight * CHUNK_AREA];
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int y = snapshotMinY; y < snapshotMaxY; y++) {
            int yOffset = y - snapshotMinY;
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    mutablePos.set(chunkX + x, y, chunkZ + z);
                    snapshot[snapshotIndex(x, yOffset, z)] = chunk.getBlockState(mutablePos);
                }
            }
        }

        BlockState[] replacements = new BlockState[baseHeight * CHUNK_AREA];
        BitsAndBalanceCommon.runParallelWorldgenRange(replacements.length, index -> {
            int yOffset = index / CHUNK_AREA;
            int layerIndex = index % CHUNK_AREA;
            int x = layerIndex / CHUNK_SIZE;
            int z = layerIndex % CHUNK_SIZE;
            int y = minY + yOffset;

            BlockState originalState = snapshot[snapshotIndex(x, y - snapshotMinY, z)];
            Block oreBlock = originalState.getBlock();
            if (!ORE_BLOCKS.contains(oreBlock)) {
                return;
            }

            replacements[index] = computeReplacementFromSnapshot(snapshot, snapshotMinY, snapshotMaxY, x, y, z, oreBlock, originalState);
        });

        for (int index = 0; index < replacements.length; index++) {
            BlockState replacement = replacements[index];
            if (replacement == null) {
                continue;
            }

            int yOffset = index / CHUNK_AREA;
            int layerIndex = index % CHUNK_AREA;
            int x = layerIndex / CHUNK_SIZE;
            int z = layerIndex % CHUNK_SIZE;
            mutablePos.set(chunkX + x, minY + yOffset, chunkZ + z);
            chunk.setBlockState(mutablePos, replacement, 0);
        }
    }

    /**
     * Determine how high we should scan for ores in this chunk.
     *
     * Vanilla-ish worlds have most ores below ~Y=80, but worldgen mods (e.g. Terralith)
     * can push terrain and exposed ores far higher. We use the chunk's surface heightmap
     * to avoid scanning empty sky up to world max while still covering high terrain.
     */
    private static int computeMaxYExclusive(ServerLevel level, LevelChunk chunk) {
        int maxSurfaceY = level.getMinY();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (surfaceY > maxSurfaceY) {
                    maxSurfaceY = surfaceY;
                }
            }
        }

        // Small buffer helps cover cliff faces / overhangs without scanning full height.
        int desiredMaxY = Math.max(80, maxSurfaceY + 8);
        return Math.min(level.getMaxY(), desiredMaxY);
    }

    private static void tryReplaceOre(Level level, LevelChunk chunk, BlockPos.MutableBlockPos pos, Block oreBlock) {
        Map<Block, Block> variants = ORE_VARIANT_MAP.get(oreBlock);
        if (variants == null) {
            return;
        }

        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunk.getPos().getMaxBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunk.getPos().getMaxBlockZ();

        int[] stoneCounts = new int[4];
        Block[] stoneTypes = STONE_TYPES;

        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();

        for (Direction direction : DIRECTIONS) {
            int neighborX = posX + direction.getStepX();
            int neighborY = posY + direction.getStepY();
            int neighborZ = posZ + direction.getStepZ();

            if (neighborX < chunkMinX || neighborX > chunkMaxX || neighborZ < chunkMinZ || neighborZ > chunkMaxZ) {
                continue;
            }

            pos.set(neighborX, neighborY, neighborZ);
            Block neighborBlock = chunk.getBlockState(pos).getBlock();
            for (int i = 0; i < stoneTypes.length; i++) {
                if (neighborBlock == stoneTypes[i]) {
                    stoneCounts[i]++;
                    break;
                }
            }
        }

        pos.set(posX, posY, posZ);

        int maxCount = 0;
        int dominantIndex = -1;
        for (int i = 0; i < stoneCounts.length; i++) {
            if (stoneCounts[i] > maxCount) {
                maxCount = stoneCounts[i];
                dominantIndex = i;
            }
        }

        if (dominantIndex == -1) {
            return;
        }

        Block dominantStone = stoneTypes[dominantIndex];
        Block variantOre = variants.get(dominantStone);
        if (variantOre == null || variantOre == Blocks.AIR) {
            return;
        }

        BlockState originalState = chunk.getBlockState(pos);
        BlockState newState = variantOre.defaultBlockState();

        // Preserve redstone "lit" state when swapping (vanilla redstone ore can be lit).
        if (originalState.hasProperty(net.minecraft.world.level.block.RedStoneOreBlock.LIT)
                && newState.hasProperty(org.onenonly.bitsandbalance.blocks.CustomRedstoneOreBlock.LIT)) {
            boolean lit = originalState.getValue(net.minecraft.world.level.block.RedStoneOreBlock.LIT);
            newState = newState.setValue(org.onenonly.bitsandbalance.blocks.CustomRedstoneOreBlock.LIT, lit);
        }

        // Keep the chunk-local write semantics, like NeoForge.
        chunk.setBlockState(pos, newState, 0);
    }

    private static BlockState computeReplacementFromSnapshot(
            BlockState[] snapshot,
            int snapshotMinY,
            int snapshotMaxY,
            int x,
            int y,
            int z,
            Block oreBlock,
            BlockState originalState
    ) {
        Map<Block, Block> variants = ORE_VARIANT_MAP.get(oreBlock);
        if (variants == null) {
            return null;
        }

        int[] stoneCounts = new int[STONE_TYPES.length];
        for (Direction direction : DIRECTIONS) {
            int neighborX = x + direction.getStepX();
            int neighborY = y + direction.getStepY();
            int neighborZ = z + direction.getStepZ();

            if (neighborX < 0 || neighborX >= CHUNK_SIZE || neighborZ < 0 || neighborZ >= CHUNK_SIZE) {
                continue;
            }
            if (neighborY < snapshotMinY || neighborY >= snapshotMaxY) {
                continue;
            }

            Block neighborBlock = snapshot[snapshotIndex(neighborX, neighborY - snapshotMinY, neighborZ)].getBlock();
            for (int i = 0; i < STONE_TYPES.length; i++) {
                if (neighborBlock == STONE_TYPES[i]) {
                    stoneCounts[i]++;
                    break;
                }
            }
        }

        int maxCount = 0;
        int dominantIndex = -1;
        for (int i = 0; i < stoneCounts.length; i++) {
            if (stoneCounts[i] > maxCount) {
                maxCount = stoneCounts[i];
                dominantIndex = i;
            }
        }

        if (dominantIndex == -1) {
            return null;
        }

        Block variantOre = variants.get(STONE_TYPES[dominantIndex]);
        if (variantOre == null || variantOre == Blocks.AIR) {
            return null;
        }

        BlockState newState = variantOre.defaultBlockState();
        if (originalState.hasProperty(net.minecraft.world.level.block.RedStoneOreBlock.LIT)
                && newState.hasProperty(org.onenonly.bitsandbalance.blocks.CustomRedstoneOreBlock.LIT)) {
            boolean lit = originalState.getValue(net.minecraft.world.level.block.RedStoneOreBlock.LIT);
            newState = newState.setValue(org.onenonly.bitsandbalance.blocks.CustomRedstoneOreBlock.LIT, lit);
        }

        return newState;
    }

    private static int snapshotIndex(int x, int yOffset, int z) {
        return yOffset * CHUNK_AREA + x * CHUNK_SIZE + z;
    }
}
