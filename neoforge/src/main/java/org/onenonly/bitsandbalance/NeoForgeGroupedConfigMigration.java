package org.onenonly.bitsandbalance;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

public final class NeoForgeGroupedConfigMigration {
    private NeoForgeGroupedConfigMigration() {
    }

    public static void migrateIfNeeded() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("Bits and Balance");

        migrateMergedSections(
                configDir,
                "gameplay.toml",
            List.of(
                "combat.toml",
                "balance.toml",
                "mechanics.toml",
                "tweaks.toml",
                "mobs.toml",
                "gameplay-combat.toml",
                "gameplay-balance.toml",
                "gameplay-tweaks.toml",
                "gameplay-mobs.toml"
            )
        );
        migrateMergedSections(
                configDir,
                "content.toml",
            List.of(
                "potions.toml",
                "building.toml",
                "enchanting.toml",
                "recipes.toml",
                "loot_tables.toml",
                "content-potions.toml",
                "content-building.toml",
                "content-enchanting.toml",
                "content-recipes.toml",
                "content-loot-tables.toml"
            )
        );
        migrateCopy(configDir, "worldgen.toml", "world.toml");
        migrateDogMusicDiscSection(configDir);
        migrateGlowGooSection(configDir);
    }

    private static void migrateGlowGooSection(Path configDir) {
        Path contentPath = configDir.resolve("content.toml");
        String targetHeader = "[glowGoo]";

        if (fileContains(contentPath, targetHeader)) {
            stripSection(configDir.resolve("gameplay.toml"), "[mechanics.glowGoo]");
            return;
        }

        String migratedBody = extractSectionBody(configDir.resolve("gameplay.toml"), "[mechanics.glowGoo]");
        if (migratedBody != null) {
            String lineSeparator = System.lineSeparator();
            String existingContent = readText(contentPath);
            StringBuilder merged = new StringBuilder(existingContent == null ? "" : existingContent.trim());
            if (!merged.isEmpty()) {
                merged.append(lineSeparator).append(lineSeparator);
            }
            merged.append("# Migrated from gameplay.toml").append(lineSeparator);
            merged.append(targetHeader).append(lineSeparator);
            merged.append(migratedBody.strip()).append(lineSeparator);

            try {
                Files.createDirectories(configDir);
                Files.writeString(contentPath, merged.toString(), StandardCharsets.UTF_8);
            } catch (IOException ignored) {
            }
        }

        stripSection(configDir.resolve("gameplay.toml"), "[mechanics.glowGoo]");
    }

    private static void migrateDogMusicDiscSection(Path configDir) {
        Path contentPath = configDir.resolve("content.toml");
        String targetHeader = "[lootTables.dogMusicDisc]";

        if (fileContains(contentPath, targetHeader)) {
            stripSection(configDir.resolve("worldgen.toml"), "[dogMusicDisc]");
            stripSection(configDir.resolve("world.toml"), "[dogMusicDisc]");
            return;
        }

        String migratedBody = null;
        String sourceFileName = null;
        for (String sourceName : List.of("worldgen.toml", "world.toml")) {
            Path sourcePath = configDir.resolve(sourceName);
            String body = extractSectionBody(sourcePath, "[dogMusicDisc]");
            if (body != null) {
                migratedBody = body;
                sourceFileName = sourceName;
                break;
            }
        }

        if (migratedBody != null) {
            String lineSeparator = System.lineSeparator();
            String existingContent = readText(contentPath);
            StringBuilder merged = new StringBuilder(existingContent == null ? "" : existingContent.trim());
            if (!merged.isEmpty()) {
                merged.append(lineSeparator).append(lineSeparator);
            }
            merged.append("# Migrated from ").append(sourceFileName).append(lineSeparator);
            merged.append(targetHeader).append(lineSeparator);
            merged.append(migratedBody.strip()).append(lineSeparator);

            try {
                Files.createDirectories(configDir);
                Files.writeString(contentPath, merged.toString(), StandardCharsets.UTF_8);
            } catch (IOException ignored) {
            }
        }

        stripSection(configDir.resolve("worldgen.toml"), "[dogMusicDisc]");
        stripSection(configDir.resolve("world.toml"), "[dogMusicDisc]");
    }

    private static boolean fileContains(Path path, String needle) {
        String text = readText(path);
        return text != null && text.contains(needle);
    }

    private static String extractSectionBody(Path path, String header) {
        String text = readText(path);
        if (text == null) {
            return null;
        }

        int start = text.indexOf(header);
        if (start < 0) {
            return null;
        }

        int bodyStart = text.indexOf('\n', start);
        if (bodyStart < 0) {
            return "";
        }
        bodyStart++;

        int nextSection = text.indexOf("\n[", bodyStart);
        int end = nextSection >= 0 ? nextSection + 1 : text.length();
        return text.substring(bodyStart, end).trim();
    }

    private static void stripSection(Path path, String header) {
        String text = readText(path);
        if (text == null) {
            return;
        }

        int start = text.indexOf(header);
        if (start < 0) {
            return;
        }

        int sectionStart = start;
        while (sectionStart > 0) {
            int previousLineStart = text.lastIndexOf('\n', sectionStart - 2);
            int lineStart = previousLineStart >= 0 ? previousLineStart + 1 : 0;
            String line = text.substring(lineStart, sectionStart).trim();
            if (!line.startsWith("#") && !line.isEmpty()) {
                break;
            }
            sectionStart = lineStart;
            if (line.isEmpty()) {
                break;
            }
        }

        int bodyStart = text.indexOf('\n', start);
        int end = bodyStart < 0 ? text.length() : bodyStart + 1;
        int nextSection = text.indexOf("\n[", end);
        if (nextSection >= 0) {
            end = nextSection + 1;
        } else {
            end = text.length();
        }

        String updated = (text.substring(0, sectionStart) + text.substring(end)).trim();
        try {
            if (updated.isEmpty()) {
                Files.deleteIfExists(path);
            } else {
                Files.writeString(path, updated + System.lineSeparator(), StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
    }

    private static String readText(Path path) {
        if (!Files.exists(path)) {
            return null;
        }

        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static void migrateMergedSections(Path configDir, String targetFileName, List<String> legacyFileNames) {
        Path targetPath = configDir.resolve(targetFileName);
        String targetText = readText(targetPath);
        List<Path> migratedPaths = new ArrayList<>();

        for (String legacyFileName : legacyFileNames) {
            Path legacyPath = configDir.resolve(legacyFileName);
            if (!Files.exists(legacyPath)) {
                continue;
            }

            String legacyText = readText(legacyPath);
            if (legacyText == null) {
                continue;
            }

            if (!legacyText.isBlank()) {
                List<SectionBlock> sourceBlocks = extractTopLevelSectionBlocks(legacyText);
                if (sourceBlocks.isEmpty()) {
                    targetText = appendBlock(targetText, legacyText.trim());
                } else {
                    targetText = mergeSectionBlocks(targetText, sourceBlocks);
                }
            }

            migratedPaths.add(legacyPath);
        }

        if (migratedPaths.isEmpty()) {
            return;
        }

        try {
            Files.createDirectories(configDir);
            if (targetText != null && !targetText.isBlank()) {
                Files.writeString(targetPath, normalizeDocument(targetText), StandardCharsets.UTF_8);
            }
            for (Path migratedPath : migratedPaths) {
                deleteIfExists(migratedPath);
            }
        } catch (IOException ignored) {
        }
    }

    private static void migrateCopy(Path configDir, String targetFileName, String legacyFileName) {
        Path targetPath = configDir.resolve(targetFileName);
        Path legacyPath = configDir.resolve(legacyFileName);
        if (Files.exists(targetPath)) {
            deleteIfExists(legacyPath);
            return;
        }
        if (!Files.exists(legacyPath)) {
            return;
        }

        try {
            Files.createDirectories(configDir);
            Files.copy(legacyPath, targetPath);
            deleteIfExists(legacyPath);
        } catch (IOException ignored) {
        }
    }

    private static String mergeSectionBlocks(String targetText, List<SectionBlock> sourceBlocks) {
        String existingText = targetText == null ? "" : normalizeNewlines(targetText);
        List<SectionBlock> targetBlocks = extractTopLevelSectionBlocks(existingText);
        LinkedHashMap<String, String> blocksByHeader = new LinkedHashMap<>();

        for (SectionBlock targetBlock : targetBlocks) {
            blocksByHeader.put(targetBlock.header, targetBlock.content);
        }
        for (SectionBlock sourceBlock : sourceBlocks) {
            blocksByHeader.put(sourceBlock.header, sourceBlock.content);
        }

        String preamble = extractPreamble(existingText, targetBlocks);
        StringBuilder rebuilt = new StringBuilder();
        if (!preamble.isBlank()) {
            rebuilt.append(preamble.stripTrailing());
        }
        for (String block : blocksByHeader.values()) {
            if (!rebuilt.isEmpty()) {
                rebuilt.append("\n\n");
            }
            rebuilt.append(block.stripTrailing());
        }

        return rebuilt.toString();
    }

    private static String appendBlock(String targetText, String blockText) {
        if (targetText == null || targetText.isBlank()) {
            return blockText;
        }

        return normalizeNewlines(targetText).stripTrailing() + "\n\n" + blockText.stripTrailing();
    }

    private static String extractPreamble(String text, List<SectionBlock> blocks) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (blocks.isEmpty()) {
            return normalizeNewlines(text).stripTrailing();
        }

        String[] lines = normalizeNewlines(text).split("\n", -1);
        StringBuilder preamble = new StringBuilder();
        for (int i = 0; i < blocks.get(0).startLine; i++) {
            if (!preamble.isEmpty()) {
                preamble.append('\n');
            }
            preamble.append(lines[i]);
        }
        return preamble.toString().stripTrailing();
    }

    private static List<SectionBlock> extractTopLevelSectionBlocks(String text) {
        String normalized = normalizeNewlines(text);
        String[] lines = normalized.split("\n", -1);
        List<Integer> headerLines = new ArrayList<>();
        List<Integer> blockStarts = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            if (!isTopLevelHeader(lines[i].trim())) {
                continue;
            }

            headerLines.add(i);
            int blockStart = i;
            while (blockStart > 0) {
                String previous = lines[blockStart - 1].trim();
                if (previous.isEmpty() || previous.startsWith("#")) {
                    blockStart--;
                    continue;
                }
                break;
            }
            blockStarts.add(blockStart);
        }

        List<SectionBlock> blocks = new ArrayList<>();
        for (int i = 0; i < headerLines.size(); i++) {
            int startLine = blockStarts.get(i);
            int headerLine = headerLines.get(i);
            int endLine = (i + 1 < blockStarts.size()) ? blockStarts.get(i + 1) - 1 : lines.length - 1;
            while (endLine >= startLine && lines[endLine].trim().isEmpty()) {
                endLine--;
            }
            if (endLine < startLine) {
                continue;
            }

            StringBuilder content = new StringBuilder();
            for (int lineIndex = startLine; lineIndex <= endLine; lineIndex++) {
                if (!content.isEmpty()) {
                    content.append('\n');
                }
                content.append(lines[lineIndex]);
            }
            blocks.add(new SectionBlock(lines[headerLine].trim(), startLine, content.toString()));
        }

        return blocks;
    }

    private static boolean isTopLevelHeader(String line) {
        return line.startsWith("[")
                && line.endsWith("]")
                && !line.startsWith("[[")
                && !line.endsWith("]]")
                && line.indexOf('.') < 0;
    }

    private static String normalizeDocument(String text) {
        return normalizeNewlines(text).stripTrailing() + System.lineSeparator();
    }

    private static String normalizeNewlines(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private static final class SectionBlock {
        private final String header;
        private final int startLine;
        private final String content;

        private SectionBlock(String header, int startLine, String content) {
            this.header = header;
            this.startLine = startLine;
            this.content = content;
        }
    }
}
