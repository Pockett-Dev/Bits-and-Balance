package org.onenonly.bitsandbalance.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.blocks.OreVariants;
import org.onenonly.bitsandbalance.common.compat.SableCompat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles replacing vanilla ores with stone variant ores during world generation.
 * Optimized to only check ores and their immediate surroundings.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class OreVariantReplacer {
    
    private static final Direction[] DIRECTIONS = Direction.values();
    
    // Cache ore blocks for fast lookup
    private static final java.util.Set<Block> ORE_BLOCKS = new java.util.HashSet<>();
    
    // Map vanilla ores to their variant replacements based on surrounding stone
    private static final Map<Block, Map<Block, Block>> ORE_VARIANT_MAP = new HashMap<>();
    private static final Object INIT_LOCK = new Object();
    private static final Object QUEUE_LOCK = new Object();
    private static final ArrayDeque<PendingChunk> PENDING_CHUNKS = new ArrayDeque<>();
    private static final Set<PendingChunkKey> PENDING_CHUNK_KEYS = new HashSet<>();
    private static final ArrayDeque<PendingChunkKey> UNPERSISTED_NEW_CHUNKS = new ArrayDeque<>();
    private static final Set<PendingChunkKey> UNPERSISTED_NEW_CHUNK_KEYS = new HashSet<>();
    private static final int MAX_CHUNKS_PER_TICK = 8;
    private static final int MAX_QUEUE_POLLS_PER_TICK = 64;
    private static final int MAX_UNAVAILABLE_RETRIES = 20;
    private static final long MAX_PROCESSING_NANOS_PER_TICK = 8_000_000L;

    private static volatile boolean initialized = false;
    
    /**
     * Initialize the ore variant mapping lazily (called on first use).
     */
    private static void initializeOreVariantMap() {
        if (initialized) {
            return; // Already initialized
        }

        synchronized (INIT_LOCK) {
            if (initialized) {
                return; // Already initialized
            }

            // Coal ore variants
            Map<Block, Block> coalVariants = new HashMap<>();
            coalVariants.put(Blocks.ANDESITE, OreVariants.ANDESITE_COAL_ORE.get());
            coalVariants.put(Blocks.DIORITE, OreVariants.DIORITE_COAL_ORE.get());
            coalVariants.put(Blocks.GRANITE, OreVariants.GRANITE_COAL_ORE.get());
            coalVariants.put(Blocks.TUFF, OreVariants.TUFF_COAL_ORE.get());
            ORE_VARIANT_MAP.put(Blocks.COAL_ORE, coalVariants);
            ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_COAL_ORE, coalVariants);

            // Iron ore variants
            Map<Block, Block> ironVariants = new HashMap<>();
            ironVariants.put(Blocks.ANDESITE, OreVariants.ANDESITE_IRON_ORE.get());
            ironVariants.put(Blocks.DIORITE, OreVariants.DIORITE_IRON_ORE.get());
            ironVariants.put(Blocks.GRANITE, OreVariants.GRANITE_IRON_ORE.get());
            ironVariants.put(Blocks.TUFF, OreVariants.TUFF_IRON_ORE.get());
            ORE_VARIANT_MAP.put(Blocks.IRON_ORE, ironVariants);
            ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_IRON_ORE, ironVariants);

            // Copper ore variants
            Map<Block, Block> copperVariants = new HashMap<>();
            copperVariants.put(Blocks.ANDESITE, OreVariants.ANDESITE_COPPER_ORE.get());
            copperVariants.put(Blocks.DIORITE, OreVariants.DIORITE_COPPER_ORE.get());
            copperVariants.put(Blocks.GRANITE, OreVariants.GRANITE_COPPER_ORE.get());
            copperVariants.put(Blocks.TUFF, OreVariants.TUFF_COPPER_ORE.get());
            ORE_VARIANT_MAP.put(Blocks.COPPER_ORE, copperVariants);
            ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_COPPER_ORE, copperVariants);

            // Gold ore variants
            Map<Block, Block> goldVariants = new HashMap<>();
            goldVariants.put(Blocks.ANDESITE, OreVariants.ANDESITE_GOLD_ORE.get());
            goldVariants.put(Blocks.DIORITE, OreVariants.DIORITE_GOLD_ORE.get());
            goldVariants.put(Blocks.GRANITE, OreVariants.GRANITE_GOLD_ORE.get());
            goldVariants.put(Blocks.TUFF, OreVariants.TUFF_GOLD_ORE.get());
            ORE_VARIANT_MAP.put(Blocks.GOLD_ORE, goldVariants);
            ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_GOLD_ORE, goldVariants);

            // Diamond ore variants
            Map<Block, Block> diamondVariants = new HashMap<>();
            diamondVariants.put(Blocks.ANDESITE, OreVariants.ANDESITE_DIAMOND_ORE.get());
            diamondVariants.put(Blocks.DIORITE, OreVariants.DIORITE_DIAMOND_ORE.get());
            diamondVariants.put(Blocks.GRANITE, OreVariants.GRANITE_DIAMOND_ORE.get());
            diamondVariants.put(Blocks.TUFF, OreVariants.TUFF_DIAMOND_ORE.get());
            ORE_VARIANT_MAP.put(Blocks.DIAMOND_ORE, diamondVariants);
            ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_DIAMOND_ORE, diamondVariants);

            // Emerald ore variants
            Map<Block, Block> emeraldVariants = new HashMap<>();
            emeraldVariants.put(Blocks.ANDESITE, OreVariants.ANDESITE_EMERALD_ORE.get());
            emeraldVariants.put(Blocks.DIORITE, OreVariants.DIORITE_EMERALD_ORE.get());
            emeraldVariants.put(Blocks.GRANITE, OreVariants.GRANITE_EMERALD_ORE.get());
            emeraldVariants.put(Blocks.TUFF, OreVariants.TUFF_EMERALD_ORE.get());
            ORE_VARIANT_MAP.put(Blocks.EMERALD_ORE, emeraldVariants);
            ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_EMERALD_ORE, emeraldVariants);

            // Lapis ore variants
            Map<Block, Block> lapisVariants = new HashMap<>();
            lapisVariants.put(Blocks.ANDESITE, OreVariants.ANDESITE_LAPIS_ORE.get());
            lapisVariants.put(Blocks.DIORITE, OreVariants.DIORITE_LAPIS_ORE.get());
            lapisVariants.put(Blocks.GRANITE, OreVariants.GRANITE_LAPIS_ORE.get());
            lapisVariants.put(Blocks.TUFF, OreVariants.TUFF_LAPIS_ORE.get());
            ORE_VARIANT_MAP.put(Blocks.LAPIS_ORE, lapisVariants);
            ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_LAPIS_ORE, lapisVariants);

            // Redstone ore variants
            Map<Block, Block> redstoneVariants = new HashMap<>();
            redstoneVariants.put(Blocks.ANDESITE, OreVariants.ANDESITE_REDSTONE_ORE.get());
            redstoneVariants.put(Blocks.DIORITE, OreVariants.DIORITE_REDSTONE_ORE.get());
            redstoneVariants.put(Blocks.GRANITE, OreVariants.GRANITE_REDSTONE_ORE.get());
            redstoneVariants.put(Blocks.TUFF, OreVariants.TUFF_REDSTONE_ORE.get());
            ORE_VARIANT_MAP.put(Blocks.REDSTONE_ORE, redstoneVariants);
            ORE_VARIANT_MAP.put(Blocks.DEEPSLATE_REDSTONE_ORE, redstoneVariants);

            // Zinc ore variants (Create Mod - soft dependency via ResourceLocation)
            try {
                Block zincOre = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse("create:zinc_ore")).orElse(Blocks.AIR);
                Block deepslateZincOre = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse("create:deepslate_zinc_ore")).orElse(Blocks.AIR);

                if (zincOre != Blocks.AIR) {
                    Map<Block, Block> zincVariants = new HashMap<>();
                    zincVariants.put(Blocks.ANDESITE, OreVariants.ANDESITE_ZINC_ORE.get());
                    zincVariants.put(Blocks.DIORITE, OreVariants.DIORITE_ZINC_ORE.get());
                    zincVariants.put(Blocks.GRANITE, OreVariants.GRANITE_ZINC_ORE.get());
                    zincVariants.put(Blocks.TUFF, OreVariants.TUFF_ZINC_ORE.get());
                    ORE_VARIANT_MAP.put(zincOre, zincVariants);

                    if (deepslateZincOre != Blocks.AIR) {
                        ORE_VARIANT_MAP.put(deepslateZincOre, zincVariants);
                    }
                }
            } catch (Exception e) {
                // Create mod not loaded, skip zinc ore variants
            }

            // Build ore block cache for fast lookup
            ORE_BLOCKS.addAll(ORE_VARIANT_MAP.keySet());
            initialized = true;
        }
    }
    
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // Check if ore variants are enabled
        if (!org.onenonly.bitsandbalance.WorldConfig.ENABLE_ORE_VARIANTS.get()) {
            return;
        }
        
        // Only process server-side world generation
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        int chunkX = event.getChunk().getPos().getMinBlockX() >> 4;
        int chunkZ = event.getChunk().getPos().getMinBlockZ() >> 4;
        if (SableCompat.isInPlotGrid(serverLevel, chunkX, chunkZ)) {
            return;
        }

        // Initialize the ore variant map on first use (lazy initialization)
        initializeOreVariantMap();

        enqueueChunk(serverLevel.dimension(), chunkX, chunkZ, event.isNewChunk());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        persistNewChunkMarkers(event.getServer());
        drainChunkQueue(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        persistNewChunkMarkers(event.getServer());
        synchronized (QUEUE_LOCK) {
            if (!PENDING_CHUNKS.isEmpty()) {
                BitsAndBalance.LOGGER.info(
                        "Preserving {} pending ore-variant chunks for processing when they next load",
                        PENDING_CHUNKS.size()
                );
            }
            PENDING_CHUNKS.clear();
            PENDING_CHUNK_KEYS.clear();
            UNPERSISTED_NEW_CHUNKS.clear();
            UNPERSISTED_NEW_CHUNK_KEYS.clear();
        }
    }

    private static void enqueueChunk(ResourceKey<Level> dimension, int chunkX, int chunkZ, boolean newChunk) {
        synchronized (QUEUE_LOCK) {
            PendingChunkKey key = new PendingChunkKey(dimension, chunkX, chunkZ);
            if (newChunk && UNPERSISTED_NEW_CHUNK_KEYS.add(key)) {
                UNPERSISTED_NEW_CHUNKS.addLast(key);
            }
            if (PENDING_CHUNK_KEYS.add(key)) {
                PENDING_CHUNKS.addLast(new PendingChunk(key, 0));
            }
        }
    }

    private static void persistNewChunkMarkers(MinecraftServer server) {
        if (server == null) {
            return;
        }

        List<PendingChunkKey> markers = new ArrayList<>();
        synchronized (QUEUE_LOCK) {
            PendingChunkKey marker;
            while ((marker = UNPERSISTED_NEW_CHUNKS.pollFirst()) != null) {
                UNPERSISTED_NEW_CHUNK_KEYS.remove(marker);
                markers.add(marker);
            }
        }

        for (PendingChunkKey marker : markers) {
            ServerLevel level = server.getLevel(marker.dimension());
            if (level != null) {
                OreVariantPendingSavedData.get(level).add(ChunkPos.asLong(marker.chunkX(), marker.chunkZ()));
            }
        }
    }

    private static void drainChunkQueue(MinecraftServer server) {
        if (server == null || !org.onenonly.bitsandbalance.WorldConfig.ENABLE_ORE_VARIANTS.get()) {
            return;
        }

        int processed = 0;
        int polled = 0;
        long startedAt = System.nanoTime();
        while (processed < MAX_CHUNKS_PER_TICK && polled < MAX_QUEUE_POLLS_PER_TICK) {
            PendingChunk pending = pollPendingChunk();
            if (pending == null) {
                return;
            }
            polled++;

            ServerLevel level = server.getLevel(pending.key().dimension());
            if (level == null) {
                completePendingChunk(pending.key());
                continue;
            }

            long chunkPos = ChunkPos.asLong(pending.key().chunkX(), pending.key().chunkZ());
            OreVariantPendingSavedData pendingData = OreVariantPendingSavedData.get(level);
            if (!pendingData.contains(chunkPos)) {
                completePendingChunk(pending.key());
                continue;
            }

            LevelChunk chunk = level.getChunkSource().getChunkNow(pending.key().chunkX(), pending.key().chunkZ());
            if (chunk == null) {
                retryUnavailableChunk(pending);
                continue;
            }

            processChunk(level, chunk);
            pendingData.remove(chunkPos);
            completePendingChunk(pending.key());
            processed++;
            if (System.nanoTime() - startedAt >= MAX_PROCESSING_NANOS_PER_TICK) {
                return;
            }
        }
    }

    private static PendingChunk pollPendingChunk() {
        synchronized (QUEUE_LOCK) {
            return PENDING_CHUNKS.pollFirst();
        }
    }

    private static void retryUnavailableChunk(PendingChunk pending) {
        synchronized (QUEUE_LOCK) {
            if (pending.unavailableAttempts() >= MAX_UNAVAILABLE_RETRIES) {
                PENDING_CHUNK_KEYS.remove(pending.key());
                return;
            }
            PENDING_CHUNKS.addLast(new PendingChunk(pending.key(), pending.unavailableAttempts() + 1));
        }
    }

    private static void completePendingChunk(PendingChunkKey key) {
        synchronized (QUEUE_LOCK) {
            PENDING_CHUNK_KEYS.remove(key);
        }
    }
    
    /**
     * Process a chunk to replace vanilla ores with variants based on surrounding stone types.
     * Optimized to minimize block checks and object allocations.
     */
    private static void processChunk(ServerLevel level, LevelChunk chunk) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Set<Long> examinedOrePositions = new HashSet<>();
        int chunkX = chunk.getPos().x << 4;
        int chunkZ = chunk.getPos().z << 4;
        
        // Optimize Y-level scanning: most ores spawn between Y=-64 and Y=64
        // Deepslate ores: Y=-64 to Y=16
        // Stone ores: Y=-64 to Y=320 (but rare above Y=64)
        int minY = Math.max(level.getMinBuildHeight(), -64);
        int maxY = computeMaxYExclusive(level, chunk);
        
        // Scan through the chunk (Y-first for better cache locality)
        for (int y = minY; y < maxY; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    mutablePos.set(chunkX + x, y, chunkZ + z);
                    BlockState state = chunk.getBlockState(mutablePos);
                    Block block = state.getBlock();
                    
                    // Fast lookup using cached set
                    if (ORE_BLOCKS.contains(block) && examinedOrePositions.add(mutablePos.asLong())) {
                        tryReplaceOreVein(level, chunk, mutablePos, block, examinedOrePositions);
                    }
                }
            }
        }
    }

    /**
     * Determine how high we should scan for ores in this chunk.
     *
     * Worldgen mods can create very tall terrain where exposed ores occur well above Y=80.
     * Use the chunk surface heightmap so we scan high terrain without scanning empty sky
     * up to world max.
     */
    private static int computeMaxYExclusive(ServerLevel level, LevelChunk chunk) {
        int maxSurfaceY = level.getMinBuildHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (surfaceY > maxSurfaceY) {
                    maxSurfaceY = surfaceY;
                }
            }
        }

        int desiredMaxY = Math.max(80, maxSurfaceY + 8);
        return Math.min(level.getMaxBuildHeight(), desiredMaxY);
    }
    
    /**
     * Replace a connected ore vein using the dominant variant stone touching any
     * member of the vein. This prevents interior ores that only touch other ores
     * from being left as vanilla blocks.
     *
     * <p>Traversal remains inside the current chunk to avoid triggering neighboring
     * chunk loads while C2ME is generating terrain asynchronously.</p>
     */
    private static void tryReplaceOreVein(ServerLevel level,
                                          LevelChunk chunk,
                                          BlockPos.MutableBlockPos pos,
                                          Block oreBlock,
                                          Set<Long> examinedOrePositions) {
        Map<Block, Block> variants = ORE_VARIANT_MAP.get(oreBlock);
        if (variants == null) {
            return;
        }
        
        // Get chunk boundaries (cache these values)
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunk.getPos().getMaxBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunk.getPos().getMaxBlockZ();

        // Use fixed-size arrays instead of HashMap for counting (4 stone types max)
        // Index: 0=andesite, 1=diorite, 2=granite, 3=tuff
        int[] stoneCounts = new int[4];
        Block[] stoneTypes = {Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.TUFF};

        ArrayDeque<BlockPos> search = new ArrayDeque<>();
        List<BlockPos> veinPositions = new ArrayList<>();
        search.addLast(pos.immutable());

        while (!search.isEmpty()) {
            BlockPos current = search.removeFirst();
            Block currentOre = chunk.getBlockState(current).getBlock();
            if (ORE_VARIANT_MAP.get(currentOre) != variants) {
                continue;
            }

            veinPositions.add(current);
            for (Direction direction : DIRECTIONS) {
                int neighborX = current.getX() + direction.getStepX();
                int neighborY = current.getY() + direction.getStepY();
                int neighborZ = current.getZ() + direction.getStepZ();

                if (neighborX < chunkMinX || neighborX > chunkMaxX ||
                        neighborZ < chunkMinZ || neighborZ > chunkMaxZ ||
                        neighborY < level.getMinBuildHeight() || neighborY >= level.getMaxBuildHeight()) {
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

                if (ORE_VARIANT_MAP.get(neighborBlock) == variants && examinedOrePositions.add(pos.asLong())) {
                    search.addLast(pos.immutable());
                }
            }
        }

        // Find the most common variant stone type
        int maxCount = 0;
        int dominantIndex = -1;
        for (int i = 0; i < stoneCounts.length; i++) {
            if (stoneCounts[i] > maxCount) {
                maxCount = stoneCounts[i];
                dominantIndex = i;
            }
        }
        
        // If no variant stones are touching, don't replace
        if (dominantIndex == -1) {
            return;
        }

        // Apply one host-stone choice to every ore in the connected family.
        Block dominantStone = stoneTypes[dominantIndex];
        Block variantOre = variants.get(dominantStone);
        if (variantOre != null) {
            BlockState replacement = variantOre.defaultBlockState();
            for (BlockPos veinPos : veinPositions) {
                setGeneratedOreState(level, chunk, veinPos, replacement);
            }
        }
    }

    private static void setGeneratedOreState(ServerLevel level, LevelChunk chunk, BlockPos pos, BlockState state) {
        int sectionIndex = chunk.getSectionIndex(pos.getY());
        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
            return;
        }

        LevelChunkSection section = chunk.getSection(sectionIndex);
        section.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state);
        SableCompat.markChunkUnsaved(chunk);

        // Deferred processing can run after the initial chunk packet has reached a player.
        // Queue a batched section update so tracking clients see the generated replacement.
        level.getChunkSource().blockChanged(pos);
    }

    private record PendingChunkKey(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
    }

    private record PendingChunk(PendingChunkKey key, int unavailableAttempts) {
    }
}

