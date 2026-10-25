package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;

/**
 * Client -> Server: craft a colored item in the Taboret Table.
 */
public record TaboretCraftPayload(int colorIndex, int categoryOrdinal, int baseIndex) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "taboret_craft");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "taboret_craft");
    //? endif
    public static final Type<TaboretCraftPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, TaboretCraftPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TaboretCraftPayload decode(RegistryFriendlyByteBuf buf) {
            return new TaboretCraftPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, TaboretCraftPayload value) {
            buf.writeVarInt(value.colorIndex());
            buf.writeVarInt(value.categoryOrdinal());
            buf.writeVarInt(value.baseIndex());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
