package org.onenonly.bitsandbalance.common.mechanics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
//? if mc == 1.21.1
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
//? else
import net.minecraft.resources.Identifier;
//? endif
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
//? if mc != 1.21.1
import net.minecraft.world.level.saveddata.SavedDataType;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
//? endif

import java.util.ArrayList;
import java.util.List;
//? if mc != 1.21.1
import java.util.function.Supplier;
//? endif

public final class EnhancedSlabHangIntentSavedData extends SavedData {
//? if mc == 1.21.1
    public static final String DATA_NAME = "bitsandbalance_enhanced_slab_hang_intent";
//? else
    private static final String DATA_NAME = "bitsandbalance_enhanced_slab_hang_intent";
//? endif
    private static final String TAG_POSITIONS = "positions";

    public static final Codec<EnhancedSlabHangIntentSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.listOf().optionalFieldOf(TAG_POSITIONS, List.of()).forGetter(EnhancedSlabHangIntentSavedData::bitsandbalance$toPositions)
    ).apply(instance, EnhancedSlabHangIntentSavedData::bitsandbalance$fromPositions));

//? if mc == 1.21.1
    public static final Factory<EnhancedSlabHangIntentSavedData> FACTORY = new Factory<>(
            EnhancedSlabHangIntentSavedData::new,
            EnhancedSlabHangIntentSavedData::load,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );
//? else
    public static final SavedDataType<EnhancedSlabHangIntentSavedData> TYPE = bitsandbalance$createType();
//? endif

    private final LongOpenHashSet explicitHangPositions = new LongOpenHashSet();

//? if mc != 1.21.1
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static SavedDataType<EnhancedSlabHangIntentSavedData> bitsandbalance$createType() {
        Supplier<EnhancedSlabHangIntentSavedData> factory = EnhancedSlabHangIntentSavedData::new;
        try {
            return (SavedDataType<EnhancedSlabHangIntentSavedData>) SavedDataType.class
                    .getConstructor(String.class, java.util.function.Supplier.class, Codec.class, DataFixTypes.class)
                    .newInstance(new Object[]{DATA_NAME, factory, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE});
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            return (SavedDataType<EnhancedSlabHangIntentSavedData>) SavedDataType.class
                    .getConstructor(Identifier.class, java.util.function.Supplier.class, Codec.class, DataFixTypes.class)
                    .newInstance(new Object[]{Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, DATA_NAME), factory, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE});
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to construct EnhancedSlabHangIntentSavedData SavedDataType", exception);
        }
    }
//? endif

    private static EnhancedSlabHangIntentSavedData bitsandbalance$fromPositions(List<Long> positions) {
        EnhancedSlabHangIntentSavedData data = new EnhancedSlabHangIntentSavedData();
        if (positions != null) {
            for (Long posLong : positions) {
                if (posLong != null) {
                    data.explicitHangPositions.add(posLong.longValue());
                }
            }
        }
        return data;
    }

//? if mc == 1.21.1
    public static EnhancedSlabHangIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag)
                .result()
                .orElseGet(EnhancedSlabHangIntentSavedData::new);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return (CompoundTag) CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), this)
                .result()
                .orElseGet(CompoundTag::new);
    }
//? endif

    private List<Long> bitsandbalance$toPositions() {
        List<Long> positions = new ArrayList<>(explicitHangPositions.size());
        for (long posLong : explicitHangPositions) {
            positions.add(posLong);
        }
        return positions;
    }

    public boolean has(long posLong) {
        return explicitHangPositions.contains(posLong);
    }

    public boolean set(long posLong, boolean explicit) {
        boolean changed = explicit ? explicitHangPositions.add(posLong) : explicitHangPositions.remove(posLong);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean remove(long posLong) {
        boolean changed = explicitHangPositions.remove(posLong);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public long[] snapshot() {
        return explicitHangPositions.toLongArray();
    }
}
