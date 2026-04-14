package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

public final class SnowballPowderSnowPacket {
    private SnowballPowderSnowPacket() {
    }

    public static final Identifier ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "snowball_powder_snow");

    public static FriendlyByteBuf encode(int entityId, int freezeTicks) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(entityId);
        buf.writeVarInt(freezeTicks);
        return buf;
    }
}
