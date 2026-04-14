package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;
import org.onenonly.bitsandbalance.common.inventory.SortInventoryMode;
import org.onenonly.bitsandbalance.common.inventory.SortInventoryTarget;

/** Client -> Server: sort either player inventory or the open container. */
public record SortInventoryPayload(SortInventoryTarget target, SortInventoryMode mode) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "sort_inventory");
    public static final Type<SortInventoryPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SortInventoryPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SortInventoryPayload decode(RegistryFriendlyByteBuf buf) {
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
        public void encode(RegistryFriendlyByteBuf buf, SortInventoryPayload value) {
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
