package org.onenonly.bitsandbalance;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.resources.ResourceKey; // Used in registry access with fully qualified names
import net.minecraft.resources.Identifier;
import net.minecraft.core.Holder; // Used in potion handling with fully qualified names
import net.minecraft.world.item.alchemy.PotionBrewing; // Used in reflection with fully qualified names
import net.minecraft.world.item.alchemy.Potions;
import org.slf4j.Logger;
import org.onenonly.bitsandbalance.potions.CustomBrewing;
import org.onenonly.bitsandbalance.compat.BetterCombatCompat;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.common.content.ModDogMusicDisc;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;
import org.onenonly.bitsandbalance.common.content.ModRawQuartz;
import org.onenonly.bitsandbalance.recipe.ModConditions;
import org.onenonly.bitsandbalance.registry.NeoForgeContentRegistrar;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(BitsAndBalance.MODID)
public class BitsAndBalance {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "bitsandbalance";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "bitsandbalance" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "bitsandbalance" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "bitsandbalance" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Server-authoritative list of players currently sitting
    public static final java.util.Set<java.util.UUID> SITTING_PLAYERS = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    // Optional mirror set kept on client via SyncSitPayload; server does not use this
    public static final java.util.Set<java.util.UUID> SYNCED_SITTING_PLAYERS = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public BitsAndBalance(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Mod lifecycle + datagen listeners (EventBusSubscriber no longer supports bus=).
        modEventBus.addListener(org.onenonly.bitsandbalance.setup.CommonSetup::onCommonSetup);
        modEventBus.addListener(org.onenonly.bitsandbalance.tweaks.NeoForgeGeneratedServerDataPacks::onAddPackFinders);
        
        // Entity attribute modification (must happen on MOD bus during setup)
        modEventBus.addListener(this::onEntityAttributeModification);

        // Gameplay event listeners (NeoForge bus)
        NeoForge.EVENT_BUS.addListener(org.onenonly.bitsandbalance.setup.CommonSetup::onBlockToolModification);
        NeoForge.EVENT_BUS.addListener(org.onenonly.bitsandbalance.tweaks.NeoForgeGeneratedServerDataPacks::onServerAboutToStart);

        // Client-only MOD bus listeners (keybinds, renderers, client setup).
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modEventBus.addListener(org.onenonly.bitsandbalance.client.ClientEvents::onClientSetup);
            modEventBus.addListener(org.onenonly.bitsandbalance.client.ClientEvents::onRegisterClientExtensions);
            modEventBus.addListener(org.onenonly.bitsandbalance.client.ClientEvents::onModifyBakingResult);
            modEventBus.addListener(org.onenonly.bitsandbalance.client.ClientEvents::onRegisterRenderPipelines);
            modEventBus.addListener(org.onenonly.bitsandbalance.client.ClientEvents::onRegisterRenderers);
            NeoForge.EVENT_BUS.addListener(org.onenonly.bitsandbalance.client.ClientEvents::onAfterTranslucentBlocks);
            modEventBus.addListener(org.onenonly.bitsandbalance.client.ClientEvents::onRegisterKeys);
            modEventBus.addListener(org.onenonly.bitsandbalance.setup.ClientSetup::onClientSetup);
            modEventBus.addListener(org.onenonly.bitsandbalance.setup.ClientSetup::registerEntityRenderers);
        }

        // Register custom wood types (must be done early, before blocks)
        org.onenonly.bitsandbalance.registry.ModWoodTypes.register();
        
        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Inject vertical slabs into vanilla creative tabs (after their source slab).
        modEventBus.addListener(org.onenonly.bitsandbalance.registry.NeoForgeVerticalSlabCreativeTabs::onBuildCreativeModeTabContents);
        modEventBus.addListener(org.onenonly.bitsandbalance.registry.NeoForgeMusicDiscCreativeTabs::onBuildCreativeModeTabContents);
        // Register entity types (for boats)
        org.onenonly.bitsandbalance.registry.ModEntityTypes.register(modEventBus);
        // Register block entities (for signs)
        org.onenonly.bitsandbalance.registry.ModBlockEntities.register(modEventBus);
        // Initialize Bits and Balance category (creative tab)
        org.onenonly.bitsandbalance.blocks.CreativeCategory.register();
        // Register effects
        ModEffects.register(modEventBus);
        // Register potions
        ModPotions.register(modEventBus);
        // Register attributes
        org.onenonly.bitsandbalance.registry.ModAttributes.register(modEventBus);
        // Register custom blocks
        ModBlocks.register(modEventBus);
        // Register ore variant blocks
        org.onenonly.bitsandbalance.blocks.OreVariants.register(modEventBus);
        // Register custom items
        ModItems.register(modEventBus);

