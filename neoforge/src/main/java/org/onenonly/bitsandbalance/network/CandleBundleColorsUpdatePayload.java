package org.onenonly.bitsandbalance.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.BitsAndBalance;

import javax.annotation.Nonnull;

/** Server -> Client: update per-candle colors for a candle bundle at a position. */
public record CandleBundleColorsUpdatePayload(long pos, int c0, int c1, int c2, int c3) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "candle_bundle_colors_update");
    public static final Type<CandleBundleColorsUpdatePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, CandleBundleColorsUpdatePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CandleBundleColorsUpdatePayload decode(@Nonnull FriendlyByteBuf buf) {
            long p = buf.readLong();
            int c0 = buf.readInt();
            int c1 = buf.readInt();
            int c2 = buf.readInt();
            int c3 = buf.readInt();
            return new CandleBundleColorsUpdatePayload(p, c0, c1, c2, c3);
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull CandleBundleColorsUpdatePayload value) {
            buf.writeLong(value.pos());
            buf.writeInt(value.c0());
            buf.writeInt(value.c1());
            buf.writeInt(value.c2());
            buf.writeInt(value.c3());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
