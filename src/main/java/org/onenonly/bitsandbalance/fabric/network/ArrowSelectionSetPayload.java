package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;

/** Requests a preferred arrow for the weapon currently held in the specified hand. */
public record ArrowSelectionSetPayload(InteractionHand hand, ItemStack arrow) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "arrow_selection_set");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "arrow_selection_set");
    //? endif
    public static final Type<ArrowSelectionSetPayload> TYPE = new Type<>(ID);
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
