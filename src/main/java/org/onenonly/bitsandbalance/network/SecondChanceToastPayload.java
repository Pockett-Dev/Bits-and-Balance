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

public record SecondChanceToastPayload() implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "second_chance_toast");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "second_chance_toast");
    //? endif
    public static final Type<SecondChanceToastPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SecondChanceToastPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SecondChanceToastPayload decode(FriendlyByteBuf buf) {
            return new SecondChanceToastPayload();
        }

        @Override
        public void encode(FriendlyByteBuf buf, SecondChanceToastPayload value) {
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//? else
final class SecondChanceToastPayload {
}
//? endif
