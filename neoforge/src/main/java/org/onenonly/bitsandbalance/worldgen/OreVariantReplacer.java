package org.onenonly.bitsandbalance.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.blocks.OreVariants;

import java.util.HashMap;
import java.util.Map;

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
    
    /**
     * Initialize the ore variant mapping lazily (called on first use).
     */
    private static void initializeOreVariantMap() {
        if (!ORE_VARIANT_MAP.isEmpty()) {
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
            Block zincOre = BuiltInRegistries.BLOCK.getOptional(Identifier.parse("create:zinc_ore")).orElse(Blocks.AIR);
            Block deepslateZincOre = BuiltInRegistries.BLOCK.getOptional(Identifier.parse("create:deepslate_zinc_ore")).orElse(Blocks.AIR);
            
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
    }
    
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // Initialize the ore variant map on first use (lazy initialization)
        initializeOreVariantMap();
        // Check if ore variants are enabled
        if (!org.onenonly.bitsandbalance.WorldConfig.ENABLE_ORE_VARIANTS.get()) {
            return;
        }
        
        // Only process server-side world generation
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // Get the chunk
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        
        // Process the chunk for ore replacement
        processChunk(serverLevel, chunk);
    }
    
    /**
     * Process a chunk to replace vanilla ores with variants based on surrounding stone types.
     * Optimized to minimize block checks and object allocations.
     */
    private static void processChunk(ServerLevel level, LevelChunk chunk) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int chunkX = chunk.getPos().x << 4;
        int chunkZ = chunk.getPos().z << 4;
        
        // Optimize Y-level scanning: most ores spawn between Y=-64 and Y=64
        // Deepslate ores: Y=-64 to Y=16
        // Stone ores: Y=-64 to Y=320 (but rare above Y=64)
        int minY = Math.max(level.getMinY(), -64);
        int maxY = computeMaxYExclusive(level, chunk);
        
        // Scan through the chunk (Y-first for better cache locality)
        for (int y = minY; y < maxY; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    mutablePos.set(chunkX + x, y, chunkZ + z);
                    BlockState state = chunk.getBlockState(mutablePos);
                    Block block = state.getBlock();
                    
                    // Fast lookup using cached set
                    if (ORE_BLOCKS.contains(block)) {
                        tryReplaceOre(level, chunk, mutablePos, block);
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
        int maxSurfaceY = level.getMinY();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (surfaceY > maxSurfaceY) {
                    maxSurfaceY = surfaceY;
                }
            }
        }

        int desiredMaxY = Math.max(80, maxSurfaceY + 8);
        return Math.min(level.getMaxY(), desiredMaxY);
    }
    
    /**
     * Try to replace an ore block with a variant based on surrounding stone types.
     * Only checks blocks within the current chunk to avoid triggering chunk loads.
     * Optimized to reduce allocations and improve cache locality.
     */
    private static void tryReplaceOre(Level level, LevelChunk chunk, BlockPos.MutableBlockPos pos, Block oreBlock) {
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
        
        // Cache position coordinates
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();
        
        // Check all 6 directions
        for (Direction direction : DIRECTIONS) {
            int neighborX = posX + direction.getStepX();
            int neighborY = posY + direction.getStepY();
            int neighborZ = posZ + direction.getStepZ();
            
            // Skip if neighbor is outside chunk boundaries (prevents chunk loading)
            if (neighborX < chunkMinX || neighborX > chunkMaxX ||
                neighborZ < chunkMinZ || neighborZ > chunkMaxZ) {
                continue;
            }
            
            // Safe to check blocks within the current chunk
            pos.set(neighborX, neighborY, neighborZ);
            BlockState neighborState = chunk.getBlockState(pos);
            Block neighborBlock = neighborState.getBlock();
            
            // Check stone type and increment counter
            for (int i = 0; i < stoneTypes.length; i++) {
                if (neighborBlock == stoneTypes[i]) {
                    stoneCounts[i]++;
                    break;
                }
            }
        }
        
        // Restore original position
        pos.set(posX, posY, posZ);
        
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
        
        // Replace with the appropriate variant
        Block dominantStone = stoneTypes[dominantIndex];
        Block variantOre = variants.get(dominantStone);
        if (variantOre != null) {
            chunk.setBlockState(pos, variantOre.defaultBlockState(), 0);
        }
    }
}

