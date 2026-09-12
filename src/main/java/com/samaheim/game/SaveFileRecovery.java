package com.samaheim.game;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;

/** Crash-resistant properties save I/O with one known-good previous-file fallback. */
public final class SaveFileRecovery {
    private SaveFileRecovery() { }

    public static Path backupPath(Path primary) {
        return primary.resolveSibling(primary.getFileName() + ".bak");
    }

    public static List<Path> candidates(Path primary) {
        return List.of(primary, backupPath(primary));
    }

    public static Properties loadProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        return properties;
    }

    public static void storeAtomic(Properties properties, Path primary, String comment) throws IOException {
        Path parent = primary.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temp = primary.resolveSibling(primary.getFileName() + ".tmp");
        Path backup = backupPath(primary);

        try (OutputStream output = Files.newOutputStream(temp)) {
            properties.store(output, comment);
        }

        if (Files.isRegularFile(primary)) {
            Files.copy(primary, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }
        moveReplace(temp, primary);
    }

    public static void restoreBackup(Path primary) throws IOException {
        Path backup = backupPath(primary);
        if (!Files.isRegularFile(backup)) throw new IOException("No backup save exists");
        Path temp = primary.resolveSibling(primary.getFileName() + ".recovering");
        Files.copy(backup, temp, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        moveReplace(temp, primary);
    }

    private static void moveReplace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
