package org.onenonly.bitsandbalance.common.recipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class DynamicRecipeDatapackSync {
    private static final String HASH_FILE = ".bitsandbalance-pack.sha256";
    private static final String INPUT_FINGERPRINT_FILE = ".bitsandbalance-input.sha256";

    private DynamicRecipeDatapackSync() {
    }

    public enum Result {
        UNCHANGED,
        WRITTEN,
        REMOVED
    }

    public static Result sync(Path datapackPath, String packMcmeta, Map<String, String> relativeFiles) throws IOException {
        return sync(datapackPath, packMcmeta, relativeFiles, null);
    }

    public static Result sync(Path datapackPath, String packMcmeta, Map<String, String> relativeFiles, String inputFingerprint) throws IOException {
        Map<String, String> allFiles = new LinkedHashMap<>();
        allFiles.put("pack.mcmeta", packMcmeta);
        allFiles.putAll(relativeFiles);

        if (relativeFiles.isEmpty()) {
            if (Files.exists(datapackPath)) {
                deleteRecursively(datapackPath);
                return Result.REMOVED;
            }
            return Result.UNCHANGED;
        }

        String desiredHash = hash(allFiles);
        Path hashPath = datapackPath.resolve(HASH_FILE);
        boolean hasInputFingerprint = inputFingerprint != null && !inputFingerprint.isBlank();
        if (Files.exists(hashPath)) {
            String currentHash = Files.readString(hashPath, StandardCharsets.UTF_8).trim();
            if (desiredHash.equals(currentHash)
                    && hasExactFileSet(datapackPath, allFiles, hasInputFingerprint)
                    && (!hasInputFingerprint || matchesInputFingerprint(datapackPath, inputFingerprint))) {
                return Result.UNCHANGED;
            }
        }

        Files.createDirectories(datapackPath);
        boolean changed = deleteUnexpectedFiles(datapackPath, allFiles, hasInputFingerprint);

        for (Map.Entry<String, String> entry : allFiles.entrySet()) {
            Path file = datapackPath.resolve(entry.getKey());
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            changed |= writeIfChanged(file, entry.getValue());
        }

        changed |= writeIfChanged(hashPath, desiredHash);

        Path inputFingerprintPath = datapackPath.resolve(INPUT_FINGERPRINT_FILE);
        if (hasInputFingerprint) {
            changed |= writeIfChanged(inputFingerprintPath, inputFingerprint);
        } else {
            changed |= Files.deleteIfExists(inputFingerprintPath);
        }

        deleteEmptyDirectories(datapackPath);
        return changed ? Result.WRITTEN : Result.UNCHANGED;
    }

    public static boolean matchesInputFingerprint(Path datapackPath, String inputFingerprint) throws IOException {
        if (inputFingerprint == null || inputFingerprint.isBlank()) {
            return false;
        }

        Path inputFingerprintPath = datapackPath.resolve(INPUT_FINGERPRINT_FILE);
        if (!Files.exists(inputFingerprintPath)) {
            return false;
        }

        return Files.readString(inputFingerprintPath, StandardCharsets.UTF_8).trim().equals(inputFingerprint);
    }

    private static boolean hasExactFileSet(Path datapackPath, Map<String, String> files, boolean hasInputFingerprint) throws IOException {
        if (!Files.isDirectory(datapackPath)) {
            return false;
        }

        var expected = new java.util.HashSet<String>();
        expected.add(HASH_FILE);
        if (hasInputFingerprint) {
            expected.add(INPUT_FINGERPRINT_FILE);
        }
        expected.addAll(files.keySet());

        var actual = new java.util.HashSet<String>();
        try (var stream = Files.walk(datapackPath)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                actual.add(datapackPath.relativize(path).toString().replace('\\', '/'));
            }
        }

        return actual.equals(expected);
    }

    private static boolean deleteUnexpectedFiles(Path datapackPath, Map<String, String> files, boolean hasInputFingerprint) throws IOException {
        if (!Files.isDirectory(datapackPath)) {
            return false;
        }

        var expected = new java.util.HashSet<String>();
        expected.add(HASH_FILE);
        if (hasInputFingerprint) {
            expected.add(INPUT_FINGERPRINT_FILE);
        }
        expected.addAll(files.keySet());

        boolean changed = false;
        try (var stream = Files.walk(datapackPath)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                String relative = datapackPath.relativize(path).toString().replace('\\', '/');
                if (!expected.contains(relative)) {
                    Files.deleteIfExists(path);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean writeIfChanged(Path file, String content) throws IOException {
        if (Files.exists(file)) {
            String current = Files.readString(file, StandardCharsets.UTF_8);
            if (current.equals(content)) {
                return false;
            }
        }

        Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return true;
    }

    private static String hash(Map<String, String> files) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Map.Entry<String, String> entry : new TreeMap<>(files).entrySet()) {
                digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            byte[] bytes = digest.digest();
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(Character.forDigit((value >> 4) & 0xF, 16));
                builder.append(Character.forDigit(value & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static void deleteEmptyDirectories(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }

        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(java.util.Comparator.reverseOrder()).toList()) {
                if (path.equals(root) || !Files.isDirectory(path)) {
                    continue;
                }

                try (var children = Files.list(path)) {
                    if (children.findAny().isEmpty()) {
                        Files.deleteIfExists(path);
                    }
                }
            }
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                for (Path child : stream.toList()) {
                    deleteRecursively(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}