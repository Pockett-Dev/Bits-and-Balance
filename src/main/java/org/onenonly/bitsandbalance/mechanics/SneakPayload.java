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
 * Client -> Server payload to toggle sneaking state for the sending player.
 */
public record SneakPayload(boolean sneaking) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "sneak_toggle");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "sneak_toggle");
    //? endif
    public static final Type<SneakPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SneakPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SneakPayload decode(@Nonnull FriendlyByteBuf buf) {
            boolean s = buf.readBoolean();
            return new SneakPayload(s);
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull SneakPayload value) {
            buf.writeBoolean(value.sneaking());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
