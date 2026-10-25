package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

/** Server -> Client: update per-slot candle ItemStacks for a candle bundle at a position. */
public record CandleBundleContentsUpdatePayload(long pos, Direction facing, ItemStack s0, ItemStack s1, ItemStack s2, ItemStack s3) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "candle_bundle_contents_update");
    public static final Type<CandleBundleContentsUpdatePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, CandleBundleContentsUpdatePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CandleBundleContentsUpdatePayload decode(RegistryFriendlyByteBuf buf) {
            long p = buf.readLong();
            Direction facing = decodeFacing(buf);
            ItemStack s0 = decodeOptionalItemStack(buf);
            ItemStack s1 = decodeOptionalItemStack(buf);
            ItemStack s2 = decodeOptionalItemStack(buf);
            ItemStack s3 = decodeOptionalItemStack(buf);
            return new CandleBundleContentsUpdatePayload(p,
                facing,
                sanitize(s0),
                sanitize(s1),
                sanitize(s2),
                sanitize(s3)
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, CandleBundleContentsUpdatePayload value) {
            buf.writeLong(value.pos());
            encodeFacing(buf, value.facing());
            encodeOptionalItemStack(buf, sanitize(value.s0()));
            encodeOptionalItemStack(buf, sanitize(value.s1()));
            encodeOptionalItemStack(buf, sanitize(value.s2()));
            encodeOptionalItemStack(buf, sanitize(value.s3()));
        }

        private static Direction decodeFacing(RegistryFriendlyByteBuf buf) {
            return sanitizeFacing(Direction.from2DDataValue(buf.readByte()));
        }

        private static void encodeFacing(RegistryFriendlyByteBuf buf, Direction facing) {
            buf.writeByte(sanitizeFacing(facing).get2DDataValue());
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

        private static Direction sanitizeFacing(Direction facing) {
            if (facing == null || !facing.getAxis().isHorizontal()) {
                return Direction.SOUTH;
            }
            return facing;
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
