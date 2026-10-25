package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

public record SecondChanceToastPayload() implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "second_chance_toast");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "second_chance_toast");
    //? endif
    public static final Type<SecondChanceToastPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SecondChanceToastPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SecondChanceToastPayload decode(RegistryFriendlyByteBuf buf) {
            return new SecondChanceToastPayload();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SecondChanceToastPayload value) {
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
