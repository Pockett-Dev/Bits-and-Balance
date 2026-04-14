package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;

/** Client -> Server: request to knock on a door at the given position. */
public record DoorKnockPayload(BlockPos doorPos) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "door_knock");
    public static final Type<DoorKnockPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, DoorKnockPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DoorKnockPayload decode(RegistryFriendlyByteBuf buf) {
            return new DoorKnockPayload(buf.readBlockPos());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, DoorKnockPayload value) {
            buf.writeBlockPos(value.doorPos());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
