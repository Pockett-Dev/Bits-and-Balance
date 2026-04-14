package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;

/** Server -> Client: open the Navigator Compass GUI with default coordinates. */
public record NavigatorCompassOpenGUIPayload(int x, int y, int z) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "navigator_compass_open_gui");
    public static final Type<NavigatorCompassOpenGUIPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, NavigatorCompassOpenGUIPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public NavigatorCompassOpenGUIPayload decode(RegistryFriendlyByteBuf buf) {
            return new NavigatorCompassOpenGUIPayload(buf.readInt(), buf.readInt(), buf.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, NavigatorCompassOpenGUIPayload value) {
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
