package org.onenonly.bitsandbalance.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import org.onenonly.bitsandbalance.BitsAndBalance;

import javax.annotation.Nonnull;

public record EnhancedSlabHangIntentBulkPayload(long[] positions) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "enhanced_slab_hang_intent_bulk");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "enhanced_slab_hang_intent_bulk");
    //? endif
    public static final Type<EnhancedSlabHangIntentBulkPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, EnhancedSlabHangIntentBulkPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EnhancedSlabHangIntentBulkPayload decode(@Nonnull FriendlyByteBuf buf) {
            return new EnhancedSlabHangIntentBulkPayload(buf.readLongArray());
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull EnhancedSlabHangIntentBulkPayload value) {
            buf.writeLongArray(value.positions());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
