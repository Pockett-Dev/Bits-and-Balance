package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.BitsAndBalance;
import javax.annotation.Nonnull;

/**
 * Client -> Server payload to toggle sitting state for the sending player.
 */
public record SitPayload(boolean sitting) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "sit_toggle");
    public static final Type<SitPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SitPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SitPayload decode(@Nonnull FriendlyByteBuf buf) {
            boolean s = buf.readBoolean();
            return new SitPayload(s);
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull SitPayload value) {
            buf.writeBoolean(value.sitting());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
