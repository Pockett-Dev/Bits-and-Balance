package org.onenonly.bitsandbalance.common.compat.jade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import org.onenonly.bitsandbalance.common.blockentity.CandleBundleBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.CandleBundleBlock;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.candle.CandleBundleContentsClientCache;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.JadeIds;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.callback.JadeItemModNameCallback;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.addon.harvest.HarvestToolProvider;
import snownee.jade.impl.ui.ItemStackElement;
import snownee.jade.util.CommonProxy;

import java.util.List;

@WailaPlugin
public final class BitsAndBalanceJadePlugin implements IWailaPlugin {
    private static final Component MOD_NAME_BITS_AND_BALANCE = Component.literal("Bits and Balance")
        .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);
    private static final Component MOD_NAME_MINECRAFT = Component.literal("Minecraft")
        .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);

    private static void bitsandbalance$replaceCoreModName(snownee.jade.api.ITooltip tooltip, Component modName) {
        if (tooltip == null || modName == null) return;
        // Jade core ModNameProvider adds a LayoutElement (TextElement) under CORE_MOD_NAME.
        // Using replace(CORE_MOD_NAME, Component) can leave that original element in place.
        // Replace using the LayoutElement-aware overload to avoid duplicate mod-name lines.
        boolean replaced = tooltip.replace(JadeIds.CORE_MOD_NAME, lines -> List.of(List.of(IThemeHelper.get().modName(modName))));
        if (!replaced) {
            tooltip.add(modName, JadeIds.CORE_MOD_NAME);
        }
        tooltip.remove(JadeIds.CORE_TRANSLATE_MOD_NAME);
    }

    private static BlockState bitsandbalance$getTargetedQuadStepState(BlockAccessor accessor) {
        try {
            if (accessor == null) return null;
            Level level = accessor.getLevel();
            if (level == null) return null;
            BlockPos pos = accessor.getPosition();

            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof QuadStepBlockEntity quad)) return null;

            int idx = QuadStepProvider.bitsandbalance$getTargetedQuadStepIndex(accessor, pos);
            BlockState slab = quad.getSlabAt(idx);
            if (slab != null) return slab;

            for (int i = 0; i < 4; i++) {
                slab = quad.getSlabAt(i);
                if (slab != null) return slab;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static BlockState bitsandbalance$getTargetedQuadVerticalStepState(BlockAccessor accessor) {
        try {
            if (accessor == null) return null;
            Level level = accessor.getLevel();
            if (level == null) return null;
            BlockPos pos = accessor.getPosition();

            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof QuadVerticalStepBlockEntity quad)) return null;

            int idx = QuadVerticalStepProvider.bitsandbalance$getTargetedQuadVerticalStepIndex(accessor, pos);
            BlockState slab = quad.getSlabAt(idx);
            if (slab != null) return slab;

            for (int i = 0; i < 4; i++) {
                slab = quad.getSlabAt(i);
                if (slab != null) return slab;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static BlockState bitsandbalance$getTargetedVerticalSlabState(BlockAccessor accessor) {
        try {
            if (accessor == null) return null;

            Level level = accessor.getLevel();
            if (level == null) return null;

            BlockPos pos = accessor.getPosition();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof VerticalSlabBlock)) return null;

            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof VerticalSlabBlockEntity vbe)) return null;

            Direction facingDir = state.getValue(VerticalSlabBlock.FACING);
            boolean isDouble = state.getValue(VerticalSlabBlock.DOUBLE);

            Direction targeted = facingDir;
            if (isDouble) {
                try {
                    BlockHitResult hit = accessor.getHitResult();
                    if (hit != null) {
                        targeted = VerticalSlabBlock.bitsandbalance$getTargetedHalf(pos, hit.getLocation(), facingDir);
                    }
                } catch (Throwable ignored) {
                }
            }

            BlockState halfState = targeted == facingDir ? vbe.getFacingSlab() : vbe.getOppositeSlab();
            if (halfState != null) return halfState;

            halfState = vbe.getFacingSlab();
            if (halfState != null) return halfState;

            return vbe.getOppositeSlab();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<Element> bitsandbalance$getHarvestToolElements(BlockState state, BlockAccessor accessor, IPluginConfig config) {
        if (state == null || accessor == null || config == null) return List.of();
        if (!state.requiresCorrectToolForDrops() && !config.get(JadeIds.MC_EFFECTIVE_TOOL)) return List.of();

        List<ItemStack> tools = HarvestToolProvider.getTool(state, accessor.getLevel(), accessor.getPosition());
        if (tools.isEmpty()) return List.of();

        int offsetY = -3;
        boolean newLine = config.get(JadeIds.MC_HARVEST_TOOL_NEW_LINE);
        List<Element> elements = new java.util.ArrayList<>();
        for (ItemStack tool : tools) {
            elements.add(JadeUI.item(tool, 0.75f).offset(-1, offsetY).size(10, 0).narration(""));
        }

        elements.add(0, JadeUI.spacer(newLine ? -2 : 5, newLine ? 10 : 0).flexGrow(1000));
        Player player = accessor.getPlayer();
        if (player != null) {
            boolean canHarvest = CommonProxy.isCorrectToolForDrops(state, player, accessor.getLevel(), accessor.getPosition());
            if (state.requiresCorrectToolForDrops() || !canHarvest) {
                IThemeHelper themes = IThemeHelper.get();
                Component text = canHarvest ? themes.success(Component.literal("✔")) : themes.danger(Component.literal("✕"));
                elements.add(JadeUI.text(text)
                        .scale(0.75F)
                        .narration("")
                        .size(0, 0)
                        .offset(-3, 6 + offsetY));
            }
        }

        return elements;
    }

    private static void bitsandbalance$replaceHarvestTool(
            snownee.jade.api.ITooltip tooltip,
            BlockAccessor accessor,
            IPluginConfig config,
            BlockState state
    ) {
        if (tooltip == null) return;

        List<Element> elements = bitsandbalance$getHarvestToolElements(state, accessor, config);
        if (elements.isEmpty()) {
            tooltip.remove(JadeIds.MC_HARVEST_TOOL);
            return;
        }

        tooltip.remove(JadeIds.MC_HARVEST_TOOL);
        if (config.get(JadeIds.MC_HARVEST_TOOL_NEW_LINE)) {
            tooltip.add(elements);
        } else {
            tooltip.append(0, elements);
        }
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockIcon(CandleBundleCycleProvider.INSTANCE, CandleBundleBlock.class);
        registration.registerBlockComponent(CandleBundleCycleProvider.INSTANCE, CandleBundleBlock.class);

        registration.registerBlockIcon(MixedSlabProvider.INSTANCE, MixedSlabBlock.class);
        registration.registerBlockComponent(MixedSlabProvider.INSTANCE, MixedSlabBlock.class);

        registration.registerBlockIcon(VerticalSlabProvider.INSTANCE, VerticalSlabBlock.class);
        registration.registerBlockComponent(VerticalSlabProvider.INSTANCE, VerticalSlabBlock.class);
        registration.registerBlockComponent(VerticalSlabHarvestProvider.INSTANCE, VerticalSlabBlock.class);

        registration.registerBlockIcon(StepProvider.INSTANCE, StepBlock.class);
        registration.registerBlockComponent(StepProvider.INSTANCE, StepBlock.class);

        registration.registerBlockIcon(QuadStepProvider.INSTANCE, QuadStepBlock.class);
        registration.registerBlockComponent(QuadStepProvider.INSTANCE, QuadStepBlock.class);
        registration.registerBlockComponent(QuadStepHarvestProvider.INSTANCE, QuadStepBlock.class);

        registration.registerBlockIcon(VerticalStepProvider.INSTANCE, VerticalStepBlock.class);
        registration.registerBlockComponent(VerticalStepProvider.INSTANCE, VerticalStepBlock.class);

        registration.registerBlockIcon(QuadVerticalStepProvider.INSTANCE, QuadVerticalStepBlock.class);
        registration.registerBlockComponent(QuadVerticalStepProvider.INSTANCE, QuadVerticalStepBlock.class);
        registration.registerBlockComponent(QuadVerticalStepHarvestProvider.INSTANCE, QuadVerticalStepBlock.class);

        // Jade also renders its own inventory item-tooltips and decides the "mod name" line itself.
        // Without this hook, dyed vanilla items (minecraft namespace + our tint component) can still show "Minecraft".
        try {
            registration.addItemModNameCallback(10000, (JadeItemModNameCallback) stack -> {
                // Some GUI paths can hand Jade a stack that reports isEmpty() (count=0)
                // but still contains our tint components.
                if (stack == null) return null;
                if (stack.getItem() == Items.AIR) return null;

                Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id == null) return null;

                if ("bitsandbalance".equals(id.getNamespace())) return "Bits and Balance";
                if ("minecraft".equals(id.getNamespace())) return "Minecraft";
                return null;
            });
        } catch (Throwable ignored) {
        }
    }

    /**
     * Mixed slabs store the actual rendered blocks in a block-entity. Jade will otherwise
     * treat this as a single "mixed slab" and show the wrong name/icon (often the bottom).
     *
     * This provider rewrites the displayed name/icon based on which half the player is aiming at.
     */
    private static final class MixedSlabProvider implements IComponentProvider<BlockAccessor> {
        private static final MixedSlabProvider INSTANCE = new MixedSlabProvider();
        private static final Identifier UID = Identifier.fromNamespaceAndPath("bitsandbalance", "mixed_slab_name");

        @Override
        public Identifier getUid() {
            return UID;
        }

        @Override
        public int getDefaultPriority() {
            // Run after core providers.
            return 12000;
        }

        @Override
        public void appendTooltip(snownee.jade.api.ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return;

            Component name = stack.getHoverName().copy().withStyle(ChatFormatting.WHITE);
            if (!tooltip.replace(JadeIds.CORE_OBJECT_NAME, name)) {
                tooltip.add(0, name);
            }

            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null) {
                if ("bitsandbalance".equals(id.getNamespace())) {
                    bitsandbalance$replaceCoreModName(tooltip, MOD_NAME_BITS_AND_BALANCE);
                } else if ("minecraft".equals(id.getNamespace())) {
                    bitsandbalance$replaceCoreModName(tooltip, MOD_NAME_MINECRAFT);
                }
            }
        }

        @Override
        public Element getIcon(BlockAccessor accessor, IPluginConfig config, Element currentIcon) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return currentIcon;
            return ItemStackElement.of(stack);
        }

        private static ItemStack getDisplayedStack(BlockAccessor accessor) {
            try {
                if (accessor == null) return ItemStack.EMPTY;

                Level level = accessor.getLevel();
                if (level == null) return ItemStack.EMPTY;

                BlockPos pos = accessor.getPosition();
                BlockEntity be = level.getBlockEntity(pos);
                if (!(be instanceof MixedSlabBlockEntity mixedBe)) return ItemStack.EMPTY;

                SlabType targetedHalf = null;
                try {
                    BlockHitResult hit = accessor.getHitResult();
                    if (hit != null) {
                        targetedHalf = EnhancedSlabHelper.getTargetedHalf(pos, hit.getLocation());
                    }
                } catch (Throwable ignored) {
                }

                if (targetedHalf == null) {
                    // Fallback: choose based on hit Y or eye Y isn't available in Jade.
                    targetedHalf = SlabType.BOTTOM;
                }

                BlockState halfState = targetedHalf == SlabType.TOP ? mixedBe.getTopSlab() : mixedBe.getBottomSlab();
                if (halfState == null) return ItemStack.EMPTY;
                return new ItemStack(halfState.getBlock().asItem());
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }
    }

    /**
     * Vertical slabs store the actual rendered slabs in a block-entity. Jade will otherwise
     * treat this as a single "vertical slab" and show the wrong name/icon for mixed doubles.
     *
     * This provider rewrites the displayed name/icon based on which half the player is aiming at.
     */
    private static final class VerticalSlabProvider implements IComponentProvider<BlockAccessor> {
        private static final VerticalSlabProvider INSTANCE = new VerticalSlabProvider();
        private static final Identifier UID = Identifier.fromNamespaceAndPath("bitsandbalance", "vertical_slab_name");

        @Override
        public Identifier getUid() {
            return UID;
        }

        @Override
        public int getDefaultPriority() {
            // Run after core providers.
            return 12000;
        }

        @Override
        public void appendTooltip(snownee.jade.api.ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return;

            Component name = stack.getHoverName().copy().withStyle(ChatFormatting.WHITE);
            if (!tooltip.replace(JadeIds.CORE_OBJECT_NAME, name)) {
                tooltip.add(0, name);
            }

            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null) {
                if ("bitsandbalance".equals(id.getNamespace())) {
                    bitsandbalance$replaceCoreModName(tooltip, MOD_NAME_BITS_AND_BALANCE);
                } else if ("minecraft".equals(id.getNamespace())) {
                    bitsandbalance$replaceCoreModName(tooltip, MOD_NAME_MINECRAFT);
                }
            }
        }

        @Override
        public Element getIcon(BlockAccessor accessor, IPluginConfig config, Element currentIcon) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return currentIcon;
            return ItemStackElement.of(stack);
        }

        private static ItemStack getDisplayedStack(BlockAccessor accessor) {
            try {
                BlockState halfState = bitsandbalance$getTargetedVerticalSlabState(accessor);
                if (halfState == null) return ItemStack.EMPTY;

                Block slabBlock = halfState.getBlock();
                Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock);

                ItemStack result;
                if (verticalBlock != null && verticalBlock.asItem() != Items.AIR) {
                    result = new ItemStack(verticalBlock.asItem());
                } else {
                    result = new ItemStack(slabBlock.asItem());
                }

                return result;
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }
    }

    private static final class VerticalSlabHarvestProvider implements IComponentProvider<BlockAccessor> {
        private static final VerticalSlabHarvestProvider INSTANCE = new VerticalSlabHarvestProvider();
        private static final Identifier UID = Identifier.fromNamespaceAndPath("bitsandbalance", "vertical_slab_harvest_tool");

        @Override public Identifier getUid() { return UID; }

        @Override public int getDefaultPriority() { return 12001; }

        @Override
        public void appendTooltip(snownee.jade.api.ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            bitsandbalance$replaceHarvestTool(tooltip, accessor, config, bitsandbalance$getTargetedVerticalSlabState(accessor));
        }
    }

    /**
     * Steps (single block) store the source slab in a block-entity. Jade will otherwise
     * show a generic name. This provider shows the correct step name/icon based on the stored slab.
     */
    private static final class StepProvider implements IComponentProvider<BlockAccessor> {
        private static final StepProvider INSTANCE = new StepProvider();
        private static final Identifier UID = Identifier.fromNamespaceAndPath("bitsandbalance", "step_name");

        @Override public Identifier getUid() { return UID; }

        @Override public int getDefaultPriority() { return 12000; }

        @Override
        public void appendTooltip(snownee.jade.api.ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return;

            Component name = stack.getHoverName().copy().withStyle(ChatFormatting.WHITE);
            if (!tooltip.replace(JadeIds.CORE_OBJECT_NAME, name)) {
                tooltip.add(0, name);
            }
            bitsandbalance$replaceCoreModName(tooltip, MOD_NAME_BITS_AND_BALANCE);
        }

        @Override
        public Element getIcon(BlockAccessor accessor, IPluginConfig config, Element currentIcon) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return currentIcon;
            return ItemStackElement.of(stack);
        }

        private static ItemStack getDisplayedStack(BlockAccessor accessor) {
            try {
                if (accessor == null) return ItemStack.EMPTY;
                Level level = accessor.getLevel();
                if (level == null) return ItemStack.EMPTY;
                BlockPos pos = accessor.getPosition();
                BlockState state = level.getBlockState(pos);

                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof StepBlockEntity sbe) {
                    BlockState slab = sbe.getSlabState();
                    if (slab != null) {
                        return StepBlock.bitsandbalance$makeStepDropItem(slab);
                    }
                }

                Block block = state.getBlock();
                if (block instanceof FixedStepBlock fixed) {
                    return StepBlock.bitsandbalance$makeStepDropItem(fixed.getSourceSlab().defaultBlockState());
                }
                return ItemStack.EMPTY;
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }

    }

    /**
     * Quad steps hold 2-4 different step quadrants. This provider shows the name/icon
     * of whichever quadrant the player is aiming at.
     */
    private static final class QuadStepProvider implements IComponentProvider<BlockAccessor> {
        private static final QuadStepProvider INSTANCE = new QuadStepProvider();
        private static final Identifier UID = Identifier.fromNamespaceAndPath("bitsandbalance", "quad_step_name");

        @Override public Identifier getUid() { return UID; }

        @Override public int getDefaultPriority() { return 12000; }

        @Override
        public void appendTooltip(snownee.jade.api.ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return;

            Component name = stack.getHoverName().copy().withStyle(ChatFormatting.WHITE);
            if (!tooltip.replace(JadeIds.CORE_OBJECT_NAME, name)) {
                tooltip.add(0, name);
            }
            bitsandbalance$replaceCoreModName(tooltip, MOD_NAME_BITS_AND_BALANCE);
        }

        @Override
        public Element getIcon(BlockAccessor accessor, IPluginConfig config, Element currentIcon) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return currentIcon;
            return ItemStackElement.of(stack);
        }

        private static ItemStack getDisplayedStack(BlockAccessor accessor) {
            try {
                BlockState slab = bitsandbalance$getTargetedQuadStepState(accessor);
                if (slab == null) return ItemStack.EMPTY;
                return StepBlock.bitsandbalance$makeStepDropItem(slab);
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }

        private static int bitsandbalance$getTargetedQuadStepIndex(BlockAccessor accessor, BlockPos pos) {
            try {
                if (accessor == null) return QuadStepBlock.IDX_BOTTOM_WEST;
                var player = accessor.getPlayer();
                if (player == null) return QuadStepBlock.IDX_BOTTOM_WEST;

                Direction.Axis axis = Direction.Axis.X;
                try {
                    BlockState bs = accessor.getLevel().getBlockState(pos);
                    if (bs.getBlock() instanceof QuadStepBlock) {
                        axis = bs.getValue(QuadStepBlock.AXIS);
                    }
                } catch (Throwable ignored) {
                }

                return QuadStepBlock.bitsandbalance$getTargetedIndex(player, pos, axis);
            } catch (Throwable ignored) {
            }
            return QuadStepBlock.IDX_BOTTOM_WEST;
        }

    }

    private static final class QuadStepHarvestProvider implements IComponentProvider<BlockAccessor> {
        private static final QuadStepHarvestProvider INSTANCE = new QuadStepHarvestProvider();
        private static final Identifier UID = Identifier.fromNamespaceAndPath("bitsandbalance", "quad_step_harvest_tool");

        @Override public Identifier getUid() { return UID; }

        @Override public int getDefaultPriority() { return 12001; }

        @Override
        public void appendTooltip(snownee.jade.api.ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            bitsandbalance$replaceHarvestTool(tooltip, accessor, config, bitsandbalance$getTargetedQuadStepState(accessor));
        }
    }

    /**
     * Vertical steps (single block) store the source vertical-slab in a block-entity.
     * This provider shows the correct vertical step name/icon.
     */
    private static final class VerticalStepProvider implements IComponentProvider<BlockAccessor> {
        private static final VerticalStepProvider INSTANCE = new VerticalStepProvider();
        private static final Identifier UID = Identifier.fromNamespaceAndPath("bitsandbalance", "vertical_step_name");

        @Override public Identifier getUid() { return UID; }

        @Override public int getDefaultPriority() { return 12000; }

        @Override
        public void appendTooltip(snownee.jade.api.ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return;

            Component name = stack.getHoverName().copy().withStyle(ChatFormatting.WHITE);
            if (!tooltip.replace(JadeIds.CORE_OBJECT_NAME, name)) {
                tooltip.add(0, name);
            }
            bitsandbalance$replaceCoreModName(tooltip, MOD_NAME_BITS_AND_BALANCE);
        }

        @Override
        public Element getIcon(BlockAccessor accessor, IPluginConfig config, Element currentIcon) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return currentIcon;
            return ItemStackElement.of(stack);
        }

        private static ItemStack getDisplayedStack(BlockAccessor accessor) {
            try {
                if (accessor == null) return ItemStack.EMPTY;
                Level level = accessor.getLevel();
                if (level == null) return ItemStack.EMPTY;
                BlockPos pos = accessor.getPosition();
                BlockState state = level.getBlockState(pos);

                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof VerticalStepBlockEntity vsbe) {
                    BlockState slab = vsbe.getSlabState();
                    if (slab != null) {
                        return VerticalStepBlock.bitsandbalance$makeVerticalStepDropItem(slab);
                    }
                }

                Block block = state.getBlock();
                if (block instanceof FixedVerticalStepBlock fixed) {
                    return VerticalStepBlock.bitsandbalance$makeVerticalStepDropItem(fixed.getSourceVerticalSlab().defaultBlockState());
                }
                return ItemStack.EMPTY;
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }

    }

    /**
     * Quad vertical steps hold 2-4 different vertical-step quadrants. This provider shows
     * the name/icon of whichever quadrant the player is aiming at.
     */
    private static final class QuadVerticalStepProvider implements IComponentProvider<BlockAccessor> {
        private static final QuadVerticalStepProvider INSTANCE = new QuadVerticalStepProvider();
        private static final Identifier UID = Identifier.fromNamespaceAndPath("bitsandbalance", "quad_vertical_step_name");

        @Override public Identifier getUid() { return UID; }

        @Override public int getDefaultPriority() { return 12000; }

        @Override
        public void appendTooltip(snownee.jade.api.ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return;

            Component name = stack.getHoverName().copy().withStyle(ChatFormatting.WHITE);
            if (!tooltip.replace(JadeIds.CORE_OBJECT_NAME, name)) {
                tooltip.add(0, name);
            }
            bitsandbalance$replaceCoreModName(tooltip, MOD_NAME_BITS_AND_BALANCE);
        }

        @Override
        public Element getIcon(BlockAccessor accessor, IPluginConfig config, Element currentIcon) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return currentIcon;
            return ItemStackElement.of(stack);
        }

        private static ItemStack getDisplayedStack(BlockAccessor accessor) {
            try {
                BlockState slab = bitsandbalance$getTargetedQuadVerticalStepState(accessor);
                if (slab == null) return ItemStack.EMPTY;
                return VerticalStepBlock.bitsandbalance$makeVerticalStepDropItem(slab);
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }

        private static int bitsandbalance$getTargetedQuadVerticalStepIndex(BlockAccessor accessor, BlockPos pos) {
            try {
                if (accessor == null) return QuadVerticalStepBlock.IDX_NORTH_WEST;
                var player = accessor.getPlayer();
                if (player == null) return QuadVerticalStepBlock.IDX_NORTH_WEST;
                return QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, pos);
            } catch (Throwable ignored) {
            }
            return QuadVerticalStepBlock.IDX_NORTH_WEST;
        }

    }

    private static final class QuadVerticalStepHarvestProvider implements IComponentProvider<BlockAccessor> {
        private static final QuadVerticalStepHarvestProvider INSTANCE = new QuadVerticalStepHarvestProvider();
        private static final Identifier UID = Identifier.fromNamespaceAndPath("bitsandbalance", "quad_vertical_step_harvest_tool");

        @Override public Identifier getUid() { return UID; }

        @Override public int getDefaultPriority() { return 12001; }

        @Override
        public void appendTooltip(snownee.jade.api.ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            bitsandbalance$replaceHarvestTool(tooltip, accessor, config, bitsandbalance$getTargetedQuadVerticalStepState(accessor));
        }
    }

    private static final class CandleBundleCycleProvider implements IComponentProvider<BlockAccessor> {
        private static final CandleBundleCycleProvider INSTANCE = new CandleBundleCycleProvider();
        private static final Identifier UID = Identifier.fromNamespaceAndPath("bitsandbalance", "candle_bundle_cycle");

        @Override
        public Identifier getUid() {
            return UID;
        }

        @Override
        public int getDefaultPriority() {
            // Run after Jade's built-in ModNameProvider (CORE_MOD_NAME).
            // (Jade core uses priority 9999 for ModNameProvider.)
            return 10000;
        }

        @Override
        public void appendTooltip(snownee.jade.api.ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return;

            Component name = stack.getHoverName().copy().withStyle(ChatFormatting.WHITE);
            if (!tooltip.replace(JadeIds.CORE_OBJECT_NAME, name)) {
                tooltip.add(0, name);
            }

            Component modName = getDisplayedModName(stack);
            if (modName != null) {
				bitsandbalance$replaceCoreModName(tooltip, modName);
            }
        }

        @Override
        public Element getIcon(BlockAccessor accessor, IPluginConfig config, Element currentIcon) {
            ItemStack stack = getDisplayedStack(accessor);
            if (stack.isEmpty()) return currentIcon;
            return ItemStackElement.of(stack);
        }

        private static Component getDisplayedModName(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return null;

            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null) return null;

            if ("minecraft".equals(id.getNamespace())) {
                return MOD_NAME_MINECRAFT;
            }

            if ("bitsandbalance".equals(id.getNamespace())) {
                return MOD_NAME_BITS_AND_BALANCE;
            }

            return null;
        }

        private static ItemStack getDisplayedStack(BlockAccessor accessor) {
            if (accessor == null) return ItemStack.EMPTY;

            Level level = accessor.getLevel();
            if (level == null) return ItemStack.EMPTY;

            var be = level.getBlockEntity(accessor.getPosition());
            if (!(be instanceof CandleBundleBlockEntity bundle)) return ItemStack.EMPTY;

            ItemStack[] stacks = new ItemStack[4];
            int count = 0;

            for (int i = 0; i < 4; i++) {
                ItemStack stack = bundle.getCandle(i);
                if (stack == null || stack.isEmpty()) {
                    // Fall back to the network-synced client cache (may arrive before BE data).
                    stack = CandleBundleContentsClientCache.get(accessor.getPosition().asLong(), i);
                }
                if (stack != null && !stack.isEmpty()) {
                    stacks[count++] = stack.copyWithCount(1);
                }
            }

            if (count == 0) {
                // Avoid letting Jade fall back to accessor.getPickedResult (which is randomized).
                return new ItemStack(Items.CANDLE);
            }

            long seconds = level.getGameTime() / 20L;
            long posHash = accessor.getPosition().asLong();
            int offset = (int) (posHash ^ (posHash >>> 32));
            offset = offset & 0x7fffffff;
            int index = (int) ((seconds + offset) % count);
            return stacks[index];
        }
    }
}
