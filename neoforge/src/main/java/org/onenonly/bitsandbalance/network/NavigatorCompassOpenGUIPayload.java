package org.onenonly.bitsandbalance.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.BitsAndBalance;

import javax.annotation.Nonnull;

/**
 * Server -> Client payload to open the Navigator Compass GUI.
 * Sent when the player right-clicks a compass.
 */
public record NavigatorCompassOpenGUIPayload(int x, int y, int z) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "navigator_compass_open_gui");
    public static final Type<NavigatorCompassOpenGUIPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, NavigatorCompassOpenGUIPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public NavigatorCompassOpenGUIPayload decode(@Nonnull FriendlyByteBuf buf) {
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            return new NavigatorCompassOpenGUIPayload(x, y, z);
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull NavigatorCompassOpenGUIPayload value) {
            buf.writeInt(value.x());
            buf.writeInt(value.y());
            buf.writeInt(value.z());
        }
    };

    @Override
    @Nonnull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
