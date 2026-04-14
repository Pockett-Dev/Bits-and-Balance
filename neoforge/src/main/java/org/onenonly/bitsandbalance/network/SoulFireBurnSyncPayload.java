package org.onenonly.bitsandbalance.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.BitsAndBalance;

import javax.annotation.Nonnull;

/** Server -> Client: sync whether an entity's current burn originated from soul fire. */
public record SoulFireBurnSyncPayload(int entityId, boolean soulFireBurn) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "soul_fire_burn_sync");
    public static final Type<SoulFireBurnSyncPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SoulFireBurnSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SoulFireBurnSyncPayload decode(@Nonnull FriendlyByteBuf buf) {
            return new SoulFireBurnSyncPayload(buf.readVarInt(), buf.readBoolean());
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull SoulFireBurnSyncPayload value) {
            buf.writeVarInt(value.entityId());
            buf.writeBoolean(value.soulFireBurn());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}