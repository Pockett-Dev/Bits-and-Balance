package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;

/** Client -> Server: set/clear navigator compass target coordinates. */
public record NavigatorCompassSetPayload(int x, int y, int z) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "navigator_compass_set");
    public static final Type<NavigatorCompassSetPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, NavigatorCompassSetPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public NavigatorCompassSetPayload decode(RegistryFriendlyByteBuf buf) {
            return new NavigatorCompassSetPayload(buf.readInt(), buf.readInt(), buf.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, NavigatorCompassSetPayload value) {
            buf.writeInt(value.x());
            buf.writeInt(value.y());
            buf.writeInt(value.z());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
