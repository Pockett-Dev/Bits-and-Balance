# Feature List

## Blocks & World Generation
- **Azalea Woodset** - Full azalea wood set (blocks + items) with recipes/loot tables and tag integration; optionally swaps naturally generated + sapling-grown azalea trees to generate with azalea logs instead of vanilla oak (configurable)
- **Dog Music Disc** - Adds the Dog music disc with the C418 - Dog track; it appears with the other music discs in creative menus, shows up in the Bits and Balance creative tab, drops from the vanilla creeper music-disc pool, and can generate in simple dungeons, woodland mansions, and ancient cities
- **Coal Veins (Mojang-Style / OreVeinifier)** - Large multi-chunk coal vein generation with two variants (independently toggleable):
  - **Coal + Andesite** - Overworld coal veins in andesite from **Y 24 to 90**
  - **Coal + Tuff** - Deep underground coal+tuff veins (tuff + deepslate coal ore with occasional coal block pockets)
- **Nether Gold Veins** - Mojang-style nether “vein” deposits (noise-driven) with configurable per-chunk rarity; generates from **Y 8 to 100** and can include rare raw gold blocks depending on settings (new chunks only)
- **Nether Quartz Veins** - Mojang-style nether “vein” deposits (noise-driven) with configurable per-chunk rarity; generates from **Y 8 to 100** (new chunks only)
- **Block of Raw Quartz** - Adds a compact decorative `bitsandbalance:raw_quartz_block` crafted from Nether Quartz (analogous to Raw Iron/Gold blocks); generates as small pockets within Nether Quartz veins (configurable / disableable)
- **Ore Variants** - Adds andesite/diorite/granite/tuff variants for vanilla ores and replaces matching vanilla ore placements during worldgen when they generate against those host stones (includes high-elevation surface ores); also supports Create zinc replacement when Create is installed (configurable)
- **Nether Lava Spring Suppression** - Optional toggle to suppress vanilla random single-source lava springs in Nether terrain generation (new chunks only; off by default)

## Client-Side Features
- **Auto Walk** - Automatic walking functionality with keybind toggle
- **Biome-Tinted Foliage Items** - Biome-tinted foliage/grass items in-hand and in-inventory match the tint they would have when placed
- **Biome Titles** - Display a title/subtitle when entering a new biome with configurable timing, position/scale, and ignore list
- **Chat Heads** – Player head icons render before chat messages with configurable sizing and positioning
- **Custom Splash Text** - Custom splash texts randomly appear on the title screen with configurable frequency
- **Elder Guardian Appearance Suppression** - Disable the Elder Guardian jumpscare effect when Mining Fatigue is inflicted, with a separate toggle to keep or silence the normal Mining Fatigue sound
- **Leaf Litter Biome Tint** - Tint leaf litter and leaf pile blocks using biome colors (supports biome tags like `#minecraft:is_overworld`)
- **Level 30 Old Sound** - Plays the long/old level-up sound when reaching level 30
- **Log Filtering** - Optional client-side log filtering to suppress specific worldgen-related error spam (for example "setBlock in a far chunk")
- **Night Vision Fade** - Smooth night vision transitions instead of vanilla blinking, while infinite-duration Night Vision stays steady at full strength
- **Recovery Compass** - Distance/coordinate display for recovery compass with enhanced UI
- **Screenshots to Clipboard** - After taking an in-game screenshot, automatically copy the saved screenshot image to the system clipboard and show a client-only chat message confirming it was copied (configurable)
- **See Held Item When Riding** - Show your held item in first-person while riding entities
- **Soul Fire Visuals** - When burning from Soul Fire / Soul Campfire, render the on-screen fire overlay and burning-entity flames with the blue soul-fire sprites; lit candles on soul sand or soul soil can also use small blue soul-fire flames with their own client toggle
- **Suppress Experimental Settings Warning** - Suppress experimental settings warnings when creating/loading worlds
- **Usage Ticker** - Visual item usage progress for held items only, with mirrored offhand support and configurable positioning

## Combat Features
- **Maintain Experience** - Players lose percentage of XP on death instead of most (configurable percentage)
- **Second Chance** - Anti-one-shot mechanic with cooldown, resistance, and nausea effects (excludes fall damage by default)
- **Snowball Rework** - Snowballs deal damage and apply freezing effects with a special Nether entity damage multiplier, without leaving hit mobs stuck with powder-snow movement behavior
- **Source Dependent Invulnerability Frames** - Allow damage within i-frames if the incoming damage type differs (with configurable blacklist)