        // Register common cross-loader content (minimal set for now)
        NeoForgeContentRegistrar commonRegistrar = new NeoForgeContentRegistrar(BLOCKS, ITEMS, org.onenonly.bitsandbalance.registry.ModEntityTypes.ENTITY_TYPES);
        ModRawQuartz.register(commonRegistrar);
        ModBottleOfCloud.register(commonRegistrar);
        ModGlowGoo.register(commonRegistrar);
        ModDogMusicDisc.register(commonRegistrar);
        // Register loot modifiers
        org.onenonly.bitsandbalance.loot.ModLootModifiers.register(modEventBus);
        // Register recipe serializers
        org.onenonly.bitsandbalance.recipe.ModRecipeSerializers.register(modEventBus);
        ModConditions.register(modEventBus);
        org.onenonly.bitsandbalance.tweaks.NeoForgeGeneratedServerDataPacks.initializeDirectories();
        // Register custom placement modifiers (worldgen config filters)
        org.onenonly.bitsandbalance.registry.ModPlacementModifiers.register(modEventBus);
        // Register custom features (noodle vein feature)
        org.onenonly.bitsandbalance.registry.ModFeatures.register(modEventBus);
        // Note: No custom entities needed anymore - using vanilla tipped arrows

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (Rebalance) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        
        // DISABLED: Azalea tree chunk processor causes infinite loop
        // NeoForge.EVENT_BUS.register(org.onenonly.bitsandbalance.worldgen.AzaleaTreeChunkProcessor.class);

        // Register spawn swap handler for cave spider replacement
        NeoForge.EVENT_BUS.addListener(org.onenonly.bitsandbalance.mobs.SpawnSwapHandler::onFinalizeSpawn);

