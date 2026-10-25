package org.onenonly.bitsandbalance.common.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
//? if mc != 26.1 && mc != 26.2 && mc != 26.3
import net.minecraft.ChatFormatting;
//? endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
//? if mc == 1.21.1
import net.minecraft.Util;
//? else
import net.minecraft.util.Util;
//? endif
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
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import java.nio.file.Files;
import java.nio.file.Path;
//? endif
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
        //? if mc == 1.21.1
        saveScreenshotAndCopy(Screenshot.takeScreenshot(renderTarget), screenshotFile, messageConsumer);
        //? else
        Screenshot.takeScreenshot(renderTarget, 1, nativeImage -> saveScreenshotAndCopy(nativeImage, screenshotFile, messageConsumer));
        //? endif
    }

    private static void saveScreenshotAndCopy(NativeImage nativeImage, File screenshotFile, Consumer<Component> messageConsumer) {
        Util.ioPool().execute(() -> writeScreenshotAndCopy(nativeImage, screenshotFile, messageConsumer));
    }

    private static void writeScreenshotAndCopy(NativeImage nativeImage, File screenshotFile, Consumer<Component> messageConsumer) {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        Path screenshotPath = screenshotFile.toPath();
        //? endif
        try (nativeImage) {
            //? if mc == 26.1 || mc == 26.2 || mc == 26.3
            Files.createDirectories(screenshotPath.getParent());
            //? else
            File parent = screenshotFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            //? endif

            BufferedImage clipboardImage = createBufferedImage(nativeImage, screenshotFile);
            //? if mc == 26.1 || mc == 26.2 || mc == 26.3
            nativeImage.writeToFile(screenshotPath);
            deliverMessage(messageConsumer, createSuccessMessage(screenshotFile));
            //? else
            nativeImage.writeToFile(screenshotFile);
            messageConsumer.accept(createSuccessMessage(screenshotFile));
            //? endif

            if (clipboardImage != null && tryCopyScreenshotToClipboard(clipboardImage, screenshotFile)) {
                //? if mc == 26.1 || mc == 26.2 || mc == 26.3
                LOGGER.info("Copied screenshot to clipboard: {}", screenshotPath);
                //? else
                LOGGER.info("Copied screenshot to clipboard: {}", screenshotFile);
                //? endif
                notifyCopiedMessage();
            }
        } catch (IOException exception) {
            //? if mc == 26.1 || mc == 26.2 || mc == 26.3
            LOGGER.warn("Failed to save screenshot {}", screenshotPath, exception);
            deliverMessage(messageConsumer, Component.translatable("screenshot.failure", exception.getMessage()));
            //? else
            LOGGER.warn("Couldn't save screenshot", exception);
            messageConsumer.accept(Component.translatable("screenshot.failure", exception.getMessage()));
            //? endif
        }
    }

    private static BufferedImage createBufferedImage(NativeImage nativeImage, File screenshotFile) {
        try {
            int width = nativeImage.getWidth();
            int height = nativeImage.getHeight();
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            //? if mc == 1.21.1
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    bufferedImage.setRGB(x, y, bitsandbalance$nativeToArgb(nativeImage.getPixelRGBA(x, y)));
                }
            }
            //? else
            bufferedImage.setRGB(0, 0, width, height, nativeImage.getPixels(), 0, width);
            //? endif
            return bufferedImage;
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to convert screenshot {} into a clipboard image", screenshotFile, throwable);
            return null;
        }
    }

    //? if mc == 1.21.1
    private static int bitsandbalance$nativeToArgb(int nativeRgba) {
        int alpha = (nativeRgba >>> 24) & 0xFF;
        int blue = (nativeRgba >>> 16) & 0xFF;
        int green = (nativeRgba >>> 8) & 0xFF;
        int red = nativeRgba & 0xFF;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
    //? endif

    private static Component createSuccessMessage(File screenshotFile) {
        //? if mc == 1.21.1
        Component fileComponent = Component.literal(screenshotFile.getName())
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, screenshotFile.getAbsolutePath())));
        //? else
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        Component fileComponent = Component.literal(screenshotFile.getName())
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(screenshotFile.getAbsolutePath()))
                        .withUnderlined(true));
        //? else
        Component fileComponent = Component.literal(screenshotFile.getName())
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(screenshotFile.getAbsoluteFile())));
        //? endif
        //? endif

        return Component.translatable("screenshot.success", fileComponent);
    }

    private static void notifyCopiedMessage() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null) {
                //? if mc == 26.1 || mc == 26.2 || mc == 26.3
                notifyClient(minecraft.player, Component.translatable("message.bitsandbalance.screenshot_copied"));
                //? else
                minecraft.player.displayClientMessage(Component.translatable("message.bitsandbalance.screenshot_copied"), false);
                //? endif
            }
        });
    }

    private static boolean tryCopyScreenshotToClipboard(BufferedImage screenshotImage, File screenshotFile) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new ImageSelection(screenshotImage), null);
            return true;
        } catch (Throwable throwable) {
            //? if mc == 26.1 || mc == 26.2 || mc == 26.3
            LOGGER.warn("Failed to copy screenshot to the system clipboard", throwable);
            //? else
            LOGGER.warn("Failed to copy screenshot {} to the system clipboard", screenshotFile, throwable);
            //? endif
            return false;
        }
    }

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
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
    //? endif

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
