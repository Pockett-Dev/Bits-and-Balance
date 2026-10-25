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

/**
 * Server -> Client: tell clients to render Powder Snow visuals for an entity for a duration.
 */
public record SnowballPowderSnowPayload(int entityId, int freezeTicks) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "snowball_powder_snow");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "snowball_powder_snow");
    //? endif
    public static final Type<SnowballPowderSnowPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SnowballPowderSnowPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SnowballPowderSnowPayload decode(RegistryFriendlyByteBuf buf) {
            int ent = buf.readVarInt();
            int ticks = buf.readVarInt();
            return new SnowballPowderSnowPayload(ent, ticks);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SnowballPowderSnowPayload value) {
            buf.writeVarInt(value.entityId());
            buf.writeVarInt(value.freezeTicks());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
