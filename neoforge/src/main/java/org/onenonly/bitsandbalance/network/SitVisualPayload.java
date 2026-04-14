package org.onenonly.bitsandbalance.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.BitsAndBalance;
import javax.annotation.Nonnull;

/**
 * Server -> Client: inform clients that an entity should be visually rendered as sitting (riding pose)
 * without actually being a passenger.
 */
public record SitVisualPayload(int entityId, boolean sitting) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "sit_visual");
    public static final Type<SitVisualPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SitVisualPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SitVisualPayload decode(@Nonnull FriendlyByteBuf buf) {
            int ent = buf.readVarInt();
            boolean sit = buf.readBoolean();
            return new SitVisualPayload(ent, sit);
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull SitVisualPayload value) {
            buf.writeVarInt(value.entityId());
            buf.writeBoolean(value.sitting());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
