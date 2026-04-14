package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.BitsAndBalance;

import javax.annotation.Nonnull;

/**
 * Client -> Server payload to set compass target coordinates.
 * Sent when the player submits coordinates in the Navigator Compass GUI.
 */
public record NavigatorCompassSetPayload(int x, int y, int z) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "navigator_compass_set");
    public static final Type<NavigatorCompassSetPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, NavigatorCompassSetPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public NavigatorCompassSetPayload decode(@Nonnull FriendlyByteBuf buf) {
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            return new NavigatorCompassSetPayload(x, y, z);
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull NavigatorCompassSetPayload value) {
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
    
    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }
}
