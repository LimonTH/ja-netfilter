package com.janetfilter.core.commons;

import com.janetfilter.core.models.FilterRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigParser.
 */
public class ConfigParserTest {

    @TempDir
    Path tempDir;

    @Test
    public void testParseShouldReturnEmptyMapForNonExistentFile() throws Exception {
        File nonExistent = tempDir.resolve("nonexistent.conf").toFile();
        Map<String, List<FilterRule>> result = ConfigParser.parse(nonExistent);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseShouldReturnEmptyMapForNullFile() throws Exception {
        Map<String, List<FilterRule>> result = ConfigParser.parse(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseShouldReadSectionsAndRules() throws Exception {
        File config = tempDir.resolve("test.conf").toFile();
        try (FileWriter writer = new FileWriter(config)) {
            writer.write("[section1]\n");
            writer.write("PREFIX,com.example.\n");
            writer.write("SUFFIX,.class\n");
            writer.write("[section2]\n");
            writer.write("EQUAL,exact_match\n");
        }

        Map<String, List<FilterRule>> result = ConfigParser.parse(config);
        assertEquals(2, result.size());

        List<FilterRule> section1 = result.get("section1");
        assertNotNull(section1);
        assertEquals(2, section1.size());
        assertEquals("PREFIX", section1.get(0).getType().name());
        assertEquals("com.example.", section1.get(0).getRule());
        assertEquals("SUFFIX", section1.get(1).getType().name());
        assertEquals(".class", section1.get(1).getRule());

        List<FilterRule> section2 = result.get("section2");
        assertNotNull(section2);
        assertEquals(1, section2.size());
        assertEquals("EQUAL", section2.get(0).getType().name());
        assertEquals("exact_match", section2.get(0).getRule());
    }

    @Test
    public void testParseShouldSkipComments() throws Exception {
        File config = tempDir.resolve("comments.conf").toFile();
        try (FileWriter writer = new FileWriter(config)) {
            writer.write("# This is a comment\n");
            writer.write("; This is also a comment\n");
            writer.write("// And this too\n");
            writer.write("[section]\n");
            writer.write("PREFIX,com.example.\n");
        }

        Map<String, List<FilterRule>> result = ConfigParser.parse(config);
        assertEquals(1, result.size());
        assertEquals(1, result.get("section").size());
    }

    @Test
    public void testParseShouldSkipEmptyLines() throws Exception {
        File config = tempDir.resolve("empty.conf").toFile();
        try (FileWriter writer = new FileWriter(config)) {
            writer.write("\n\n");
            writer.write("[section]\n");
            writer.write("\n");
            writer.write("PREFIX,com.example.\n");
        }

        Map<String, List<FilterRule>> result = ConfigParser.parse(config);
        assertEquals(1, result.size());
        assertEquals(1, result.get("section").size());
    }

    @Test
    public void testParseShouldThrowOnInvalidSection() throws Exception {
        File config = tempDir.resolve("invalid.conf").toFile();
        try (FileWriter writer = new FileWriter(config)) {
            writer.write("[invalid\n");
        }

        assertThrows(Exception.class, () -> ConfigParser.parse(config));
    }

    @Test
    public void testParseShouldThrowOnInvalidRule() throws Exception {
        File config = tempDir.resolve("invalid_rule.conf").toFile();
        try (FileWriter writer = new FileWriter(config)) {
            writer.write("[section]\n");
            writer.write("INVALID_RULE\n");
        }

        assertThrows(Exception.class, () -> ConfigParser.parse(config));
    }
}