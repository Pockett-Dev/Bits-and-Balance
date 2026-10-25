package org.onenonly.bitsandbalance.fabric.mobs;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
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
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;

/**
 * Fabric port of NeoForge's Depth Scaling Enemies system.
 */
public final class FabricDepthScalingEnemies {
    private enum DepthTier { SURFACE, MID, DEEP }

    private FabricDepthScalingEnemies() {
    }

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!FabricBalanceConfig.dseEnabled) return;

            // Only apply to zombies and skeletons
            if (!(entity instanceof Zombie || entity instanceof Skeleton)) return;

            // Skip entities loaded from disk/worldgen pass to avoid deadlocks during chunk promotion
            if (entity.tickCount > 0) return;

            if (!(world instanceof ServerLevel level)) return;
            if (!(entity instanceof Mob mob)) return;

            try {
                level.getServer().execute(() -> {
                    try {
                        if (!mob.isAlive() || mob.isRemoved()) return;

                        // Don't force-load chunks
                        var chunkSource = level.getChunkSource();
                        if (chunkSource.getChunkNow(mob.chunkPosition().x, mob.chunkPosition().z) == null) {
                            return;
                        }

                        DepthTier tier = getDepthTier(mob.blockPosition(), level);
                        if (!isTierEnabled(tier)) return;
                        applyDepthScaling(mob, tier);
                    } catch (Throwable ignored) {
                    }
                });
            } catch (Throwable ignored) {
            }
        });
    }

    private static boolean isTierEnabled(DepthTier tier) {
        return switch (tier) {
            case SURFACE -> FabricBalanceConfig.dseEnableSurfaceTier;
            case MID -> FabricBalanceConfig.dseEnableMidTier;
            case DEEP -> FabricBalanceConfig.dseEnableDeepTier;
        };
    }

    private static DepthTier getDepthTier(BlockPos pos, ServerLevel level) {
        if (FabricBalanceConfig.dseOverworldOnly && !level.dimension().equals(Level.OVERWORLD)) {
            return DepthTier.SURFACE;
        }

        int y = pos.getY();
        int mid = FabricBalanceConfig.dseMidDepthY;
        int deep = FabricBalanceConfig.dseDeepDepthY;

        if (y <= deep) return DepthTier.DEEP;
        if (y <= mid) return DepthTier.MID;
        return DepthTier.SURFACE;
    }

    private static void applyDepthScaling(Mob mob, DepthTier tier) {
        applyArmorScaling(mob, tier);

        if (mob instanceof Zombie zombie) {
            applyWeaponScaling(zombie, tier);
        }
    }

    private static void applyArmorScaling(Mob mob, DepthTier tier) {
        ArmorConfig armorConfig = getArmorConfig(tier);

        int ironEquipped = 0;
        RandomSource rand = mob.getRandom();

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {

            double slotMultiplier = switch (slot) {
                case CHEST -> 1.40D;
                case LEGS -> 1.00D;
                case HEAD, FEET -> 0.90D;
                default -> 1.00D;
            };

            double slotEquipChance = Math.min(1.0D, armorConfig.equipChance * slotMultiplier);

            if (rand.nextDouble() >= slotEquipChance) continue;
            if (tier == DepthTier.DEEP && ironEquipped >= armorConfig.maxIronPieces) continue;

            String material = selectArmorMaterial(rand, armorConfig);

            if (tier == DepthTier.DEEP && "iron".equals(material) && ironEquipped >= armorConfig.maxIronPieces) {
                material = "gold";
            }

            ItemStack armorPiece = createArmorPiece(slot, material);
            mob.setItemSlot(slot, armorPiece);

            if ("iron".equals(material)) {
                ironEquipped++;
            }
        }
    }

    private static void applyWeaponScaling(Zombie zombie, DepthTier tier) {
        WeaponConfig weaponConfig = getWeaponConfig(tier);
        RandomSource rand = zombie.getRandom();

        if (rand.nextDouble() < weaponConfig.weaponChance) {
            String[] weaponTypes = {"sword", "shovel", "pickaxe"};
            String weaponType = weaponTypes[rand.nextInt(weaponTypes.length)];
            String material = selectWeaponMaterial(rand, weaponConfig);

            ItemStack weapon = createWeapon(weaponType, material);
            zombie.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        }
    }

    private static ArmorConfig getArmorConfig(DepthTier tier) {
        return switch (tier) {
            case DEEP -> new ArmorConfig(
                    FabricBalanceConfig.dseDeepEquipChance,
                    FabricBalanceConfig.dseDeepLeatherWeight,
                    FabricBalanceConfig.dseDeepChainWeight,
                    FabricBalanceConfig.dseDeepIronWeight,
                    FabricBalanceConfig.dseDeepGoldWeight,
                    Math.max(0, FabricBalanceConfig.dseDeepMaxIronPieces)
            );
            case MID -> new ArmorConfig(
                    FabricBalanceConfig.dseMidEquipChance,
                    FabricBalanceConfig.dseMidLeatherWeight,
                    FabricBalanceConfig.dseMidChainWeight,
                    FabricBalanceConfig.dseMidIronWeight,
                    FabricBalanceConfig.dseMidGoldWeight,
                    4
            );
            default -> new ArmorConfig(
                    FabricBalanceConfig.dseSurfaceEquipChance,
                    FabricBalanceConfig.dseSurfaceLeatherWeight,
                    FabricBalanceConfig.dseSurfaceChainWeight,
                    FabricBalanceConfig.dseSurfaceIronWeight,
                    FabricBalanceConfig.dseSurfaceGoldWeight,
                    4
            );
        };
    }

    private static WeaponConfig getWeaponConfig(DepthTier tier) {
        return switch (tier) {
            case DEEP -> new WeaponConfig(
                    FabricBalanceConfig.dseDeepZombieWeaponChance,
                    0.10D, 0.30D, 0.35D, 0.25D
            );
            case MID -> new WeaponConfig(
                    FabricBalanceConfig.dseMidZombieWeaponChance,
                    0.25D, 0.45D, 0.20D, 0.10D
            );
            default -> new WeaponConfig(
                    FabricBalanceConfig.dseSurfaceZombieWeaponChance,
                    0.50D, 0.45D, 0.05D, 0.00D
            );
        };
    }

    private static String selectArmorMaterial(RandomSource rand, ArmorConfig config) {
        double totalWeight = config.leatherWeight + config.chainWeight + config.ironWeight + config.goldWeight;
        if (totalWeight <= 0.0D) totalWeight = 1.0D;

        double roll = rand.nextDouble() * totalWeight;

        if (roll < config.leatherWeight) return "leather";
        if (roll < config.leatherWeight + config.chainWeight) return "chain";
        if (roll < config.leatherWeight + config.chainWeight + config.goldWeight) return "gold";
        return "iron";
    }

    private static String selectWeaponMaterial(RandomSource rand, WeaponConfig config) {
        double totalWeight = config.woodWeight + config.stoneWeight + config.goldWeight + config.ironWeight;
        if (totalWeight <= 0.0D) totalWeight = 1.0D;

        double roll = rand.nextDouble() * totalWeight;

        if (roll < config.woodWeight) return "wood";
        if (roll < config.woodWeight + config.stoneWeight) return "stone";
        if (roll < config.woodWeight + config.stoneWeight + config.goldWeight) return "gold";
        return "iron";
    }

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
            default -> switch (material) {
                case "wood" -> new ItemStack(Items.WOODEN_SWORD);
                case "stone" -> new ItemStack(Items.STONE_SWORD);
                case "gold" -> new ItemStack(Items.GOLDEN_SWORD);
                default -> new ItemStack(Items.IRON_SWORD);
            };
        };
    }

    private record ArmorConfig(
            double equipChance,
            double leatherWeight,
            double chainWeight,
            double ironWeight,
            double goldWeight,
            int maxIronPieces
    ) {
    }

    private record WeaponConfig(
            double weaponChance,
            double woodWeight,
            double stoneWeight,
            double goldWeight,
            double ironWeight
    ) {
    }
}
