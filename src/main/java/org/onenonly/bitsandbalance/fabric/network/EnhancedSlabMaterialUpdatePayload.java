package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.SlabStateTagHelper;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;

public record EnhancedSlabMaterialUpdatePayload(String dimensionId, long pos, CompoundTag[] slabTags) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "enhanced_slab_material_update");
    public static final Type<EnhancedSlabMaterialUpdatePayload> TYPE = new Type<>(ID);
    private static final int MAX_SLOTS = 4;
    private static final int MAX_PROPERTIES = 32;
    private static final String TAG_BLOCK_NAME = "Name";
    private static final String TAG_BLOCK_PROPERTIES = "Properties";

    public static final StreamCodec<RegistryFriendlyByteBuf, EnhancedSlabMaterialUpdatePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EnhancedSlabMaterialUpdatePayload decode(RegistryFriendlyByteBuf buf) {
            return new EnhancedSlabMaterialUpdatePayload(buf.readUtf(), buf.readLong(), readSlots(buf));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, EnhancedSlabMaterialUpdatePayload value) {
            buf.writeUtf(value.dimensionId() == null ? "" : value.dimensionId());
            buf.writeLong(value.pos());
            writeSlots(buf, value.slabTags());
        }
    };

    public static EnhancedSlabMaterialUpdatePayload of(String dimensionId, long pos, BlockState[] slabs) {
        return new EnhancedSlabMaterialUpdatePayload(dimensionId, pos, toTags(slabs));
    }

    public static CompoundTag[] toTags(BlockState[] slabs) {
        CompoundTag[] tags = new CompoundTag[MAX_SLOTS];
        if (slabs != null) {
            int count = Math.min(MAX_SLOTS, slabs.length);
            for (int i = 0; i < count; i++) {
                BlockState slab = slabs[i];
                tags[i] = slab == null ? new CompoundTag() : SlabStateTagHelper.saveSlabState(slab, Blocks.OAK_SLAB);
            }
        }
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (tags[i] == null) {
                tags[i] = new CompoundTag();
            }
        }
        return tags;
    }

    public BlockState @Nullable [] toSlabs() {
        return toSlabs(slabTags);
    }

    public static BlockState @Nullable [] toSlabs(CompoundTag[] tags) {
        BlockState[] slabs = new BlockState[MAX_SLOTS];
        boolean any = false;
        int count = Math.min(MAX_SLOTS, tags == null ? 0 : tags.length);
        for (int i = 0; i < count; i++) {
            CompoundTag tag = tags[i];
            if (tag == null || tag.isEmpty()) {
                continue;
            }
            slabs[i] = SlabStateTagHelper.loadStoredSlabState(tag, Blocks.OAK_SLAB);
            any = true;
        }
        return any ? slabs : null;
    }

    static void writeSlots(RegistryFriendlyByteBuf buf, CompoundTag[] tags) {
        int count = Math.min(MAX_SLOTS, tags == null ? 0 : tags.length);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            writeSlabTag(buf, tags[i]);
        }
    }

    static CompoundTag[] readSlots(RegistryFriendlyByteBuf buf) {
        int count = Math.min(MAX_SLOTS, Math.max(0, buf.readVarInt()));
        CompoundTag[] tags = new CompoundTag[MAX_SLOTS];
        for (int i = 0; i < count; i++) {
            tags[i] = readSlabTag(buf);
        }
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (tags[i] == null) {
                tags[i] = new CompoundTag();
            }
        }
        return tags;
    }

    private static void writeSlabTag(RegistryFriendlyByteBuf buf, @Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            buf.writeBoolean(false);
            return;
        }

        buf.writeBoolean(true);
        buf.writeUtf(tag.getStringOr(TAG_BLOCK_NAME, ""));

        CompoundTag properties = tag.getCompoundOrEmpty(TAG_BLOCK_PROPERTIES);
        int count = Math.min(MAX_PROPERTIES, properties.size());
        buf.writeVarInt(count);
        int written = 0;
        for (String key : properties.keySet()) {
            if (written >= count) {
                break;
            }
            buf.writeUtf(key == null ? "" : key);
            buf.writeUtf(properties.getStringOr(key, ""));
            written++;
        }
    }

    private static CompoundTag readSlabTag(RegistryFriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return new CompoundTag();
        }

        CompoundTag tag = new CompoundTag();
        String blockName = buf.readUtf();
        if (!blockName.isEmpty()) {
            tag.putString(TAG_BLOCK_NAME, blockName);
        }

        int count = Math.min(MAX_PROPERTIES, Math.max(0, buf.readVarInt()));
        CompoundTag properties = new CompoundTag();
        for (int i = 0; i < count; i++) {
            String key = buf.readUtf();
            String value = buf.readUtf();
            if (!key.isEmpty()) {
                properties.putString(key, value);
            }
        }
        if (!properties.isEmpty()) {
            tag.put(TAG_BLOCK_PROPERTIES, properties);
        }
        return tag;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