## Enchantments
- **Aerodynamic** - Treasure enchantment for Elytra that reduces horizontal drag based on altitude; caps max flight speed
  - **Loot Source**: Rarely found as an enchanted book in End City Treasure loot (~1/100)
  - **Implementation**: Data-driven enchantment (`data/bitsandbalance/enchantment/aerodynamic.json`) + elytra flight physics mixin

## Custom Items, Potions & Effects
- **Glow Goo** - Throwable blob crafted from glow ink that splats into Goo Splatter: a bright, waterloggable, face-hugging light source with 4-way placement rotation, ambient glow particles, underwater-friendly projectile travel, and separate toggles for the 2-ink and 4-ink recipes
- **Glowing Potion** - Brewable from Glow Berries, with long-duration, splash, lingering, and tipped-arrow variants
- **Bioluminescence Potion** - Brewable from Glow Goo; makes entities emit light while active, with long-duration, splash, lingering, and tipped-arrow variants
- **Displacement Potion** - Random teleportation within 100-block radius (Recipe: Awkward Potion + Ender Eye)
- **Haste Potion** - Brewable haste effect (Recipe: Speed Potion + Quartz, upgradeable with Redstone/Glowstone)
- **Levitation Potion** - Brewable levitation effect (Recipe: Slow Falling Potion + Shulker Shell, upgradeable with Redstone/Glowstone)
- **Resurfacing Potion** - Teleports player to surface (currently no brewing recipe)
- **Returning Potion** - Teleports to latest death location (Recipe: Displacement Potion + Echo Shard)
- **Withering Potion** - Brewable wither effect potion (Recipe: Poison/Harming Potion + Wither Rose, upgradeable with Redstone/Glowstone)

## Loot Table Modifications
- **Custom Loot Tables** - Add custom potions to various structure chests:
  - **Mineshafts** - Resurfacing, Returning, Haste (regular and strong)
  - **Strongholds** - Resurfacing, Returning
  - **Ancient Cities** - Resurfacing, Returning
  - **End Cities** - Returning
  - **Simple Dungeons** - (various custom potions)
  - **Trial Chambers** - (various custom potions)

## Mechanics Features
- **Campfires Ignite** - Campfires cook entities standing on them
- **Chat Mentions** - Highlight and optionally ping on name mentions in chat, plus @mention input highlighting, username tab completion, and live suggestion overlays while typing
- **Cozy Campfire** - Regeneration effect near lit campfires with configurable range, duration, and amplifier (affects bees too)
- **Coyote Time Jump** - Jump after falling off blocks without jumping first with configurable duration and fall distance
- **Crawling** - Explicit crawling stance mechanic with keybind integration
- **Dismount Entities** - Crouch+right-click to eject vehicle passengers; ejection is forward-biased and collision-safe with configurable velocity
- **Door Knocking** - Knock on doors with left-click, plays material-appropriate sound
- **Friendly Fire Friendlies (Tamed Pets)** - Prevents accidentally melee-attacking your own tamed pets
- **Item Sharing** - Shift+chat while hovering items to share in chat with hoverable links
  - Shared items preserve enchantment glint when applicable
- **Leashed Teleport** - Leashed mobs follow through all teleportation (commands, chorus fruit, ender pearls, portals) with cross-dimension support, safe placement, and configurable limits
- **Navigator Compass** - Right-click compass to set custom coordinates with distance and coordinate display
- **Pistons Move Tile Entities** - Allow pistons to push and pull supported block entities such as chests and furnaces while preserving their stored data (configurable)
- **Quick Harvesting** - Right-click harvest that behaves like breaking with your current tool (enchant/loot-context compatible):
  - **Hoes** - 3x3 area harvest for mature crops; ignores melon/pumpkin stems/attached stems
  - **Axes** - Harvest pumpkins, melons, cocoa, and compatible modded crops
  - **Drops** - Quick-harvested drops can briefly home back to the player, with a config toggle to disable the homing behavior
- **Rapid Fire Jump** - Hold jump key for continuous jumping within 2-block tall gaps with configurable interval
- **Respawn Anchor Anywhere** - Use Respawn Anchors in any dimension, not just the Nether
- **Sitting** - Toggle or hold-to-sit with synced third-person visuals (proper seated pose + grounded render offset)
- **Speedy Wolves** - Tamed wolves move faster to keep up with owner; reliably reapplies speed scaling on NeoForge
- **Toggle Stance** - Keybind to switch between standing/sneaking/crawling with full stance integration
- **Villagers Follow Emeralds** - Villagers follow players holding emeralds/emerald blocks with configurable speed and particle effects, and stay focused on the active lure instead of immediately snapping back toward workstation pathing

