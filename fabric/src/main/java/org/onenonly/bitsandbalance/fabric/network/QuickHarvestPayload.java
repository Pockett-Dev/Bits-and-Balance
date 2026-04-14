package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;

/** Client -> Server: request quick-harvesting at a targeted block with a specific hand. */
public record QuickHarvestPayload(BlockPos pos, InteractionHand hand) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "quick_harvest");
    public static final Type<QuickHarvestPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, QuickHarvestPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public QuickHarvestPayload decode(RegistryFriendlyByteBuf buf) {
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
            InteractionHand hand = buf.readEnum(InteractionHand.class);
            return new QuickHarvestPayload(pos, hand);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, QuickHarvestPayload value) {
            BlockPos.STREAM_CODEC.encode(buf, value.pos());
            buf.writeEnum(value.hand());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}