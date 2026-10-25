package org.onenonly.bitsandbalance.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.BitsAndBalance;

public record EnhancedSlabMaterialBulkPayload(String dimensionId,
                                              int centerChunkX,
                                              int centerChunkZ,
                                              int chunkRadius,
                                              long[] positions,
                                              CompoundTag[][] slabTags) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "enhanced_slab_material_bulk");
    public static final Type<EnhancedSlabMaterialBulkPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, EnhancedSlabMaterialBulkPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EnhancedSlabMaterialBulkPayload decode(FriendlyByteBuf buf) {
            String dimensionId = buf.readUtf();
            int centerChunkX = buf.readInt();
            int centerChunkZ = buf.readInt();
            int chunkRadius = buf.readVarInt();
            int count = Math.max(0, buf.readVarInt());
            long[] positions = new long[count];
            CompoundTag[][] tags = new CompoundTag[count][];
            for (int i = 0; i < count; i++) {
                positions[i] = buf.readLong();
                tags[i] = EnhancedSlabMaterialUpdatePayload.readSlots(buf);
            }
            return new EnhancedSlabMaterialBulkPayload(dimensionId, centerChunkX, centerChunkZ, chunkRadius, positions, tags);
        }

        @Override
        public void encode(FriendlyByteBuf buf, EnhancedSlabMaterialBulkPayload value) {
            buf.writeUtf(value.dimensionId() == null ? "" : value.dimensionId());
            buf.writeInt(value.centerChunkX());
            buf.writeInt(value.centerChunkZ());
            buf.writeVarInt(Math.max(0, value.chunkRadius()));
            long[] positions = value.positions() == null ? new long[0] : value.positions();
            CompoundTag[][] tags = value.slabTags() == null ? new CompoundTag[0][] : value.slabTags();
            int count = Math.min(positions.length, tags.length);
            buf.writeVarInt(count);
            for (int i = 0; i < count; i++) {
                buf.writeLong(positions[i]);
                EnhancedSlabMaterialUpdatePayload.writeSlots(buf, tags[i]);
            }
        }
    };

    public static EnhancedSlabMaterialBulkPayload of(String dimensionId,
                                                     int centerChunkX,
                                                     int centerChunkZ,
                                                     int chunkRadius,
                                                     long[] positions,
                                                     BlockState[][] slabs) {
        CompoundTag[][] tags = new CompoundTag[slabs == null ? 0 : slabs.length][];
        for (int i = 0; i < tags.length; i++) {
            tags[i] = EnhancedSlabMaterialUpdatePayload.toTags(slabs[i]);
        }
        return new EnhancedSlabMaterialBulkPayload(dimensionId, centerChunkX, centerChunkZ, Math.max(0, chunkRadius),
                positions == null ? new long[0] : positions, tags);
    }

    public BlockState @Nullable [] toSlabs(int index) {
        if (slabTags == null || index < 0 || index >= slabTags.length) {
            return null;
        }
        return EnhancedSlabMaterialUpdatePayload.toSlabs(slabTags[index]);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
