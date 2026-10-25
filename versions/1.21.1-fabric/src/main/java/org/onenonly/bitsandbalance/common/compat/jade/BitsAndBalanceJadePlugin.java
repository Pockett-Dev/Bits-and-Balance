package org.onenonly.bitsandbalance.common.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.util.PickBlockHelperCompat;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.impl.ui.ItemStackElement;

@WailaPlugin
public final class BitsAndBalanceJadePlugin implements IWailaPlugin {
    private static final Component MOD_NAME_BITS_AND_BALANCE = Component.literal("Bits and Balance")
            .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);
    private static final Component MOD_NAME_MINECRAFT = Component.literal("Minecraft")
            .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);

    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        TargetedProvider mixedSlab = new TargetedProvider("mixed_slab_name");
        registration.registerBlockIcon(mixedSlab, MixedSlabBlock.class);
        registration.registerBlockComponent(mixedSlab, MixedSlabBlock.class);

        TargetedProvider verticalSlab = new TargetedProvider("vertical_slab_name");
        registration.registerBlockIcon(verticalSlab, VerticalSlabBlock.class);
        registration.registerBlockComponent(verticalSlab, VerticalSlabBlock.class);

        TargetedProvider step = new TargetedProvider("step_name");
        registration.registerBlockIcon(step, StepBlock.class);
        registration.registerBlockComponent(step, StepBlock.class);

        TargetedProvider quadStep = new TargetedProvider("quad_step_name");
        registration.registerBlockIcon(quadStep, QuadStepBlock.class);
        registration.registerBlockComponent(quadStep, QuadStepBlock.class);

        TargetedProvider verticalStep = new TargetedProvider("vertical_step_name");
        registration.registerBlockIcon(verticalStep, VerticalStepBlock.class);
        registration.registerBlockComponent(verticalStep, VerticalStepBlock.class);

        TargetedProvider quadVerticalStep = new TargetedProvider("quad_vertical_step_name");
        registration.registerBlockIcon(quadVerticalStep, QuadVerticalStepBlock.class);
        registration.registerBlockComponent(quadVerticalStep, QuadVerticalStepBlock.class);
    }

    private static PickBlockHelperCompat.PickTarget bitsandbalance$getTarget(BlockAccessor accessor) {
        try {
            if (accessor == null) return null;
            Level level = accessor.getLevel();
            if (level == null) return null;

            try {
                PickBlockHelperCompat.PickTarget target =
                        PickBlockHelperCompat.resolve(level, accessor.getPlayer(), accessor.getHitResult());
                if (target != null && !target.stack().isEmpty()) return target;
            } catch (Throwable ignored) {
            }

            return PickBlockHelperCompat.resolve(level, accessor.getPlayer(), accessor.getPosition());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ItemStack bitsandbalance$getDisplayedStack(BlockAccessor accessor) {
        PickBlockHelperCompat.PickTarget target = bitsandbalance$getTarget(accessor);
        return target != null && !target.stack().isEmpty() ? target.stack() : ItemStack.EMPTY;
    }

    private static void bitsandbalance$replaceDisplayedBlock(snownee.jade.api.ITooltip tooltip, ItemStack stack) {
        if (tooltip == null || stack == null || stack.isEmpty()) return;

        Component name = stack.getHoverName().copy().withStyle(ChatFormatting.WHITE);
        if (!tooltip.replace(JadeIds.CORE_OBJECT_NAME, name)) {
            tooltip.add(0, name);
        }

        Component modName = bitsandbalance$getDisplayedModName(stack);
        if (modName != null) {
            bitsandbalance$replaceCoreModName(tooltip, modName);
        }
    }

    private static Component bitsandbalance$getDisplayedModName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return null;

        if ("minecraft".equals(id.getNamespace())) return MOD_NAME_MINECRAFT;
        if ("bitsandbalance".equals(id.getNamespace())) return MOD_NAME_BITS_AND_BALANCE;

        return Component.literal(bitsandbalance$humanizeNamespace(id.getNamespace()))
                .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);
    }

    private static String bitsandbalance$humanizeNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) return "";

        String[] parts = namespace.replace('-', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                result.append(part.substring(1));
            }
        }
        return result.isEmpty() ? namespace : result.toString();
    }

    private static void bitsandbalance$replaceCoreModName(snownee.jade.api.ITooltip tooltip, Component modName) {
        if (tooltip == null || modName == null) return;
        boolean replaced = tooltip.replace(JadeIds.CORE_MOD_NAME, modName);
        if (!replaced) {
            tooltip.add(modName, JadeIds.CORE_MOD_NAME);
        }
    }

    private static final class TargetedProvider implements IComponentProvider<BlockAccessor> {
        private final ResourceLocation uid;

        private TargetedProvider(String path) {
            this.uid = ResourceLocation.fromNamespaceAndPath("bitsandbalance", path);
        }

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public int getDefaultPriority() {
            return 12000;
        }

        @Override
        public void appendTooltip(snownee.jade.api.ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ItemStack stack = bitsandbalance$getDisplayedStack(accessor);
            if (stack.isEmpty()) return;

            bitsandbalance$replaceDisplayedBlock(tooltip, stack);
        }

        @Override
        public IElement getIcon(BlockAccessor accessor, IPluginConfig config, IElement currentIcon) {
            ItemStack stack = bitsandbalance$getDisplayedStack(accessor);
            if (stack.isEmpty()) return currentIcon;
            return ItemStackElement.of(stack);
        }
    }
}
