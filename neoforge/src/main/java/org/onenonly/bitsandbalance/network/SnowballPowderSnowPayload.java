package org.onenonly.bitsandbalance.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.BitsAndBalance;
import javax.annotation.Nonnull;

public record SnowballPowderSnowPayload(int entityId, int freezeTicks) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "snowball_powder_snow");
    public static final Type<SnowballPowderSnowPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SnowballPowderSnowPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SnowballPowderSnowPayload decode(@Nonnull FriendlyByteBuf buf) {
            int ent = buf.readVarInt();
            int ticks = buf.readVarInt();
            return new SnowballPowderSnowPayload(ent, ticks);
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull SnowballPowderSnowPayload value) {
            buf.writeVarInt(value.entityId());
            buf.writeVarInt(value.freezeTicks());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
