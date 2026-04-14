package org.onenonly.bitsandbalance.mechanics.modules;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;

/**
 * Registration class for Leashed Teleport data attachments.
 * 
 * This class handles the registration of data attachments used by the
 * Leashed Teleport module to track pending follower teleports.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class LeashedTeleportAttachments {
    
    // Data attachment for tracking pending follower teleports
    public static final AttachmentType<PendingFollowers> PENDING_FOLLOWERS = 
        AttachmentType.builder(() -> PendingFollowers.create(
            java.util.List.of(), 
            net.minecraft.world.level.Level.OVERWORLD, 
            0L, 
            net.minecraft.world.phys.Vec3.ZERO, 
            0L
        )).build();
    
    /**
     * Registers the data attachment when the registry event fires.
     */
    @SubscribeEvent
    public static void registerAttachments(RegisterEvent event) {
        if (event.getRegistryKey().equals(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.ATTACHMENT_TYPES)) {
            event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.ATTACHMENT_TYPES, 
                net.minecraft.resources.Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "pending_followers"), 
                () -> PENDING_FOLLOWERS);
        }
    }
}
