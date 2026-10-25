package org.onenonly.bitsandbalance.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import org.onenonly.bitsandbalance.BitsAndBalance;

public record BiomeTitlesStructurePayload(boolean insideStructure) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "biome_titles_structure");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "biome_titles_structure");
    //? endif
    public static final Type<BiomeTitlesStructurePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, BiomeTitlesStructurePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BiomeTitlesStructurePayload decode(FriendlyByteBuf buf) {
            return new BiomeTitlesStructurePayload(buf.readBoolean());
        }

        @Override
        public void encode(FriendlyByteBuf buf, BiomeTitlesStructurePayload value) {
            buf.writeBoolean(value.insideStructure());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
