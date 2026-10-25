package org.onenonly.bitsandbalance.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

public record BioluminescenceSyncPayload(int entityId, int remainingTicks, int amplifier) implements CustomPacketPayload {
    //? if mc == 1.21.1
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "bioluminescence_sync");
    //? else
    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "bioluminescence_sync");
    //? endif
    public static final Type<BioluminescenceSyncPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, BioluminescenceSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BioluminescenceSyncPayload decode(RegistryFriendlyByteBuf buf) {
            return new BioluminescenceSyncPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, BioluminescenceSyncPayload value) {
            buf.writeVarInt(value.entityId());
            buf.writeVarInt(value.remainingTicks());
            buf.writeVarInt(value.amplifier());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}