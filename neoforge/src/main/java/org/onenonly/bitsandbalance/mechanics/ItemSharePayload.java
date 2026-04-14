package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.BitsAndBalance;
import javax.annotation.Nonnull;

/**
 * Client -> Server payload to share a hovered ItemStack into chat.
 * Includes full NBT data to preserve enchantments, lore, and other item properties.
 */
public record ItemSharePayload(ItemStack stack) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "share_item");
    public static final Type<ItemSharePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemSharePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ItemSharePayload decode(@Nonnull RegistryFriendlyByteBuf buf) {
            ItemStack stack = ItemStack.STREAM_CODEC.decode(buf);
            // Ensure count is within reasonable bounds for safety
            if (stack.getCount() > 64) {
                stack = stack.copyWithCount(64);
            } else if (stack.getCount() < 1) {
                stack = stack.copyWithCount(1);
            }
            return new ItemSharePayload(stack);
        }

        @Override
        public void encode(@Nonnull RegistryFriendlyByteBuf buf, @Nonnull ItemSharePayload value) {
            ItemStack stack = value.stack();
            // Ensure count is within reasonable bounds for safety
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
