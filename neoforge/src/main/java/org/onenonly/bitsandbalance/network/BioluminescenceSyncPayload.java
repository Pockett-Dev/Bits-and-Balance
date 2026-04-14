package org.onenonly.bitsandbalance.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.BitsAndBalance;

import javax.annotation.Nonnull;

public record BioluminescenceSyncPayload(int entityId, int remainingTicks, int amplifier) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "bioluminescence_sync");
    public static final Type<BioluminescenceSyncPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, BioluminescenceSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BioluminescenceSyncPayload decode(@Nonnull FriendlyByteBuf buf) {
            return new BioluminescenceSyncPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull BioluminescenceSyncPayload value) {
            buf.writeVarInt(value.entityId());
            buf.writeVarInt(value.remainingTicks());
            buf.writeVarInt(value.amplifier());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}