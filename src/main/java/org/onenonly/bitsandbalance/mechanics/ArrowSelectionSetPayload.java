package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.BitsAndBalance;

/** NeoForge client request; server validates held weapon and available ammunition. */
public record ArrowSelectionSetPayload(InteractionHand hand, ItemStack arrow) implements CustomPacketPayload {
    public static final Type<ArrowSelectionSetPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BitsAndBalance.MODID, "arrow_selection_set"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArrowSelectionSetPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ArrowSelectionSetPayload decode(RegistryFriendlyByteBuf buf) {
            return new ArrowSelectionSetPayload(buf.readEnum(InteractionHand.class),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ArrowSelectionSetPayload value) {
            buf.writeEnum(value.hand());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, value.arrow().isEmpty() ? ItemStack.EMPTY : value.arrow().copyWithCount(1));
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
