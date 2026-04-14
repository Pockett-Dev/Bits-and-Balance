package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;

/** Client -> Server: share a hovered ItemStack into chat. */
public record ItemSharePayload(ItemStack stack) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "share_item");
    public static final Type<ItemSharePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemSharePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ItemSharePayload decode(RegistryFriendlyByteBuf buf) {
            ItemStack stack = ItemStack.STREAM_CODEC.decode(buf);
            if (stack.getCount() > 64) {
                stack = stack.copyWithCount(64);
            } else if (stack.getCount() < 1) {
                stack = stack.copyWithCount(1);
            }
            return new ItemSharePayload(stack);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ItemSharePayload value) {
            ItemStack stack = value.stack();
            int safeCount = Math.max(1, Math.min(64, stack.getCount()));
            ItemStack safeStack = stack.copyWithCount(safeCount);
            ItemStack.STREAM_CODEC.encode(buf, safeStack);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
