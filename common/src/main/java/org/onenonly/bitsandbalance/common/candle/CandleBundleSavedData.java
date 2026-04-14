package org.onenonly.bitsandbalance.common.candle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

public final class CandleBundleSavedData extends SavedData {
    private static final String DATA_NAME = "bitsandbalance_candle_bundles";
    private static final String TAG_BUNDLES = "bundles";

    public record Entry(long pos, List<ItemStack> stacks) {
    }

    public static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("pos").forGetter(Entry::pos),
        ItemStack.CODEC.listOf().optionalFieldOf("stacks", List.of()).forGetter(Entry::stacks)
    ).apply(instance, Entry::new));

    public static final Codec<CandleBundleSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ENTRY_CODEC.listOf().optionalFieldOf(TAG_BUNDLES, List.of()).forGetter(CandleBundleSavedData::toEntries)
    ).apply(instance, entries -> {
        CandleBundleSavedData data = new CandleBundleSavedData();
        data.loadEntries(entries);
        return data;
    }));

    public static final SavedDataType<CandleBundleSavedData> TYPE = new SavedDataType<>(
        DATA_NAME,
        CandleBundleSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Long2ObjectOpenHashMap<List<ItemStack>> bundlesByPos = new Long2ObjectOpenHashMap<>();

    /** Returns a deep-copied snapshot of all bundles for networking/debug purposes. */
    public List<Entry> snapshot() {
        return toEntries();
    }

    public List<ItemStack> get(long posLong) {
        return bundlesByPos.get(posLong);
    }

    public boolean has(long posLong) {
        return bundlesByPos.containsKey(posLong);
    }

    public void set(long posLong, List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            remove(posLong);
            return;
        }

        List<ItemStack> copy = new ArrayList<>(stacks.size());
        for (ItemStack s : stacks) {
            if (s == null || s.isEmpty()) continue;
            copy.add(s.copyWithCount(1));
        }

        if (copy.isEmpty()) {
            remove(posLong);
            return;
        }

        bundlesByPos.put(posLong, copy);
        setDirty();
    }

    public void remove(long posLong) {
        if (bundlesByPos.remove(posLong) != null) {
            setDirty();
        }
    }

    private List<Entry> toEntries() {
        List<Entry> out = new ArrayList<>(bundlesByPos.size());
        for (var entry : bundlesByPos.long2ObjectEntrySet()) {
            List<ItemStack> copy = new ArrayList<>();
            List<ItemStack> list = entry.getValue();
            if (list != null) {
                for (ItemStack s : list) {
                    if (s == null || s.isEmpty()) continue;
                    copy.add(s.copyWithCount(1));
                }
            }
            out.add(new Entry(entry.getLongKey(), copy));
        }
        return out;
    }

    private void loadEntries(List<Entry> entries) {
        bundlesByPos.clear();
        if (entries == null) return;

        for (Entry entry : entries) {
            if (entry == null) continue;
            long pos = entry.pos();
            List<ItemStack> stacks = entry.stacks();
            if (stacks == null || stacks.isEmpty()) continue;

            List<ItemStack> copy = new ArrayList<>(stacks.size());
            for (ItemStack s : stacks) {
                if (s == null || s.isEmpty()) continue;
                copy.add(s.copyWithCount(1));
            }

            if (!copy.isEmpty()) {
                bundlesByPos.put(pos, copy);
            }
        }
    }
}
