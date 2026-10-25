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

public record BiomeTitlesStructurePayload(boolean insideStructure) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "biome_titles_structure");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "biome_titles_structure");
    //? endif
    public static final Type<BiomeTitlesStructurePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, BiomeTitlesStructurePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BiomeTitlesStructurePayload decode(RegistryFriendlyByteBuf buf) {
            return new BiomeTitlesStructurePayload(buf.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, BiomeTitlesStructurePayload value) {
            buf.writeBoolean(value.insideStructure());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
