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

import javax.annotation.Nonnull;

/**
 * Network payload for sending player mention notifications from server to client.
 * This allows the server to notify clients when they are mentioned in chat.
 */
public record PlayerMentionPayload(String mentionedPlayer, String messageSender) implements CustomPacketPayload {
    
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "player_mention");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "player_mention");
    //? endif
    public static final Type<PlayerMentionPayload> TYPE = new Type<>(ID);
    
    public static final StreamCodec<FriendlyByteBuf, PlayerMentionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PlayerMentionPayload decode(@Nonnull FriendlyByteBuf buf) {
            String mentionedPlayer = buf.readUtf();
            String messageSender = buf.readUtf();
            return new PlayerMentionPayload(mentionedPlayer, messageSender);
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull PlayerMentionPayload value) {
            buf.writeUtf(value.mentionedPlayer());
            buf.writeUtf(value.messageSender());
        }
    };
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
