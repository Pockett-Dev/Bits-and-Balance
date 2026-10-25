package org.onenonly.bitsandbalance.fabric.config.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FabricModMenuConfigScreen extends Screen {
    private static final int LABEL_LEFT = 12;
    private static final int TAB_TOP = 0;
    private static final int TAB_HEIGHT = 24;
    private static final int ROW_HEIGHT = 32;
    private static final int VALUE_WIDTH = 150;
    private static final int RESET_WIDTH = 24;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int BOTTOM_MESSAGE_SPACE = 24;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DESCRIPTION_COLOR = 0xFFB8B8B8;
    private static final int ERROR_COLOR = 0xFFFF8080;
    private static final int WARNING_COLOR = 0xFFFFD966;
    private static volatile boolean fillResolved;
    private static volatile Method fillMethod;

    private final Screen parent;
    private final List<FabricConfigScreenModel.Category> categories = FabricConfigScreenModel.categories();
    private int categoryIndex;
    private int groupIndex = -1;
    private int rowOffset;
    private int visibleRows;
    private String searchText = "";
    private String status = "";
    private EditBox searchBox;
    private final Set<String> expandedFeatures = new HashSet<>();
    private boolean draggingScrollbar;

    public FabricModMenuConfigScreen(Screen parent) {
        super(Component.literal("Bits and Balance Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        this.clearWidgets();

        for (int i = 0; i < categories.size(); i++) {
            int tabLeft = tabLeft(i);
            int tabWidth = tabWidth(i);
            final int index = i;
            Button tab = Button.builder(Component.literal(trim(categories.get(i).name(), tabWidth - 8)), ignored -> {
                categoryIndex = index;
                groupIndex = -1;
                rowOffset = 0;
                status = "";
                rebuild();
            }).bounds(tabLeft, TAB_TOP, tabWidth, TAB_HEIGHT).build();
            tab.active = i != categoryIndex;
            this.addRenderableWidget(tab);
        }

        searchBox = new EditBox(this.font, LABEL_LEFT, searchTop(), searchWidth(), 20, Component.literal("Search"));
        searchBox.setMaxLength(128);
        searchBox.setValue(searchText);
        searchBox.setResponder(value -> {
            searchText = value;
            rowOffset = 0;
            rebuild();
            if (searchBox != null) {
                setFocused(searchBox);
            }
        });
        this.addRenderableWidget(searchBox);

        List<FabricConfigScreenModel.Group> groups = currentCategory().groups();
        if (hasGroupButtons()) {
            int groupCount = groups.size() + 1;
            int groupTop = groupTop();
            for (int i = 0; i < groupCount; i++) {
                int buttonLeft = groupLeft(i, groupCount);
                int buttonWidth = groupWidth(i, groupCount);
                int selectedIndex = i - 1;
                Component label = Component.literal(i == 0 ? "All" : trim(groups.get(selectedIndex).name(), buttonWidth - 8));
                Button groupButton = Button.builder(label, ignored -> {
                    groupIndex = selectedIndex;
                    rowOffset = 0;
                    rebuild();
                }).bounds(buttonLeft, groupTop, buttonWidth, 20).build();
                groupButton.active = groupIndex != selectedIndex;
                this.addRenderableWidget(groupButton);
            }
        }

        int rowsTop = rowsTop();
        int footerY = footerY();
        this.visibleRows = Math.max(1, (footerY - rowsTop - BOTTOM_MESSAGE_SPACE) / ROW_HEIGHT);
        clampRowOffset();

        int valueLeft = valueLeft();
        int resetLeft = resetLeft();
        int valueWidth = Math.min(VALUE_WIDTH, Math.max(80, resetLeft - valueLeft - 5));

        List<ConfigRow> rows = visibleConfigRows();
        int end = Math.min(rows.size(), rowOffset + visibleRows);
        for (int i = rowOffset; i < end; i++) {
            ConfigRow row = rows.get(i);
            int y = rowsTop + (i - rowOffset) * ROW_HEIGHT;
            if (row.header()) {
                String featureKey = row.featureKey();
                boolean expanded = expandedFeatures.contains(featureKey);
                final int headerIndex = i;
                Button toggle = Button.builder(Component.literal(expanded ? "Hide" : "Show"), ignored -> {
                    toggleFeatureExpansion(featureKey, headerIndex, row.children().size());
                }).bounds(valueLeft, y, valueWidth, 20).build();
                toggle.active = !expanded;
                this.addRenderableWidget(toggle);
                continue;
            }

            FabricConfigScreenModel.Option option = row.option();
            if (option.isBoolean()) {
                Button toggle = Button.builder(booleanLabel(option), ignored -> {
                    option.toggleBoolean();
                    rebuild();
                }).bounds(valueLeft, y, valueWidth, 20).build();
                this.addRenderableWidget(toggle);
            } else {
                EditBox editBox = new EditBox(this.font, valueLeft, y, valueWidth, 20, Component.literal(option.name()));
                editBox.setMaxLength(4096);
                editBox.setValue(option.pendingText());
                editBox.setResponder(option::setPendingText);
                this.addRenderableWidget(editBox);
            }

            this.addRenderableWidget(Button.builder(Component.empty(), ignored -> {
                option.resetPendingValue();
                rebuild();
            }).bounds(resetLeft, y, RESET_WIDTH, 20).build());
        }

        int footerButtonWidth = 150;
        int footerGap = 8;
        int footerLeft = this.width / 2 - footerButtonWidth - footerGap / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), ignored -> onClose())
                .bounds(footerLeft, footerY, footerButtonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), ignored -> saveAndClose())
                .bounds(footerLeft + footerButtonWidth + footerGap, footerY, footerButtonWidth, 20)
                .build());
    }

    private FabricConfigScreenModel.Category currentCategory() {
        if (categoryIndex < 0 || categoryIndex >= categories.size()) {
            categoryIndex = 0;
        }
        int maxGroup = categories.get(categoryIndex).groups().size() - 1;
        if (groupIndex < -1 || groupIndex > maxGroup) {
            groupIndex = -1;
        }
        return categories.get(categoryIndex);
    }

    private void clampRowOffset() {
        int max = Math.max(0, visibleConfigRows().size() - visibleRows);
        rowOffset = Math.max(0, Math.min(rowOffset, max));
    }

    private void revealExpandedRows(int headerIndex, int childCount) {
        if (childCount <= 0) {
            clampRowOffset();
            return;
        }

        int desiredLastVisibleRow = Math.min(headerIndex + childCount, headerIndex + visibleRows - 1);
        int currentLastVisibleRow = rowOffset + visibleRows - 1;
        if (desiredLastVisibleRow > currentLastVisibleRow) {
            rowOffset += desiredLastVisibleRow - currentLastVisibleRow;
        }
        if (headerIndex < rowOffset) {
            rowOffset = headerIndex;
        }
        clampRowOffset();
    }

    private void toggleFeatureExpansion(String featureKey, int headerIndex, int childCount) {
        if (expandedFeatures.contains(featureKey)) {
            expandedFeatures.remove(featureKey);
            clampRowOffset();
        } else {
            expandedFeatures.add(featureKey);
            revealExpandedRows(headerIndex, childCount);
        }
        rebuild();
    }

    private List<FabricConfigScreenModel.Option> visibleOptions() {
        List<FabricConfigScreenModel.Option> source;
        FabricConfigScreenModel.Category category = currentCategory();
        if (groupIndex >= 0 && groupIndex < category.groups().size()) {
            source = category.groups().get(groupIndex).options();
        } else {
            source = orderedAllOptions(category.options());
        }

        if (searchText.isBlank()) {
            return source;
        }

        return source.stream().filter(option -> option.matches(searchText)).toList();
    }

    private List<ConfigRow> visibleConfigRows() {
        List<FabricConfigScreenModel.Option> options = visibleOptions();
        if (!searchText.isBlank()) {
            return options.stream().map(ConfigRow::option).toList();
        }

        Map<String, List<FabricConfigScreenModel.Option>> byFeature = new LinkedHashMap<>();
        for (FabricConfigScreenModel.Option option : options) {
            byFeature.computeIfAbsent(featureKey(option), ignored -> new ArrayList<>()).add(option);
        }

        List<ConfigRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<FabricConfigScreenModel.Option>> entry : orderedFeatureEntries(byFeature)) {
            List<FabricConfigScreenModel.Option> featureOptions = entry.getValue();
            if (featureOptions.size() <= 1) {
                rows.add(ConfigRow.singleton(featureOptions.get(0)));
                continue;
            }

            FabricConfigScreenModel.Option first = featureOptions.get(0);
            rows.add(ConfigRow.header(entry.getKey(), first.featureName(), featureOptions, 0));
            if (expandedFeatures.contains(entry.getKey())) {
                appendFeatureOptions(rows, entry.getKey(), featureOptions);
            }
        }
        return rows;
    }

    private List<Map.Entry<String, List<FabricConfigScreenModel.Option>>> orderedFeatureEntries(Map<String, List<FabricConfigScreenModel.Option>> byFeature) {
        List<Map.Entry<String, List<FabricConfigScreenModel.Option>>> entries = new ArrayList<>(byFeature.entrySet());
        if (isSelectedGroup("Loot")) {
            entries.sort((left, right) -> {
                boolean leftMaster = isLootSectionMaster(left.getValue());
                boolean rightMaster = isLootSectionMaster(right.getValue());
                if (leftMaster != rightMaster) {
                    return leftMaster ? -1 : 1;
                }
                return 0;
            });
        }
        return entries;
    }

    private boolean isSelectedGroup(String name) {
        FabricConfigScreenModel.Category category = currentCategory();
        return groupIndex >= 0
                && groupIndex < category.groups().size()
                && category.groups().get(groupIndex).name().equals(name);
    }

    private boolean isLootSectionMaster(List<FabricConfigScreenModel.Option> options) {
        return options.size() == 1 && options.get(0).featureName().equals("Custom Loot Tables");
    }

    private void appendFeatureOptions(List<ConfigRow> rows, String parentKey, List<FabricConfigScreenModel.Option> options) {
        List<FabricConfigScreenModel.Option> directOptions = new ArrayList<>();
        Map<String, List<FabricConfigScreenModel.Option>> bySubFeature = new LinkedHashMap<>();
        for (FabricConfigScreenModel.Option option : options) {
            if (option.subFeatureName().isBlank()) {
                directOptions.add(option);
            } else {
                bySubFeature.computeIfAbsent(option.subFeatureName(), ignored -> new ArrayList<>()).add(option);
            }
        }

        for (FabricConfigScreenModel.Option option : orderedOptions(directOptions)) {
            rows.add(ConfigRow.option(option, 1));
        }

        for (Map.Entry<String, List<FabricConfigScreenModel.Option>> entry : bySubFeature.entrySet()) {
            String subFeature = entry.getKey();
            List<FabricConfigScreenModel.Option> subOptions = entry.getValue();
            String subKey = parentKey + "|" + subFeature;
            rows.add(ConfigRow.header(subKey, subFeature, subOptions, 1));
            if (expandedFeatures.contains(subKey)) {
                for (FabricConfigScreenModel.Option option : orderedOptions(subOptions)) {
                    rows.add(ConfigRow.option(option, 2));
                }
            }
        }
    }

    private List<FabricConfigScreenModel.Option> orderedOptions(List<FabricConfigScreenModel.Option> options) {
        if (options.size() <= 1) {
            return options;
        }
        List<FabricConfigScreenModel.Option> ordered = new ArrayList<>(options);
        if (groupIndex < 0) {
            ordered.sort(this::compareOptionsBySetting);
            return ordered;
        }
        ordered.sort((left, right) -> {
            boolean leftEnabled = isEnabledOption(left);
            boolean rightEnabled = isEnabledOption(right);
            if (leftEnabled != rightEnabled) {
                return leftEnabled ? -1 : 1;
            }
            return compareOptionsBySetting(left, right);
        });
        return ordered;
    }

    private List<FabricConfigScreenModel.Option> orderedAllOptions(List<FabricConfigScreenModel.Option> options) {
        if (options.size() <= 1) {
            return options;
        }
        List<FabricConfigScreenModel.Option> ordered = new ArrayList<>(options);
        ordered.sort((left, right) -> {
            int feature = compareText(left.featureName(), right.featureName());
            if (feature != 0) {
                return feature;
            }
            int subFeature = compareText(left.subFeatureName(), right.subFeatureName());
            if (subFeature != 0) {
                return subFeature;
            }
            return compareOptionsBySetting(left, right);
        });
        return ordered;
    }

    private int compareOptionsBySetting(FabricConfigScreenModel.Option left, FabricConfigScreenModel.Option right) {
        int setting = compareText(left.settingName(), right.settingName());
        if (setting != 0) {
            return setting;
        }
        return compareText(left.name(), right.name());
    }

    private int compareText(String left, String right) {
        int insensitive = String.CASE_INSENSITIVE_ORDER.compare(left, right);
        return insensitive != 0 ? insensitive : left.compareTo(right);
    }

    private boolean isEnabledOption(FabricConfigScreenModel.Option option) {
        return option.settingName().equals("Enabled") || option.name().equals("Enabled");
    }

    private String featureKey(FabricConfigScreenModel.Option option) {
        return currentCategory().name() + "|" + option.group() + "|" + option.featureName();
    }

    private void scroll(int amount) {
        rowOffset += amount;
        clampRowOffset();
        rebuild();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0.0D) {
            scroll(verticalAmount > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 0 && canScroll() && mouseX >= scrollbarLeft() - 4 && mouseX <= scrollbarLeft() + SCROLLBAR_WIDTH + 4
                && mouseY >= scrollbarTop() && mouseY <= scrollbarBottom()) {
            draggingScrollbar = true;
            updateRowOffsetFromMouse(mouseY);
            return true;
        }
        if (event.button() == 0 && mouseClickedHeaderLabel(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScrollbar) {
            updateRowOffsetFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private Component booleanLabel(FabricConfigScreenModel.Option option) {
        return Component.literal(option.booleanValue() ? "True" : "False")
                .withStyle(option.booleanValue() ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private boolean mouseClickedHeaderLabel(double mouseX, double mouseY) {
        if (mouseX < LABEL_LEFT || mouseX >= valueButtonRight() || mouseY < rowsTop()) {
            return false;
        }

        int rowIndex = rowOffset + (int) ((mouseY - rowsTop()) / ROW_HEIGHT);
        if (rowIndex < rowOffset || rowIndex >= rowOffset + visibleRows) {
            return false;
        }

        List<ConfigRow> rows = visibleConfigRows();
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return false;
        }

        ConfigRow row = rows.get(rowIndex);
        if (!row.header()) {
            return false;
        }

        playButtonClickSound();
        toggleFeatureExpansion(row.featureKey(), rowIndex, row.children().size());
        return true;
    }

    private void playButtonClickSound() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
    }

    private void saveAndClose() {
        try {
            FabricConfigScreenModel.saveAll(categories);
            Minecraft.getInstance().setScreen(parent);
        } catch (IOException e) {
            status = e.getMessage() == null ? "Failed to save config" : e.getMessage();
            rebuild();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        drawSearchHint(guiGraphics);
        drawRows(guiGraphics);
        drawResetIcons(guiGraphics);
        drawScrollbar(guiGraphics);
        drawHoveredFullText(guiGraphics, mouseX, mouseY);
        if (visibleConfigRows().isEmpty()) {
            drawCentered(guiGraphics, Component.literal("No matching configuration options"), this.width / 2, rowsTop() + 20, DESCRIPTION_COLOR);
        }
        drawRestartWarning(guiGraphics);
        if (!status.isBlank()) {
            int statusY = restartWarningNeeded() ? this.height - 58 : this.height - 48;
            drawCentered(guiGraphics, Component.literal(trim(status, this.width - 40)), this.width / 2, statusY, ERROR_COLOR);
        }
    }

    private void drawRestartWarning(GuiGraphicsExtractor guiGraphics) {
        if (restartWarningNeeded()) {
            drawCentered(guiGraphics, Component.literal("Restart required after saving these changes."), this.width / 2, footerY() - 18, WARNING_COLOR);
        }
    }

    private boolean restartWarningNeeded() {
        for (FabricConfigScreenModel.Category category : categories) {
            for (FabricConfigScreenModel.Option option : category.options()) {
                if (option.restartRecommended() && option.hasPendingChange()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void drawSearchHint(GuiGraphicsExtractor guiGraphics) {
        if (searchText.isBlank()) {
            guiGraphics.text(this.font, Component.literal("Search..."), LABEL_LEFT + 4, searchTop() + 6, 0xFF808080, false);
        }
    }

    private void drawRows(GuiGraphicsExtractor guiGraphics) {
        int rowsTop = rowsTop();
        int labelWidth = Math.max(80, valueLeft() - LABEL_LEFT - 16);
        List<ConfigRow> rows = visibleConfigRows();
        int end = Math.min(rows.size(), rowOffset + visibleRows);
        for (int i = rowOffset; i < end; i++) {
            ConfigRow row = rows.get(i);
            int y = rowsTop + (i - rowOffset) * ROW_HEIGHT + 2;
            if (row.header()) {
                int labelLeft = LABEL_LEFT + row.level() * 12;
                int adjustedWidth = Math.max(40, labelWidth - row.level() * 12);
                guiGraphics.text(this.font, Component.literal(trim(row.featureName(), adjustedWidth)), labelLeft, y, TEXT_COLOR, false);
                guiGraphics.text(this.font, Component.literal(row.children().size() + " settings"), labelLeft, y + 11, DESCRIPTION_COLOR, false);
                continue;
            }

            FabricConfigScreenModel.Option option = row.option();
            String title = optionTitle(row);
            int labelLeft = LABEL_LEFT + row.level() * 12;
            int adjustedWidth = Math.max(40, labelWidth - row.level() * 12);
            guiGraphics.text(this.font, Component.literal(trim(title, adjustedWidth)), labelLeft, y, TEXT_COLOR, false);
            guiGraphics.text(this.font, Component.literal(trim(option.description(), adjustedWidth)), labelLeft, y + 11, DESCRIPTION_COLOR, false);
        }
    }

    private String optionTitle(ConfigRow row) {
        FabricConfigScreenModel.Option option = row.option();
        if (searchText.isBlank()
                && row.level() == 0
                && row.children().size() == 1
                && isEnabledOption(option)
                && !isSectionMasterToggle(option)
                && !option.featureName().equals(option.settingName())) {
            return option.featureName();
        }
        if (groupIndex < 0
                && searchText.isBlank()
                && row.level() == 0
                && isEnabledOption(option)
                && !option.featureName().equals(option.settingName())) {
            return option.featureName();
        }
        return searchText.isBlank() && !option.settingName().equals(option.name()) ? option.settingName() : option.name();
    }

    private boolean isSectionMasterToggle(FabricConfigScreenModel.Option option) {
        return option.featureName().equals("Custom Brewing")
                || option.featureName().equals("Custom Loot Tables");
    }

    private void drawResetIcons(GuiGraphicsExtractor guiGraphics) {
        int rowsTop = rowsTop();
        int resetLeft = resetLeft();
        List<ConfigRow> rows = visibleConfigRows();
        int end = Math.min(rows.size(), rowOffset + visibleRows);
        for (int i = rowOffset; i < end; i++) {
            ConfigRow row = rows.get(i);
            if (row.header()) {
                continue;
            }
            drawResetIcon(guiGraphics, resetLeft, rowsTop + (i - rowOffset) * ROW_HEIGHT);
        }
    }

    private void drawResetIcon(GuiGraphicsExtractor guiGraphics, int buttonLeft, int buttonTop) {
        int color = 0xFFE6E6E6;
        int shadow = 0x80303030;
        int left = buttonLeft + 7;
        int top = buttonTop + 5;
        drawResetIconPixels(guiGraphics, left + 1, top + 1, shadow);
        drawResetIconPixels(guiGraphics, left, top, color);
    }

    private void drawResetIconPixels(GuiGraphicsExtractor guiGraphics, int left, int top, int color) {
        fill(guiGraphics, left + 2, top + 1, left + 4, top + 2, color);
        fill(guiGraphics, left + 1, top + 2, left + 5, top + 3, color);
        fill(guiGraphics, left, top + 3, left + 2, top + 4, color);
        fill(guiGraphics, left + 1, top + 4, left + 5, top + 5, color);
        fill(guiGraphics, left + 2, top + 5, left + 4, top + 6, color);

        fill(guiGraphics, left + 5, top + 2, left + 8, top + 3, color);
        fill(guiGraphics, left + 8, top + 3, left + 10, top + 4, color);
        fill(guiGraphics, left + 9, top + 4, left + 10, top + 7, color);
        fill(guiGraphics, left + 8, top + 7, left + 9, top + 8, color);
        fill(guiGraphics, left + 5, top + 8, left + 8, top + 9, color);
    }

    private void drawHoveredFullText(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        String text = hoveredFullText(mouseX, mouseY);
        if (text.isBlank()) {
            return;
        }

        int maxContentWidth = Math.max(80, Math.min(320, this.width - 40));
        List<String> lines = wrapText(text, maxContentWidth);
        int contentWidth = 0;
        for (String line : lines) {
            contentWidth = Math.max(contentWidth, this.font.width(line));
        }
        int boxWidth = contentWidth + 8;
        int boxHeight = lines.size() * 10 + 8;
        int left = Math.min(mouseX + 12, this.width - boxWidth - 6);
        int top = Math.min(mouseY + 12, this.height - boxHeight - 6);
        left = Math.max(6, left);
        top = Math.max(6, top);

        fill(guiGraphics, left - 1, top - 1, left + boxWidth + 1, top + boxHeight + 1, 0xEE101010);
        fill(guiGraphics, left, top, left + boxWidth, top + boxHeight, 0xEE2A2A2A);
        for (int i = 0; i < lines.size(); i++) {
            guiGraphics.text(this.font, Component.literal(lines.get(i)), left + 4, top + 4 + i * 10, TEXT_COLOR, false);
        }
    }

    private String hoveredFullText(double mouseX, double mouseY) {
        int labelWidth = Math.max(80, valueLeft() - LABEL_LEFT - 16);
        if (mouseX < LABEL_LEFT || mouseX >= valueLeft() - 8 || mouseY < rowsTop()) {
            return "";
        }

        int rowIndex = rowOffset + (int) ((mouseY - rowsTop()) / ROW_HEIGHT);
        if (rowIndex < rowOffset || rowIndex >= rowOffset + visibleRows) {
            return "";
        }

        List<ConfigRow> rows = visibleConfigRows();
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return "";
        }

        ConfigRow row = rows.get(rowIndex);
        if (row.header()) {
            int adjustedWidth = Math.max(40, labelWidth - row.level() * 12);
            return isTruncated(row.featureName(), adjustedWidth) ? row.featureName() : "";
        }

        FabricConfigScreenModel.Option option = row.option();
        String title = optionTitle(row);
        int adjustedWidth = Math.max(40, labelWidth - row.level() * 12);
        if (!isTruncated(title, adjustedWidth) && !isTruncated(option.description(), adjustedWidth)) {
            return "";
        }
        return title + "\n" + option.description();
    }

    private List<String> wrapText(String text, int width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            String remaining = paragraph.trim();
            if (remaining.isBlank()) {
                lines.add("");
                continue;
            }
            while (!remaining.isBlank()) {
                String line = this.font.plainSubstrByWidth(remaining, width);
                if (line.length() >= remaining.length()) {
                    lines.add(remaining);
                    break;
                }
                int split = line.lastIndexOf(' ');
                if (split <= 0) {
                    split = Math.max(1, line.length());
                }
                lines.add(remaining.substring(0, split).trim());
                remaining = remaining.substring(split).trim();
            }
        }
        return lines;
    }

    private boolean isTruncated(String text, int width) {
        return this.font != null && this.font.width(text) > width;
    }

    private void drawScrollbar(GuiGraphicsExtractor guiGraphics) {
        if (!canScroll()) {
            return;
        }

        int top = scrollbarTop();
        int bottom = scrollbarBottom();
        int left = scrollbarLeft();
        int thumbHeight = scrollbarThumbHeight();
        int thumbTop = scrollbarThumbTop();

        fill(guiGraphics, left, top, left + SCROLLBAR_WIDTH, bottom, 0xAA202020);
        fill(guiGraphics, left + 1, thumbTop, left + SCROLLBAR_WIDTH - 1, thumbTop + thumbHeight, 0xFFD0D0D0);
    }

    private int tabLeft(int index) {
        return index * availableTabWidth() / Math.max(1, categories.size());
    }

    private int tabWidth(int index) {
        if (index == categories.size() - 1) {
            return this.width - tabLeft(index);
        }
        return Math.max(40, tabLeft(index + 1) - tabLeft(index));
    }

    private int availableTabWidth() {
        return Math.max(1, this.width);
    }

    private int searchTop() {
        return TAB_TOP + TAB_HEIGHT + 7;
    }

    private int searchWidth() {
        return Math.max(80, this.width - LABEL_LEFT * 2);
    }

    private int groupTop() {
        return searchTop() + 24;
    }

    private int groupLeft(int index, int count) {
        return LABEL_LEFT + index * searchWidth() / Math.max(1, count);
    }

    private int groupWidth(int index, int count) {
        int left = groupLeft(index, count);
        int right = index == count - 1 ? LABEL_LEFT + searchWidth() : groupLeft(index + 1, count);
        return Math.max(40, right - left - 4);
    }

    private int rowsTop() {
        return hasGroupButtons() ? groupTop() + 26 : searchTop() + 26;
    }

    private boolean hasGroupButtons() {
        return currentCategory().groups().size() > 1;
    }

    private int footerY() {
        return this.height - 26;
    }

    private int valueLeft() {
        return Math.max(LABEL_LEFT + 90, this.width - 185);
    }

    private int resetLeft() {
        return this.width - 34;
    }

    private int valueButtonRight() {
        int valueLeft = valueLeft();
        int valueWidth = Math.min(VALUE_WIDTH, Math.max(80, resetLeft() - valueLeft - 5));
        return valueLeft + valueWidth;
    }

    private boolean canScroll() {
        return visibleConfigRows().size() > visibleRows;
    }

    private int scrollbarTop() {
        return rowsTop();
    }

    private int scrollbarBottom() {
        return footerY() - 7;
    }

    private int scrollbarLeft() {
        return this.width - 7;
    }

    private int scrollbarThumbHeight() {
        int rowCount = visibleConfigRows().size();
        int trackHeight = Math.max(1, scrollbarBottom() - scrollbarTop());
        return Math.max(18, trackHeight * visibleRows / Math.max(1, rowCount));
    }

    private int scrollbarThumbTop() {
        int rowCount = visibleConfigRows().size();
        int trackHeight = Math.max(1, scrollbarBottom() - scrollbarTop());
        int thumbHeight = scrollbarThumbHeight();
        int maxOffset = Math.max(1, rowCount - visibleRows);
        return scrollbarTop() + (trackHeight - thumbHeight) * rowOffset / maxOffset;
    }

    private void updateRowOffsetFromMouse(double mouseY) {
        int rowCount = visibleConfigRows().size();
        int trackHeight = Math.max(1, scrollbarBottom() - scrollbarTop());
        int thumbHeight = scrollbarThumbHeight();
        int maxOffset = Math.max(1, rowCount - visibleRows);
        double relative = (mouseY - scrollbarTop() - thumbHeight / 2.0D) / Math.max(1.0D, trackHeight - thumbHeight);
        rowOffset = (int) Math.round(relative * maxOffset);
        clampRowOffset();
        rebuild();
    }

    private void drawCentered(GuiGraphicsExtractor guiGraphics, Component text, int centerX, int y, int color) {
        int x = centerX - this.font.width(text) / 2;
        guiGraphics.text(this.font, text, x, y, color, false);
    }

    private static void fill(GuiGraphicsExtractor guiGraphics, int left, int top, int right, int bottom, int color) {
        Method method = resolveFillMethod(guiGraphics);
        if (method == null) {
            return;
        }

        try {
            method.invoke(guiGraphics, left, top, right, bottom, color);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method resolveFillMethod(GuiGraphicsExtractor guiGraphics) {
        if (fillResolved) {
            return fillMethod;
        }

        fillResolved = true;
        for (Method method : guiGraphics.getClass().getMethods()) {
            if (!method.getName().equals("fill")) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 5) {
                continue;
            }
            boolean allInts = true;
            for (Class<?> parameterType : parameterTypes) {
                if (parameterType != int.class) {
                    allInts = false;
                    break;
                }
            }
            if (!allInts) {
                continue;
            }

            method.setAccessible(true);
            fillMethod = method;
            break;
        }

        return fillMethod;
    }

    private String trim(String text, int width) {
        String result = text;
        while (this.font != null && this.font.width(result) > width && result.length() > 4) {
            result = result.substring(0, result.length() - 4) + "...";
        }
        return result;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record ConfigRow(FabricConfigScreenModel.Option option, String featureKey, String featureName,
                             List<FabricConfigScreenModel.Option> children, int level) {
        static ConfigRow option(FabricConfigScreenModel.Option option) {
            return new ConfigRow(option, "", "", List.of(), 0);
        }

        static ConfigRow singleton(FabricConfigScreenModel.Option option) {
            return new ConfigRow(option, "", "", List.of(option), 0);
        }

        static ConfigRow option(FabricConfigScreenModel.Option option, int level) {
            return new ConfigRow(option, "", "", List.of(), level);
        }

        static ConfigRow header(String featureKey, String featureName, List<FabricConfigScreenModel.Option> children, int level) {
            return new ConfigRow(null, featureKey, featureName, children, level);
        }

        boolean header() {
            return option == null;
        }
    }

}
