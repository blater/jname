// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JnameDictionaryTest {
    @TempDir
    Path directory;

    @Test
    void builtInTolkienUsesThemedNamesAndMergesSmallModifiers() {
        var small = WordListLoader.load(JnameOptions.builder().complexity(Complexity.SMALL).build());
        var tolkien = WordListLoader.load(JnameOptions.builder().strategy(JnameStrategy.TOLKIEN).build());

        assertTrue(tolkien.getAdverbs().containsAll(small.getAdverbs()));
        assertTrue(tolkien.getAdjectives().containsAll(small.getAdjectives()));
        assertEquals(
                readResourceLines("/blater/jname/words/tolkien/names.txt"),
                tolkien.getNames());
        assertCaseInsensitiveSorted(tolkien.getAdverbs());
        assertCaseInsensitiveSorted(tolkien.getAdjectives());
    }

    @Test
    void modifierMergeDeduplicatesExactStringsAndUsesStableCaseInsensitiveOrder() {
        var merged = WordListLoader.mergeWords(
                List.of("Alpha", "same", "alpha", "small"),
                List.of("ALPHA", "same", "New"));

        assertEquals(List.of("ALPHA", "Alpha", "alpha", "New", "same", "small"), merged);
    }

    @Test
    void customTolkienDirectoryIsLoadedCompleteWithoutBundledModifiers() throws IOException {
        writeWords(directory.resolve("tolkien"), "eagerly", "eldritch", "Earendil");
        var options = JnameOptions.builder()
                .strategy(JnameStrategy.TOLKIEN)
                .wordDirectory(directory)
                .build();

        var loaded = WordListLoader.load(options);
        assertEquals(List.of("eagerly"), loaded.getAdverbs());
        assertEquals(List.of("eldritch"), loaded.getAdjectives());
        assertEquals(List.of("Earendil"), loaded.getNames());
    }

    @Test
    void defaultCasingIsLocaleIndependentAndMixedCasePreservesSource() throws IOException {
        var originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            writeWords(directory.resolve("tolkien"), "IRISLY", "ionic", "Isildur");
            var defaultOptions = JnameOptions.builder()
                    .strategy(JnameStrategy.TOLKIEN)
                    .wordDirectory(directory)
                    .words(3)
                    .alliterate(true)
                    .build();
            var mixedOptions = defaultOptions.toBuilder().mixedCase(true).build();
            var generator = JnameGenerator.seeded(1);

            assertEquals("irisly-ionic-isildur", generator.generate(defaultOptions));
            assertEquals("IRISLY-ionic-Isildur", generator.generate(mixedOptions));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void alliterationMatchesInitialCodePointsRegardlessOfSourceCase() {
        var alliteration = new JnameAlliteration(new JnameWordSelector(new Random(1)));

        assertEquals(List.of(List.of("Apple"), List.of("apple")),
                alliteration.select(List.of(List.of("Apple"), List.of("apple"))));
        assertEquals(List.of(List.of("Ωmega"), List.of("ωmega")),
                alliteration.select(List.of(List.of("Ωmega"), List.of("ωmega"))));
    }

    private static void assertCaseInsensitiveSorted(List<String> words) {
        for (var index = 1; index < words.size(); index++) {
            var previous = words.get(index - 1);
            var current = words.get(index);
            var comparison = previous.toLowerCase(Locale.ROOT).compareTo(current.toLowerCase(Locale.ROOT));
            assertTrue(comparison < 0 || comparison == 0 && previous.compareTo(current) <= 0,
                    previous + " should sort before " + current);
        }
    }

    private static List<String> readResourceLines(String resource) {
        try (var input = JnameDictionaryTest.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new AssertionError("missing resource: " + resource);
            }
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines().toList();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void writeWords(Path directory, String adverbs, String adjectives, String names)
            throws IOException {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("adverbs.txt"), adverbs + "\n");
        Files.writeString(directory.resolve("adjectives.txt"), adjectives + "\n");
        Files.writeString(directory.resolve("names.txt"), names + "\n");
    }
}
