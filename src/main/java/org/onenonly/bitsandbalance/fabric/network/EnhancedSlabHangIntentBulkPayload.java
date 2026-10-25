package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;

public record EnhancedSlabHangIntentBulkPayload(long[] positions) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "enhanced_slab_hang_intent_bulk");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "enhanced_slab_hang_intent_bulk");
    //? endif
    public static final Type<EnhancedSlabHangIntentBulkPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, EnhancedSlabHangIntentBulkPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EnhancedSlabHangIntentBulkPayload decode(RegistryFriendlyByteBuf buf) {
            return new EnhancedSlabHangIntentBulkPayload(buf.readLongArray());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, EnhancedSlabHangIntentBulkPayload value) {
            buf.writeLongArray(value.positions());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
