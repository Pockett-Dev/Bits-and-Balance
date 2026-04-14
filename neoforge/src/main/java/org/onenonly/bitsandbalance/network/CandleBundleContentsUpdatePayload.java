package org.onenonly.bitsandbalance.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.BitsAndBalance;

import javax.annotation.Nonnull;

/** Server -> Client: update per-slot candle ItemStacks for a candle bundle at a position. */
public record CandleBundleContentsUpdatePayload(long pos, ItemStack s0, ItemStack s1, ItemStack s2, ItemStack s3) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "candle_bundle_contents_update");
    public static final Type<CandleBundleContentsUpdatePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, CandleBundleContentsUpdatePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CandleBundleContentsUpdatePayload decode(@Nonnull RegistryFriendlyByteBuf buf) {
            long p = buf.readLong();
            ItemStack s0 = decodeOptionalItemStack(buf);
            ItemStack s1 = decodeOptionalItemStack(buf);
            ItemStack s2 = decodeOptionalItemStack(buf);
            ItemStack s3 = decodeOptionalItemStack(buf);
            return new CandleBundleContentsUpdatePayload(p,
                sanitize(s0),
                sanitize(s1),
                sanitize(s2),
                sanitize(s3)
            );
        }

        @Override
        public void encode(@Nonnull RegistryFriendlyByteBuf buf, @Nonnull CandleBundleContentsUpdatePayload value) {
            buf.writeLong(value.pos());
            encodeOptionalItemStack(buf, sanitize(value.s0()));
            encodeOptionalItemStack(buf, sanitize(value.s1()));
            encodeOptionalItemStack(buf, sanitize(value.s2()));
            encodeOptionalItemStack(buf, sanitize(value.s3()));
        }

        private static ItemStack decodeOptionalItemStack(RegistryFriendlyByteBuf buf) {
            boolean present = buf.readBoolean();
            if (!present) {
                return ItemStack.EMPTY;
            }
            return ItemStack.STREAM_CODEC.decode(buf);
        }

        private static void encodeOptionalItemStack(RegistryFriendlyByteBuf buf, ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                buf.writeBoolean(false);
                return;
            }
            buf.writeBoolean(true);
            ItemStack.STREAM_CODEC.encode(buf, stack);
        }

        private static ItemStack sanitize(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int safeCount = Math.max(1, Math.min(64, stack.getCount()));
            return stack.copyWithCount(safeCount);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
