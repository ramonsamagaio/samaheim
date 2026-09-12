package com.samaheim.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SaveFileRecoveryTest {
    @TempDir
    Path tempDir;

    @Test
    void secondAtomicSaveKeepsPreviousPrimaryAsBackup() throws Exception {
        Path save = tempDir.resolve("cave-save.properties");
        Properties first = new Properties();
        first.setProperty("kills", "7");
        SaveFileRecovery.storeAtomic(first, save, "test");
        assertFalse(Files.exists(SaveFileRecovery.backupPath(save)));

        Properties second = new Properties();
        second.setProperty("kills", "11");
        SaveFileRecovery.storeAtomic(second, save, "test");

        assertEquals("11", SaveFileRecovery.loadProperties(save).getProperty("kills"));
        assertEquals("7", SaveFileRecovery.loadProperties(SaveFileRecovery.backupPath(save)).getProperty("kills"));
    }

    @Test
    void backupCanRestorePrimaryAfterCorruption() throws Exception {
        Path save = tempDir.resolve("save.properties");
        Properties good = new Properties();
        good.setProperty("seed", "42");
        SaveFileRecovery.storeAtomic(good, save, "test");
        SaveFileRecovery.storeAtomic(good, save, "test");

        Files.writeString(save, "seed=not-a-number\n");
        SaveFileRecovery.restoreBackup(save);

        assertEquals("42", SaveFileRecovery.loadProperties(save).getProperty("seed"));
        assertTrue(Files.isRegularFile(SaveFileRecovery.backupPath(save)));
    }

    @Test
    void candidateOrderAlwaysPrefersPrimaryThenBackup() {
        Path save = tempDir.resolve("world.properties");
        assertEquals(save, SaveFileRecovery.candidates(save).get(0));
        assertEquals(SaveFileRecovery.backupPath(save), SaveFileRecovery.candidates(save).get(1));
    }
}
