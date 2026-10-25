package org.onenonly.bitsandbalance.common.client;

/**
 * Marker interface injected onto {@code EntityRenderState} so we can carry whether an entity's
 * burn should be rendered as soul fire through Minecraft's render-state pipeline.
 */
public interface SoulFireFlameRenderStateAccess {
    boolean bitsandbalance$isSoulFireFlame();

    void bitsandbalance$setSoulFireFlame(boolean soulFire);
}