        // Register spawn handler for disabling nitwits
        NeoForge.EVENT_BUS.addListener(org.onenonly.bitsandbalance.mobs.VillagerDisableNitwitHandler::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(org.onenonly.bitsandbalance.mobs.VillagerDisableNitwitHandler::onEntityJoinLevel);


        // Register our mod's ModConfigSpecs so that FML can create and load the config files for us under a dedicated folder
        NeoForgeGroupedConfigMigration.migrateIfNeeded();

        modContainer.registerConfig(ModConfig.Type.COMMON, org.onenonly.bitsandbalance.GameplayConfig.SPEC, "Bits and Balance/gameplay.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, org.onenonly.bitsandbalance.ContentConfig.SPEC, "Bits and Balance/content.toml");
        // Client config (client-only settings)
        modContainer.registerConfig(ModConfig.Type.CLIENT, org.onenonly.bitsandbalance.ClientConfig.SPEC, "Bits and Balance/client.toml");
        // World config (world generation and feature toggles)
        modContainer.registerConfig(ModConfig.Type.COMMON, org.onenonly.bitsandbalance.WorldConfig.SPEC, "Bits and Balance/worldgen.toml");
    }

    /**
     * Add custom attributes to entity types.
     * This is necessary for custom attributes to be accessible on entities.
     */
    private void onEntityAttributeModification(net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent event) {
        // Add aerodynamic_drag attribute to all living entities
        // This allows any entity with elytra to benefit from the Aerodynamic enchantment
        event.add(net.minecraft.world.entity.EntityType.PLAYER, org.onenonly.bitsandbalance.registry.ModAttributes.AERODYNAMIC_DRAG);
        
        LOGGER.debug("[{}] Added aerodynamic_drag attribute to Player entity type", MODID);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        // Initialize Better Combat compatibility if the mod is present
        BetterCombatCompat.initializeCompatibility();

        // Try to register our brewing mixes reflectively (covers MC 1.21.1 API variants)
        event.enqueueWork(() -> {
            // Ensure default custom brewing rules are available at startup (datapack reload can override later)
            try { CustomBrewing.loadDefaults(); } catch (Throwable ignored) {}
            
        // Register custom brewing recipes with Minecraft's brewing system for JEI compatibility
        LOGGER.info("[{}] Attempting to register brewing recipes...", MODID);
        try { 
            org.onenonly.bitsandbalance.potions.BrewingRecipeRegistrar.registerWithMinecraftBrewing(); 
        } catch (Throwable e) {
            LOGGER.error("[{}] Failed to register brewing recipes: {}", MODID, e.getMessage(), e);
        }
        
        // (Intentionally not logging the full brewing rule list; it's very noisy for end users.)
            
            if (!Config.enableWitheringPotion) return;
            try {
                var witheringRL = Identifier.parse(MODID + ":withering");
                var strongRL = Identifier.parse(MODID + ":strong_withering");
                var witheringHolderOpt = BuiltInRegistries.POTION.get(witheringRL);
                var strongHolderOpt = BuiltInRegistries.POTION.get(strongRL);
                if (witheringHolderOpt.isEmpty() || strongHolderOpt.isEmpty()) {
                    LOGGER.warn("[{}] Withering potions not found in registry; skipping mix registration.", MODID);
                    return;
                }
                var witheringHolder = witheringHolderOpt.get();
                var strongHolder = strongHolderOpt.get();
                var awkwardPotion = Potions.AWKWARD;
                var witheringPotion = witheringHolder.value();
                var strongWitheringPotion = strongHolder.value();

                int registered = 0;
                for (var m : PotionBrewing.class.getDeclaredMethods()) {
                    var p = m.getParameterTypes();
                    if (p.length != 3) continue;
                    try {
                        m.setAccessible(true);
                        boolean p0Potion = p[0].getName().equals("net.minecraft.world.item.alchemy.Potion");
                        boolean p2Potion = p[2].getName().equals("net.minecraft.world.item.alchemy.Potion");
                        boolean p0Holder = p[0].getName().equals(Holder.class.getName());
                        boolean p2Holder = p[2].getName().equals(Holder.class.getName());
                        // Accept both Item and ItemLike for reagent parameter
                        boolean p1ItemOrLike = p[1].getName().equals("net.minecraft.world.item.Item") || p[1].getName().equals("net.minecraft.world.level.ItemLike") || p[1].isAssignableFrom(net.minecraft.world.item.Item.class);

                        if (p0Potion && p1ItemOrLike && p2Potion) {
                            // (Potion, ItemLike, Potion)
                            m.invoke(null, awkwardPotion, Items.WITHER_ROSE, witheringPotion);
                            m.invoke(null, witheringPotion, Items.GLOWSTONE_DUST, strongWitheringPotion);
                            registered += 2;
                            LOGGER.debug("[{}] Registered Withering mixes reflectively using signature (Potion, ItemLike, Potion)", MODID);
                            break;
                        } else if (p0Holder && p1ItemOrLike && p2Holder) {
                            // (Holder<Potion>, ItemLike, Holder<Potion>)
                            m.invoke(null, Holder.direct(awkwardPotion), Items.WITHER_ROSE, witheringHolder);
                            m.invoke(null, witheringHolder, Items.GLOWSTONE_DUST, strongHolder);
                            registered += 2;
                            LOGGER.debug("[{}] Registered Withering mixes reflectively using signature (Holder<Potion>, ItemLike, Holder<Potion>)", MODID);
                            break;
                        }
                    } catch (Throwable ignore) {
                        // try next overload
                    }
                }
                if (registered == 0) {
                    LOGGER.debug("[{}] Reflective brewing registration not available; using data-driven potion_mixing only (expected on 1.21+).", MODID);
                }
            } catch (Throwable t) {
                LOGGER.warn("[{}] Failed to register Withering mixes reflectively: {}", MODID, t.toString());
            }

            // Diagnostics removed for production build.
        });
    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

        // Diagnostics removed for production build.

    }
}
