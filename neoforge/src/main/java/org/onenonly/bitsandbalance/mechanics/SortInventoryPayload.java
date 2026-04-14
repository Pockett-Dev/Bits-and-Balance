package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.inventory.SortInventoryMode;
import org.onenonly.bitsandbalance.common.inventory.SortInventoryTarget;

import javax.annotation.Nonnull;

/** Client -> Server: sort either player inventory or the open container. */
public record SortInventoryPayload(SortInventoryTarget target, SortInventoryMode mode) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "sort_inventory");
    public static final Type<SortInventoryPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SortInventoryPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SortInventoryPayload decode(@Nonnull FriendlyByteBuf buf) {
            int targetId;
            try {
                targetId = buf.readVarInt();
            } catch (Throwable t) {
                targetId = 0;
            }

            int modeId;
            try {
                modeId = buf.readVarInt();
            } catch (Throwable t) {
                modeId = 0;
            }

            return new SortInventoryPayload(SortInventoryTarget.fromId(targetId), SortInventoryMode.fromId(modeId));
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf, @Nonnull SortInventoryPayload value) {
            int targetId = 0;
            int modeId = 0;
            try {
                if (value != null) {
                    if (value.target() != null) {
                        targetId = value.target().id();
                    }
                    if (value.mode() != null) {
                        modeId = value.mode().id();
                    }
                }
            } catch (Throwable ignored) {
            }
            buf.writeVarInt(targetId);
            buf.writeVarInt(modeId);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
