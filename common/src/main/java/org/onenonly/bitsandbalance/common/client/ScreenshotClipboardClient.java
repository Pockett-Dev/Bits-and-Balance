package org.onenonly.bitsandbalance.common.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
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
import java.util.function.Consumer;

public final class ScreenshotClipboardClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile boolean clipboardSupportInitialized;

    private ScreenshotClipboardClient() {
    }

    public static void initializeClipboardSupport() {
        if (clipboardSupportInitialized) {
            return;
        }

        synchronized (ScreenshotClipboardClient.class) {
            if (clipboardSupportInitialized) {
                return;
            }

            if (!isMacOs()) {
                System.setProperty("java.awt.headless", "false");
            }

            clipboardSupportInitialized = true;
        }
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
        Screenshot.takeScreenshot(renderTarget, 1, nativeImage -> saveScreenshotAndCopy(nativeImage, screenshotFile, messageConsumer));
    }

    private static void saveScreenshotAndCopy(NativeImage nativeImage, File screenshotFile, Consumer<Component> messageConsumer) {
        Util.ioPool().execute(() -> writeScreenshotAndCopy(nativeImage, screenshotFile, messageConsumer));
    }

    private static void writeScreenshotAndCopy(NativeImage nativeImage, File screenshotFile, Consumer<Component> messageConsumer) {
        try (nativeImage) {
            File parent = screenshotFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            BufferedImage clipboardImage = createBufferedImage(nativeImage, screenshotFile);
            nativeImage.writeToFile(screenshotFile);

            messageConsumer.accept(createSuccessMessage(screenshotFile));

            if (clipboardImage != null && tryCopyScreenshotToClipboard(clipboardImage, screenshotFile)) {
                LOGGER.info("Copied screenshot to clipboard: {}", screenshotFile);
                notifyCopiedMessage();
            }
        } catch (IOException exception) {
            LOGGER.warn("Couldn't save screenshot", exception);
            messageConsumer.accept(Component.translatable("screenshot.failure", exception.getMessage()));
        }
    }

    private static BufferedImage createBufferedImage(NativeImage nativeImage, File screenshotFile) {
        try {
            int width = nativeImage.getWidth();
            int height = nativeImage.getHeight();
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            bufferedImage.setRGB(0, 0, width, height, nativeImage.getPixels(), 0, width);
            return bufferedImage;
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to convert screenshot {} into a clipboard image", screenshotFile, throwable);
            return null;
        }
    }

    private static Component createSuccessMessage(File screenshotFile) {
        Component fileComponent = Component.literal(screenshotFile.getName())
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(screenshotFile.getAbsoluteFile())));

        return Component.translatable("screenshot.success", fileComponent);
    }

    private static void notifyCopiedMessage() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("message.bitsandbalance.screenshot_copied"), false);
            }
        });
    }

    private static boolean tryCopyScreenshotToClipboard(BufferedImage screenshotImage, File screenshotFile) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new ImageSelection(screenshotImage), null);
            return true;
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to copy screenshot {} to the system clipboard", screenshotFile, throwable);
            return false;
        }
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