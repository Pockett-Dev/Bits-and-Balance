package org.onenonly.bitsandbalance.mechanics.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.mechanics.FeatureModule;

/**
 * Depth Scaling Enemies Module
 * 
 * Scales enemy equipment and difficulty based on their spawn depth.
 * The deeper enemies spawn, the better equipment they receive.
 * 
 * Features:
 * - Three depth tiers: Surface, Mid, Deep (configurable Y levels)
 * - Configurable equipment chances per tier
 * - Weighted armor material selection (leather, chain, iron, gold)
 * - Iron piece limits for deep tier to prevent overpowering
 * - Zombie weapon assignment with material-based chances
 * - Overworld-only option (configurable)
 * - Deferred equipment application to avoid vanilla conflicts
 * 
 * Depth Tiers:
 * - Surface: Y > mid depth (default equipment)
 * - Mid: deep depth < Y <= mid depth (moderate equipment)
 * - Deep: Y <= deep depth (best equipment, iron limits)
 * 
 * This creates natural progression where deeper exploration
 * means facing tougher, better-equipped enemies.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class DepthScalingEnemiesModule implements FeatureModule {
    
    /**
     * Depth tier classification based on Y level
     */
    private enum DepthTier { SURFACE, MID, DEEP }
    
    @Override
    public String getFeatureName() {
        return "Depth Scaling Enemies";
    }
    
    @Override
    public boolean isEnabled() {
        return Config.dseEnabled;
    }
    
    @Override
    public int getInitializationPriority() {
        return 700; // Lower priority - enemy enhancement system
    }
    
    /**
     * Main depth scaling logic - triggered when entities join the world
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!Config.dseEnabled) return;
        
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        
        // Only apply to zombies and skeletons
        if (!(entity instanceof Zombie || entity instanceof Skeleton)) return;
        
        // Skip entities loaded from disk/worldgen pass to avoid deadlocks during chunk promotion
        if (entity.tickCount > 0) return;
        
        try {
            Mob mob = (Mob) entity;
            ServerLevel level = (ServerLevel) event.getLevel();
            
            // Defer depth tier calculation and equipment to avoid chunk loading deadlocks during C2ME chunk upgrades
            level.getServer().execute(() -> {
                try {
                    if (!mob.isAlive() || mob.isRemoved()) return;
                    
                    // Check if chunk is available before querying heightmap (for overworld dimension check)
                    var chunkSource = level.getChunkSource();
                    if (chunkSource.getChunkNow(mob.chunkPosition().x, mob.chunkPosition().z) == null) {
                        return; // Don't force-load chunks
                    }
                    
                    // Now safe to calculate depth tier since chunk is fully promoted
                    DepthTier tier = getDepthTier(mob.blockPosition(), level);
                    if (!isTierEnabled(tier)) return;
                    applyDepthScaling(mob, tier);
                } catch (Throwable ignored) {
                    // Fail silently to avoid crashes
                }
            });
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }

    private static boolean isTierEnabled(DepthTier tier) {
        return switch (tier) {
            case SURFACE -> Config.dseEnableSurfaceTier;
            case MID -> Config.dseEnableMidTier;
            case DEEP -> Config.dseEnableDeepTier;
        };
    }
    
    /**
     * Determines the depth tier based on entity position and level
     */
    private static DepthTier getDepthTier(BlockPos pos, ServerLevel level) {
        // Check if we should only apply in overworld
        if (Config.dseOverworldOnly && !level.dimension().equals(Level.OVERWORLD)) {
            return DepthTier.SURFACE;
        }
        
        int y = pos.getY();
        int mid = Config.dseMidDepthY;
        int deep = Config.dseDeepDepthY;
        
        if (y <= deep) return DepthTier.DEEP;
        if (y <= mid) return DepthTier.MID;
        return DepthTier.SURFACE;
    }
    
    /**
     * Applies depth-based scaling to a mob, including armor and weapons
     */
    private static void applyDepthScaling(Mob mob, DepthTier tier) {
        try {
            applyArmorScaling(mob, tier);
            
            // Apply weapon scaling only to zombies
            if (mob instanceof Zombie zombie) {
                applyWeaponScaling(zombie, tier);
            }
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }
    
    /**
     * Applies armor scaling based on depth tier
     */
    private static void applyArmorScaling(Mob mob, DepthTier tier) {
        // Get tier-specific configuration
        ArmorConfig armorConfig = getArmorConfig(tier);
        
        int ironEquipped = 0;
        RandomSource rand = mob.getRandom();
        
        // Apply armor to each equipment slot.
        // Bias chestplates to be the most common piece across all tiers.
        for (EquipmentSlot slot : new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {

            double slotMultiplier = switch (slot) {
                case CHEST -> 1.40D;
                case LEGS -> 1.00D;
                case HEAD, FEET -> 0.90D;
                default -> 1.00D;
            };

            double slotEquipChance = Math.min(1.0D, armorConfig.equipChance * slotMultiplier);

            // Check if this slot should get armor
            if (rand.nextDouble() >= slotEquipChance) continue;
            
            // Check iron piece limit for deep tier
            if (ironEquipped >= armorConfig.maxIronPieces && tier == DepthTier.DEEP) continue;
            
            // Select armor material based on weights
            String material = selectArmorMaterial(rand, armorConfig);
            
            // Apply iron limit fallback for deep tier
            if ("iron".equals(material) && ironEquipped >= armorConfig.maxIronPieces && tier == DepthTier.DEEP) {
                material = "gold"; // fallback
            }
            
            // Create and equip armor piece
            ItemStack armorPiece = createArmorPiece(slot, material);
            mob.setItemSlot(slot, armorPiece);
            
            if ("iron".equals(material)) {
                ironEquipped++;
            }
        }
    }
    
    /**
     * Applies weapon scaling to zombies based on depth tier
     */
    private static void applyWeaponScaling(Zombie zombie, DepthTier tier) {
        WeaponConfig weaponConfig = getWeaponConfig(tier);
        RandomSource rand = zombie.getRandom();
        
        // Check if zombie should get a weapon
        if (rand.nextDouble() < weaponConfig.weaponChance) {
            try {
                String[] weaponTypes = {"sword", "shovel", "pickaxe"};
                String weaponType = weaponTypes[rand.nextInt(weaponTypes.length)];
                String material = selectWeaponMaterial(rand, weaponConfig);
                
                ItemStack weapon = createWeapon(weaponType, material);
                zombie.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            } catch (Throwable ignored) {
                // Fail silently if weapon creation fails
            }
        }
    }
    
    /**
     * Gets armor configuration for a specific depth tier
     */
    private static ArmorConfig getArmorConfig(DepthTier tier) {
        return switch (tier) {
            case DEEP -> new ArmorConfig(
                Config.dseDeepEquipChance,
                Config.dseDeepLeatherWeight,
                Config.dseDeepChainWeight,
                Config.dseDeepIronWeight,
                Config.dseDeepGoldWeight,
                Math.max(0, Config.dseDeepMaxIronPieces)
            );
            case MID -> new ArmorConfig(
                Config.dseMidEquipChance,
                Config.dseMidLeatherWeight,
                Config.dseMidChainWeight,
                Config.dseMidIronWeight,
                Config.dseMidGoldWeight,
                4 // No iron limit for mid tier
            );
            default -> new ArmorConfig(
                Config.dseSurfaceEquipChance,
                Config.dseSurfaceLeatherWeight,
                Config.dseSurfaceChainWeight,
                Config.dseSurfaceIronWeight,
                Config.dseSurfaceGoldWeight,
                4 // No iron limit for surface tier
            );
        };
    }
    
    /**
     * Gets weapon configuration for a specific depth tier
     */
    private static WeaponConfig getWeaponConfig(DepthTier tier) {
        return switch (tier) {
            case DEEP -> new WeaponConfig(
                Config.dseDeepZombieWeaponChance,
                0.10, 0.30, 0.35, 0.25 // wood, stone, gold, iron
            );
            case MID -> new WeaponConfig(
                Config.dseMidZombieWeaponChance,
                0.25, 0.45, 0.20, 0.10
            );
            default -> new WeaponConfig(
                Config.dseSurfaceZombieWeaponChance,
                0.50, 0.45, 0.05, 0.00
            );
        };
    }
    
    /**
     * Selects armor material based on weighted probabilities
     */
    private static String selectArmorMaterial(RandomSource rand, ArmorConfig config) {
        double totalWeight = config.leatherWeight + config.chainWeight + config.ironWeight + config.goldWeight;
        if (totalWeight <= 0.0) totalWeight = 1.0;
        
        double roll = rand.nextDouble() * totalWeight;
        
        if (roll < config.leatherWeight) return "leather";
        if (roll < config.leatherWeight + config.chainWeight) return "chain";
        if (roll < config.leatherWeight + config.chainWeight + config.goldWeight) return "gold";
        return "iron";
    }
    
    /**
     * Selects weapon material based on weighted probabilities
     */
    private static String selectWeaponMaterial(RandomSource rand, WeaponConfig config) {
        double totalWeight = config.woodWeight + config.stoneWeight + config.goldWeight + config.ironWeight;
        if (totalWeight <= 0.0) totalWeight = 1.0;
        
        double roll = rand.nextDouble() * totalWeight;
        
        if (roll < config.woodWeight) return "wood";
        if (roll < config.woodWeight + config.stoneWeight) return "stone";
        if (roll < config.woodWeight + config.stoneWeight + config.goldWeight) return "gold";
        return "iron";
    }
    
    /**
     * Creates an armor piece for the specified slot and material
     */
    private static ItemStack createArmorPiece(EquipmentSlot slot, String material) {
        return switch (slot) {
            case HEAD -> switch (material) {
                case "chain" -> new ItemStack(Items.CHAINMAIL_HELMET);
                case "iron" -> new ItemStack(Items.IRON_HELMET);
                case "gold" -> new ItemStack(Items.GOLDEN_HELMET);
                default -> new ItemStack(Items.LEATHER_HELMET);
            };
            case CHEST -> switch (material) {
                case "chain" -> new ItemStack(Items.CHAINMAIL_CHESTPLATE);
                case "iron" -> new ItemStack(Items.IRON_CHESTPLATE);
                case "gold" -> new ItemStack(Items.GOLDEN_CHESTPLATE);
                default -> new ItemStack(Items.LEATHER_CHESTPLATE);
            };
            case LEGS -> switch (material) {
                case "chain" -> new ItemStack(Items.CHAINMAIL_LEGGINGS);
                case "iron" -> new ItemStack(Items.IRON_LEGGINGS);
                case "gold" -> new ItemStack(Items.GOLDEN_LEGGINGS);
                default -> new ItemStack(Items.LEATHER_LEGGINGS);
            };
            case FEET -> switch (material) {
                case "chain" -> new ItemStack(Items.CHAINMAIL_BOOTS);
                case "iron" -> new ItemStack(Items.IRON_BOOTS);
                case "gold" -> new ItemStack(Items.GOLDEN_BOOTS);
                default -> new ItemStack(Items.LEATHER_BOOTS);
            };
            default -> ItemStack.EMPTY;
        };
    }
    
    /**
     * Creates a weapon of the specified type and material
     */
    private static ItemStack createWeapon(String weaponType, String material) {
        return switch (weaponType) {
            case "shovel" -> switch (material) {
                case "wood" -> new ItemStack(Items.WOODEN_SHOVEL);
                case "stone" -> new ItemStack(Items.STONE_SHOVEL);
                case "gold" -> new ItemStack(Items.GOLDEN_SHOVEL);
                default -> new ItemStack(Items.IRON_SHOVEL);
            };
            case "pickaxe" -> switch (material) {
                case "wood" -> new ItemStack(Items.WOODEN_PICKAXE);
                case "stone" -> new ItemStack(Items.STONE_PICKAXE);
                case "gold" -> new ItemStack(Items.GOLDEN_PICKAXE);
                default -> new ItemStack(Items.IRON_PICKAXE);
            };
            default -> switch (material) { // sword
                case "wood" -> new ItemStack(Items.WOODEN_SWORD);
                case "stone" -> new ItemStack(Items.STONE_SWORD);
                case "gold" -> new ItemStack(Items.GOLDEN_SWORD);
                default -> new ItemStack(Items.IRON_SWORD);
            };
        };
    }
    
    /**
     * Configuration record for armor scaling
     */
    private record ArmorConfig(
        double equipChance,
        double leatherWeight,
        double chainWeight,
        double ironWeight,
        double goldWeight,
        int maxIronPieces
    ) {}
    
    /**
     * Configuration record for weapon scaling
     */
    private record WeaponConfig(
        double weaponChance,
        double woodWeight,
        double stoneWeight,
        double goldWeight,
        double ironWeight
    ) {}
}
