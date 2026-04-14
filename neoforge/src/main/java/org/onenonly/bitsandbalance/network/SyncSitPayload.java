package org.onenonly.bitsandbalance.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.BitsAndBalance;
import javax.annotation.Nonnull;

import java.util.UUID;

/**
 * Server -> Client: sync a player's sitting state.
 */
public record SyncSitPayload(UUID uuid, boolean sitting) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "sync_sit");
    public static final Type<SyncSitPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SyncSitPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncSitPayload decode(@Nonnull FriendlyByteBuf buf) {
            UUID id = buf.readUUID();
            boolean s = buf.readBoolean();
            return new SyncSitPayload(id, s);
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull SyncSitPayload value) {
            buf.writeUUID(value.uuid());
            buf.writeBoolean(value.sitting());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
