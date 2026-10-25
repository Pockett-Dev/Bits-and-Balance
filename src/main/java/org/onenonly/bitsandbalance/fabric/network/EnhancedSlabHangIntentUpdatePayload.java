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

public record EnhancedSlabHangIntentUpdatePayload(long pos, boolean explicit) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "enhanced_slab_hang_intent_update");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "enhanced_slab_hang_intent_update");
    //? endif
    public static final Type<EnhancedSlabHangIntentUpdatePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, EnhancedSlabHangIntentUpdatePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EnhancedSlabHangIntentUpdatePayload decode(RegistryFriendlyByteBuf buf) {
            return new EnhancedSlabHangIntentUpdatePayload(buf.readLong(), buf.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, EnhancedSlabHangIntentUpdatePayload value) {
            buf.writeLong(value.pos());
            buf.writeBoolean(value.explicit());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
