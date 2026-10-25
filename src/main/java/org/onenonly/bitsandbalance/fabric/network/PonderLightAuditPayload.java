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

public record PonderLightAuditPayload(double x, double y, double z, int emission) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "ponder_light_audit");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "ponder_light_audit");
    //? endif
    public static final Type<PonderLightAuditPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PonderLightAuditPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PonderLightAuditPayload decode(RegistryFriendlyByteBuf buf) {
            return new PonderLightAuditPayload(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, PonderLightAuditPayload value) {
            buf.writeDouble(value.x());
            buf.writeDouble(value.y());
            buf.writeDouble(value.z());
            buf.writeVarInt(value.emission());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
