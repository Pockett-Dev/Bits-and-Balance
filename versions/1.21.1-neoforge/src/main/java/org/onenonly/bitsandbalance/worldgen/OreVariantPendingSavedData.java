package org.onenonly.bitsandbalance.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists newly generated chunks that still need their ore-variant replacement pass.
 */
public final class OreVariantPendingSavedData extends SavedData {
    private static final String DATA_NAME = "bitsandbalance_pending_ore_variant_chunks";
    private static final String TAG_CHUNKS = "chunks";

    private static final Codec<OreVariantPendingSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.listOf().optionalFieldOf(TAG_CHUNKS, List.of()).forGetter(OreVariantPendingSavedData::toChunks)
    ).apply(instance, OreVariantPendingSavedData::fromChunks));

    private static final Factory<OreVariantPendingSavedData> FACTORY = new Factory<>(
            OreVariantPendingSavedData::new,
            OreVariantPendingSavedData::load,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final LongOpenHashSet pendingChunks = new LongOpenHashSet();

    public static OreVariantPendingSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static OreVariantPendingSavedData fromChunks(List<Long> chunks) {
        OreVariantPendingSavedData data = new OreVariantPendingSavedData();
        for (Long chunk : chunks) {
            if (chunk != null) {
                data.pendingChunks.add(chunk.longValue());
            }
        }
        return data;
    }

    public static OreVariantPendingSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag)
                .result()
                .orElseGet(OreVariantPendingSavedData::new);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return (CompoundTag) CODEC.encodeStart(
                        registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                        this
                )
                .result()
                .orElseGet(CompoundTag::new);
    }

    public boolean contains(long chunkPos) {
        return pendingChunks.contains(chunkPos);
    }

    public void add(long chunkPos) {
        if (pendingChunks.add(chunkPos)) {
            setDirty();
        }
    }

    public void remove(long chunkPos) {
        if (pendingChunks.remove(chunkPos)) {
            setDirty();
        }
    }

    private List<Long> toChunks() {
        List<Long> chunks = new ArrayList<>(pendingChunks.size());
        for (long chunk : pendingChunks) {
            chunks.add(chunk);
        }
        return chunks;
    }
}
