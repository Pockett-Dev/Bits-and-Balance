package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import org.onenonly.bitsandbalance.BitsAndBalance;

import javax.annotation.Nonnull;

/**
 * Client -> Server payload to toggle crawling state for the sending player.
 */
public record CrawlPayload(boolean crawling) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "crawl_toggle");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "crawl_toggle");
    //? endif
    public static final Type<CrawlPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, CrawlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CrawlPayload decode(@Nonnull FriendlyByteBuf buf) {
            boolean c = buf.readBoolean();
            return new CrawlPayload(c);
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull CrawlPayload value) {
            buf.writeBoolean(value.crawling());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