## Mob Modifications
- **Cave Spider Spawning** - Cave spiders have a configurable chance to replace spiders in caves
- **Depth Scaling Enemies** - Depth-based scaling for hostile mob equipment spawns; chestplates are the most common piece, surface-tier weights favor leather, copper gear is included in the equipment pools, and each tier (surface/mid/deep) can be toggled independently. Surface tier is disabled by default.
- **Disable Nitwits** - Prevent nitwit villagers from spawning
- **Improved Phantoms** - Multiple enhancements:
  - **Pickup Mechanics** - Phantoms can pick up and carry players with configurable chance and minimum delay
  - **Slowness Stacking** - Stacking slowness effect when hit by phantoms with configurable duration and max level
  - **Double Damage When Carried** - Deal double damage when hitting a phantom while being carried
  - **Carry Counterplay** - Allows damaging the carrying phantom while being carried, with optional hit-triggered forced drops
  - **Carry Recovery** - Optional auto-drop timer and optional Slow Falling on hit-earned carry endings
- **Iron Golems vs Creepers** - Iron Golems target Creepers; Creepers won’t retaliate/ignite when attacked by Iron Golems (prevents explosions)

## Recipe Modifications
- **Alternative Map Recipe** - Craft maps using paper and black dye (ink sac) instead of compass
- **Alternative Repeater Recipe** - Craft repeaters with redstone, sticks, and stone
- **Chest from Logs** - Craft chests from logs (4 chests per recipe)
- **Dynamic Stair Recipe System** – Override ALL stair recipes (vanilla, modded, custom) to output 8x stairs plus compact 2x2 stair recipes (3 blocks in corner = 4 stairs) for all stair types
- **Ender Eye Recipe** - Alternative ender eye crafting using echo shard, wind charge, blaze powder, and ender pearl
- **Raw Block Smelting** - Smelt/blast raw iron/gold/copper blocks into their respective metal blocks
- **Recovery Compass Recipe** - Improved recipe using echo shards and compass
- **Steps** - Craft steps from **2 slabs in a horizontal row** to output **4 steps**; matching placements can merge into a quad-step block for up to 4 quarter-block steps in one space; **2 matching steps** convert back into **1 slab**; dyed/tinted variants preserve their color in drops and conversions
- **Stonecutter Depth** - The stonecutter accepts **all wood types** (including modded wood) for dynamic shortcut recipes: **Planks** -> 1 stair, 2 slabs, 4 pressure plates, or 4 buttons; **Logs/Wood/Stems** -> 1 stripped variant, 4 planks, 4 stairs, 8 slabs, 16 pressure plates, or 16 buttons. Recipes are generated dynamically at world load.
- **Torch Fuel** - Use torches as furnace fuel with configurable burn time
- **Vertical Steps** - Craft vertical steps from **2 vertical slabs in a vertical column** to output **4 vertical steps**; matching placements can merge into a quad vertical-step block for up to 4 quarter-block vertical steps in one space; **2 matching vertical steps** convert back into **1 vertical slab**; dyed/tinted variants preserve their color in drops and conversions

