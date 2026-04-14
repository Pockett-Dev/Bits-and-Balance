package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.onenonly.bitsandbalance.fabric.network.NavigatorCompassSetPayload;

/** GUI screen for Navigator Compass coordinate input. */
public class FabricNavigatorCompassScreen extends Screen {

    private EditBox xInput;
    private EditBox yInput;
    private EditBox zInput;
    private Button setButton;
    private Button clearButton;
    private Button cancelButton;

    private final int currentX;
    private final int currentY;
    private final int currentZ;

    public FabricNavigatorCompassScreen(int currentX, int currentY, int currentZ) {
        super(Component.literal("Navigator Compass"));
        this.currentX = currentX;
        this.currentY = currentY;
        this.currentZ = currentZ;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.xInput = new EditBox(this.font, centerX - 100, centerY - 20, 60, 20, Component.literal("X"));
        this.yInput = new EditBox(this.font, centerX - 30, centerY - 20, 60, 20, Component.literal("Y"));
        this.zInput = new EditBox(this.font, centerX + 40, centerY - 20, 60, 20, Component.literal("Z"));

        this.xInput.setValue(String.valueOf(currentX));
        this.yInput.setValue(String.valueOf(currentY));
        this.zInput.setValue(String.valueOf(currentZ));

        this.setButton = Button.builder(Component.literal("Set"), this::onSetTarget)
                .bounds(centerX - 100, centerY + 30, 60, 20)
                .build();

        this.clearButton = Button.builder(Component.literal("Clear"), this::onClearTarget)
                .bounds(centerX - 30, centerY + 30, 60, 20)
                .build();

        this.cancelButton = Button.builder(Component.literal("Cancel"), this::onCancel)
                .bounds(centerX + 40, centerY + 30, 60, 20)
                .build();

        this.addRenderableWidget(xInput);
        this.addRenderableWidget(yInput);
        this.addRenderableWidget(zInput);
        this.addRenderableWidget(setButton);
        this.addRenderableWidget(clearButton);
        this.addRenderableWidget(cancelButton);

        this.setInitialFocus(xInput);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawCentered(guiGraphics, this.title, this.width / 2, 20, 0xFFFFFF);
        drawCentered(guiGraphics, Component.literal("X"), this.width / 2 - 70, this.height / 2 - 45, 0xFFFFFF);
        drawCentered(guiGraphics, Component.literal("Y"), this.width / 2, this.height / 2 - 45, 0xFFFFFF);
        drawCentered(guiGraphics, Component.literal("Z"), this.width / 2 + 70, this.height / 2 - 45, 0xFFFFFF);
        drawCentered(guiGraphics, Component.literal("Enter coordinates for compass target"), this.width / 2, this.height / 2 - 70, 0xAAAAAA);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
        int x = centerX - this.font.width(text) / 2;
        guiGraphics.drawString(this.font, text, x, y, color, false);
    }

    private void onSetTarget(Button button) {
        try {
            int x = Integer.parseInt(xInput.getValue());
            int y = Integer.parseInt(yInput.getValue());
            int z = Integer.parseInt(zInput.getValue());

            ClientPlayNetworking.send(new NavigatorCompassSetPayload(x, y, z));
            this.onClose();
        } catch (NumberFormatException e) {
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(), 0.5f, 0.5f));
        }
    }

    private void onClearTarget(Button button) {
        ConfirmScreen confirmScreen = new ConfirmScreen(
                (confirmed) -> {
                    if (confirmed) {
                        ClientPlayNetworking.send(new NavigatorCompassSetPayload(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
                        this.onClose();
                    } else {
                        Minecraft.getInstance().setScreen(this);
                    }
                },
                Component.literal("Clear Compass Target"),
                Component.literal("Are you sure you want to clear the compass target?\nThis will make the compass point to world spawn again.")
        );

        Minecraft.getInstance().setScreen(confirmScreen);
    }

    private void onCancel(Button button) {
        this.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (super.keyPressed(keyEvent)) {
            return true;
        }

        int keyCode = keyEvent.key();
        if (keyCode == 257) { // Enter
            onSetTarget(setButton);
            return true;
        }
        if (keyCode == 256) { // Escape
            onCancel(cancelButton);
            return true;
        }

        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
