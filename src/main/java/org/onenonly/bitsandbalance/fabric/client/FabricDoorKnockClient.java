package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.network.DoorKnockPayload;

/**
 * Client-side door knocking handler: raycast to find door and send packet.
 */
public final class FabricDoorKnockClient {
    private FabricDoorKnockClient() {
    }

    public static void initClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!FabricMechanicsConfig.enableDoorKnocking) return;
            if (client.player == null) return;

            if (FabricKeyBindings.DOOR_KNOCK.consumeClick()) {
                HitResult hit = client.hitResult;
                if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos pos = blockHit.getBlockPos();
                    BlockState state = client.level.getBlockState(pos);
                    
                    if (state.getBlock() instanceof DoorBlock) {
                        if (ClientPlayNetworking.canSend(DoorKnockPayload.TYPE)) {
                            ClientPlayNetworking.send(new DoorKnockPayload(pos));
                        }
                    }
                }
            }
        });
    }
}
