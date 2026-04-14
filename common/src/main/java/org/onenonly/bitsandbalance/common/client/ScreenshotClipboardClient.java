package org.onenonly.bitsandbalance.common.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.slf4j.Logger;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class ScreenshotClipboardClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean clipboardSupportInitialized;

    private ScreenshotClipboardClient() {
    }

    public static synchronized void initializeClipboardSupport() {
        if (clipboardSupportInitialized) {
            return;
        }

        if (!isMacOs()) {
            System.setProperty("java.awt.headless", "false");
        }

        clipboardSupportInitialized = true;
    }

    public static File createUniqueScreenshotFile(File gameDirectory) {
        File screenshotDirectory = new File(gameDirectory, "screenshots");
        String baseName = Util.getFilenameFormattedDateTime();
        int duplicateIndex = 1;

        while (true) {
            String suffix = duplicateIndex == 1 ? "" : "_" + duplicateIndex;
            File screenshotFile = new File(screenshotDirectory, baseName + suffix + ".png");
            if (!screenshotFile.exists()) {
                return screenshotFile;
            }

            duplicateIndex++;
        }
    }

    public static void captureScreenshot(File gameDirectory, RenderTarget renderTarget, Consumer<Component> messageConsumer) {
        initializeClipboardSupport();

        File screenshotFile = createUniqueScreenshotFile(gameDirectory);
        Screenshot.takeScreenshot(renderTarget, 1, nativeImage ->
                Util.ioPool().execute(() -> saveAndCopyScreenshot(screenshotFile, nativeImage, messageConsumer))
        );
    }

    private static void saveAndCopyScreenshot(File screenshotFile, NativeImage nativeImage, Consumer<Component> messageConsumer) {
        Path screenshotPath = screenshotFile.toPath();
        try {
            Files.createDirectories(screenshotPath.getParent());
            BufferedImage screenshotImage = toBufferedImage(nativeImage);
            nativeImage.writeToFile(screenshotPath);

            deliverMessage(messageConsumer, Component.translatable("screenshot.success", openFileComponent(screenshotFile)));

            if (tryCopyScreenshotToClipboard(screenshotImage)) {
                LOGGER.info("Copied screenshot to clipboard: {}", screenshotPath);
                notifyScreenshotCopied();
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to save screenshot {}", screenshotPath, exception);
            deliverMessage(messageConsumer, Component.translatable("screenshot.failure", exception.getMessage()));
        } finally {
            nativeImage.close();
        }
    }

    private static BufferedImage toBufferedImage(NativeImage nativeImage) {
        int width = nativeImage.getWidth();
        int height = nativeImage.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        bufferedImage.setRGB(0, 0, width, height, nativeImage.getPixels(), 0, width);
        return bufferedImage;
    }

    private static Component openFileComponent(File screenshotFile) {
        return Component.literal(screenshotFile.getName())
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(screenshotFile.getAbsolutePath()))
                        .withUnderlined(true));
    }

    private static boolean tryCopyScreenshotToClipboard(BufferedImage screenshotImage) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new ImageSelection(screenshotImage), null);
            return true;
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to copy screenshot to the system clipboard", throwable);
            return false;
        }
    }

    private static void notifyScreenshotCopied() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null) {
                notifyClient(minecraft.player, Component.translatable("message.bitsandbalance.screenshot_copied"));
            }
        });
    }

    private static void notifyClient(Object player, Component message) {
        try {
            player.getClass().getMethod("displayClientMessage", Component.class, boolean.class).invoke(player, message, false);
            return;
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            player.getClass().getMethod("sendOverlayMessage", Component.class).invoke(player, message);
            return;
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            player.getClass().getMethod("sendSystemMessage", Component.class).invoke(player, message);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void deliverMessage(Consumer<Component> messageConsumer, Component message) {
        Minecraft.getInstance().execute(() -> messageConsumer.accept(message));
    }

    private static boolean isMacOs() {
        String osName = System.getProperty("os.name", "");
        return osName.regionMatches(true, 0, "Mac", 0, "Mac".length());
    }

    private record ImageSelection(Image image) implements Transferable {
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }

            return image;
        }
    }
}