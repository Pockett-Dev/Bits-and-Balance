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

import java.util.UUID;

/** Server -> Client: sync a player's sitting state. */
public record SyncSitPayload(UUID uuid, boolean sitting) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "sync_sit");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "sync_sit");
    //? endif
    public static final Type<SyncSitPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSitPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncSitPayload decode(RegistryFriendlyByteBuf buf) {
            return new SyncSitPayload(buf.readUUID(), buf.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SyncSitPayload value) {
            buf.writeUUID(value.uuid());
            buf.writeBoolean(value.sitting());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
