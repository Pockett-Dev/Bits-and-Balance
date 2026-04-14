package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;

/** Client -> Server: request to change crawling (forced swimming pose) for the sending player. */
public record CrawlTogglePayload(boolean crawling) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "crawl_toggle");
    public static final Type<CrawlTogglePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, CrawlTogglePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CrawlTogglePayload decode(RegistryFriendlyByteBuf buf) {
            return new CrawlTogglePayload(buf.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, CrawlTogglePayload value) {
            buf.writeBoolean(value.crawling());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