## Tweaks & Quality of Life
- **Armed Armor Stands** - Armor stands spawn with arms and can hold items
- **Automatic Block Restock** - When you run out of blocks in your selected hotbar slot while building, automatically restock from your inventory; strongly prefers exact matches (custom data / custom names)
- **Automatic Tool Restock** - When a tool breaks in your selected hotbar slot, automatically restock from your inventory; strongly prefers similar enchantments (Fortune/Silk Touch, plus Mending/Unbreaking/Efficiency)
- **Bottle o' Cloud** - Obtain a Bottle o' Cloud by right-clicking a Glass Bottle at cloud height (Y ≥ 192); place it to create Cloud Blocks, and pick those blocks back up again with an empty bottle
- **Brushing XP Rewards** - XP from finishing brushable blocks with configurable min/max values
- **Candle Bundles (Mixed Candles)** - Allows bundling different candle items into the same multi-candle block; each candle keeps its own color visuals and drops back correctly, mixed bundles orient toward the placer, and both regular and soul-fire particles follow the stored bundle orientation (configurable)
- **Compostable Items** - Rotten flesh (30% chance) and poisonous potatoes (65% chance) can be composted
- **Creeper Sunlight Burn** - Creepers burn in sunlight like zombies and skeletons
- **Curse Uses** - Hide pumpkin overlay with Curse of Vanishing on pumpkins
- **Despawn With Master** - Vex despawn with Evoker, Shulker bullets despawn with Shulker
- **Double Door Opening** - Double doors open together with redstone mirroring support, crouch for single door, and chain trapdoors up to 5; same-block pairing is on by default and compatible hand-openable modded trapdoors can join the chain behavior
- **Enhanced Slab Behavior (Mixed Slabs + Slab QoL)** - A bundle of slab improvements (configurable):
  - **Mixed Slabs** - Placing a different slab onto an existing slab forms a mixed slab (instead of a vanilla double slab); each half retains its own block type/textures/sounds and can be mined independently, with per-half mining sounds and piston movement/restoration support
  - **Hang-Below Support** - Top slabs and double slabs provide a sturdy downward face, allowing lanterns, chains, hanging signs, and other hangable items to be placed beneath them
  - **Knee Slab Mining** - Independently mine each half of a vanilla double slab (or mixed slab) in survival mode; mining speed, crack animation, and particles are locked to the targeted half
  - **Material Inheritance** - Generated vertical slabs, steps, vertical steps, and quad variants inherit wood fuel/flammability plus copper waxing, unwaxing, scraping, and oxidation from their source slab families
- **Vertical Slabs (Crafting Recipes)** - Craft and convert vertical slabs (dynamic; applies to supported slab types, including runtime-generated vertical slabs):
  - **3 planks vertically** (crafting table) -> **6 vertical slabs**
  - **1 slab ↔ 1 vertical slab** conversion in any crafting grid
- **Enderdragon Egg Always** - Every Ender Dragon kill spawns a dragon egg with configurable placement (random or vanilla location) and particle effects
- **Glowing Glowberries** - Glowberries give glowing effect when consumed (15 seconds for players, 5 seconds for foxes)
- **Improved Climbing** - Adds chain placement support on climbables and configurable faster climbing up or down on climbable blocks, using separate speed and look-angle thresholds for ascending and descending
- **Improved Recovery Compass** - Keep on death, show distance/coords, right-click to toggle display, and shift-right-click to send a soul-and-soul-fire trail toward your latest death location
- **Keybind Modifier System** – Global Shift/Alt/Ctrl modifier support for mod keybinds (used by Item Sharing)
- **More Mining XP** - Increased XP from mining ores (vanilla + variants), configurable per ore type (coal, diamond, emerald, lapis, redstone)
- **Responsive Shields** - Remove shield blocking delay with configurable raise time (0 = instant blocking)
- **Return to Killer** - Airborne kills tag nearby drops/XP to home toward the killer (15s); item homing speed is capped (5 m/s)
- **Sophisticated Scaffolding** - All drops appear at player location when breaking scaffolding
- **Speedy Happy Ghasts** - Happy Ghasts are affected by Swiftness and Slowness (applied to flying speed).
- **Sugarcane Sand Growth** – Sugarcane grows 25% faster on sand or red sand
- **Tipped Arrows Lingering Clouds** - Tipped arrows spawn a lingering-potion-style area effect cloud immediately on impact (block hit and entity hit); cloud behavior matches lingering potions; afterwards, the arrow becomes a normal arrow
- **Treasure Enchantment Gold Color** - Beneficial treasure enchantments (Mending, Frost Walker, Soul Speed, Swift Sneak) display in gold color instead of purple
- **Unbreakable Trial Spawners** - Trial Spawners are completely indestructible in survival (immune to mining, explosions, and all other damage sources); creative mode can still break them, and the feature can be disabled via config
- **Unbreakable Vaults** - Vaults are completely indestructible in survival (immune to mining, explosions, and all other damage sources); creative mode can still break them, and the feature can be disabled via config

## Balance Changes
- **Balanced Elytra** - Elytras lose extra durability when flying at high speeds (on top of vanilla durability loss)
- **Full Ender Dragon XP** - Ender Dragon drops full XP every time it's killed, not just the first time
- **Food Always Edible** - Allow eating food at full hunger (toggleable)
- **Nerfed Mending** - Increase the XP cost per point of durability repaired by Mending (configurable multiplier)
- **Nerfed Discounts** - Remove zombie curing discounts (optional) and cap maximum villager trade discounts