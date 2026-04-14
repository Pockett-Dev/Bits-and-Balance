package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.BitsAndBalance;

import javax.annotation.Nonnull;

/**
 * Client -> Server payload to trigger door knocking at a specific position.
 * Sent when the player presses the door knock keybind while looking at a door.
 */
public record DoorKnockPayload(BlockPos doorPos) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "door_knock");
    public static final Type<DoorKnockPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, DoorKnockPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DoorKnockPayload decode(@Nonnull FriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            return new DoorKnockPayload(pos);
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull DoorKnockPayload value) {
            buf.writeBlockPos(value.doorPos());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
