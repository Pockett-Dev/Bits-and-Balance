package org.onenonly.bitsandbalance.network;

//? if neoforge
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import org.onenonly.bitsandbalance.BitsAndBalance;

public record YearnEchoPayload(double x, double y, double z) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "yearn_echo");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "yearn_echo");
    //? endif
    public static final Type<YearnEchoPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, YearnEchoPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public YearnEchoPayload decode(FriendlyByteBuf buf) {
            return new YearnEchoPayload(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        @Override
        public void encode(FriendlyByteBuf buf, YearnEchoPayload value) {
            buf.writeDouble(value.x());
            buf.writeDouble(value.y());
            buf.writeDouble(value.z());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//? else
final class YearnEchoPayload {
}
//? endif
