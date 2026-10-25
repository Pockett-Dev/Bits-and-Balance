package org.onenonly.bitsandbalance.common.candle;

//? if mc != 1.21.1
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//? endif
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
//? if mc == 1.21.1
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
//? endif
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.resources.Identifier;
//? endif
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
//? if mc != 1.21.1
import net.minecraft.world.level.saveddata.SavedDataType;
//? endif
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
//? endif

import java.util.ArrayList;
import java.util.List;
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import java.util.function.Supplier;
//? endif

public final class CandleBundleSavedData extends SavedData {
    public static final String DATA_NAME = "bitsandbalance_candle_bundles";
    private static final String TAG_BUNDLES = "bundles";
    //? if mc == 1.21.1
    private static final String TAG_POS = "pos";
    private static final String TAG_STACKS = "stacks";

    public static final Factory<CandleBundleSavedData> FACTORY = new Factory<>(
        CandleBundleSavedData::new,
        CandleBundleSavedData::load,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );
    //? endif

    public record Entry(long pos, List<ItemStack> stacks) {
    }

    //? if mc != 1.21.1
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

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    public static final SavedDataType<CandleBundleSavedData> TYPE = bitsandbalance$createType();
    //? else
    public static final SavedDataType<CandleBundleSavedData> TYPE = new SavedDataType<>(
        DATA_NAME,
        CandleBundleSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );
    //? endif
    //? endif

    private final Long2ObjectOpenHashMap<List<ItemStack>> bundlesByPos = new Long2ObjectOpenHashMap<>();

    //? if mc == 1.21.1
    public static CandleBundleSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CandleBundleSavedData data = new CandleBundleSavedData();
        data.loadFromTag(tag, registries);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag bundles = new ListTag();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);

        for (Entry entry : toEntries()) {
            CompoundTag bundleTag = new CompoundTag();
            bundleTag.putLong(TAG_POS, entry.pos());

            ListTag stacksTag = new ListTag();
            for (ItemStack stack : entry.stacks()) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                Tag encoded = ItemStack.CODEC.encodeStart(ops, stack.copyWithCount(1)).result().orElse(null);
                if (encoded != null) {
                    stacksTag.add(encoded);
                }
            }

            if (!stacksTag.isEmpty()) {
                bundleTag.put(TAG_STACKS, stacksTag);
                bundles.add(bundleTag);
            }
        }

        tag.put(TAG_BUNDLES, bundles);
        return tag;
    }

    //? endif
    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static SavedDataType<CandleBundleSavedData> bitsandbalance$createType() {
        Supplier<CandleBundleSavedData> factory = CandleBundleSavedData::new;
        try {
            return (SavedDataType<CandleBundleSavedData>) SavedDataType.class
                    .getConstructor(String.class, java.util.function.Supplier.class, Codec.class, DataFixTypes.class)
                    .newInstance(new Object[]{DATA_NAME, factory, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE});
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            return (SavedDataType<CandleBundleSavedData>) SavedDataType.class
                    .getConstructor(Identifier.class, java.util.function.Supplier.class, Codec.class, DataFixTypes.class)
                    .newInstance(new Object[]{Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, DATA_NAME), factory, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE});
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to construct CandleBundleSavedData SavedDataType", exception);
        }
    }

    //? endif
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

    //? if mc == 1.21.1
    private void loadFromTag(CompoundTag tag, HolderLookup.Provider registries) {
        bundlesByPos.clear();

        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (Tag rawBundle : tag.getList(TAG_BUNDLES, Tag.TAG_COMPOUND)) {
            if (!(rawBundle instanceof CompoundTag bundleTag) || !bundleTag.contains(TAG_POS) || !bundleTag.contains(TAG_STACKS)) {
                continue;
            }

            long pos = bundleTag.getLong(TAG_POS);
            List<ItemStack> copy = new ArrayList<>();
            for (Tag encoded : bundleTag.getList(TAG_STACKS, Tag.TAG_COMPOUND)) {
                ItemStack stack = ItemStack.CODEC.parse(ops, encoded).result().orElse(ItemStack.EMPTY);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                copy.add(stack.copyWithCount(1));
            }

            if (!copy.isEmpty()) {
                bundlesByPos.put(pos, copy);
            }
        }
    }

    //? endif
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

    //? if mc != 1.21.1
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
    //? endif
}
