package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;

/** Client -> Server: request to change sitting state for the sending player. */
public record SitTogglePayload(boolean sitting) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "sit_toggle");
    public static final Type<SitTogglePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SitTogglePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SitTogglePayload decode(RegistryFriendlyByteBuf buf) {
            return new SitTogglePayload(buf.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SitTogglePayload value) {
            buf.writeBoolean(value.sitting());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
