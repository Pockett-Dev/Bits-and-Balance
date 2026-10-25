package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;

/** Adds passive chunk-NBT mirrors to compound enhanced-slab containers from older worlds. */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class NeoForgeEnhancedSlabPortableStorage {
    private NeoForgeEnhancedSlabPortableStorage() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            EnhancedSlabMaterialSavedData.queuePortableBlockEntities(level, chunk);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        EnhancedSlabMaterialSavedData.drainPortableBlockEntities(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        EnhancedSlabMaterialSavedData.clearPortableBlockEntities(event.getServer());
    }
}
