package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

/** Server -> Client: initial sync of all candle bundle per-candle colors (best-effort, small maps expected). */
public record CandleBundleColorsBulkPayload(long[] positions, int[] c0, int[] c1, int[] c2, int[] c3) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "candle_bundle_colors_bulk");
    public static final Type<CandleBundleColorsBulkPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, CandleBundleColorsBulkPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CandleBundleColorsBulkPayload decode(RegistryFriendlyByteBuf buf) {
            int n = buf.readVarInt();
            long[] pos = new long[n];
            int[] c0 = new int[n];
            int[] c1 = new int[n];
            int[] c2 = new int[n];
            int[] c3 = new int[n];
            for (int i = 0; i < n; i++) {
                pos[i] = buf.readLong();
                c0[i] = buf.readInt();
                c1[i] = buf.readInt();
                c2[i] = buf.readInt();
                c3[i] = buf.readInt();
            }
            return new CandleBundleColorsBulkPayload(pos, c0, c1, c2, c3);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, CandleBundleColorsBulkPayload value) {
            int n = value.positions().length;
            n = Math.min(n, value.c0().length);
            n = Math.min(n, value.c1().length);
            n = Math.min(n, value.c2().length);
            n = Math.min(n, value.c3().length);
            buf.writeVarInt(n);
            for (int i = 0; i < n; i++) {
                buf.writeLong(value.positions()[i]);
                buf.writeInt(value.c0()[i]);
                buf.writeInt(value.c1()[i]);
                buf.writeInt(value.c2()[i]);
                buf.writeInt(value.c3()[i]);
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
