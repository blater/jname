// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JnameGeneratorTest {
    @TempDir
    Path words;

    @Test
    void followsTheUpstreamWordGrammar() throws IOException {
        writeWords(words, "swiftly", "calm", "otter");
        var generator = JnameGenerator.seeded(1);

        assertEquals("otter", generator.generate(options(1, "-")));
        assertEquals("calm-otter", generator.generate(options(2, "-")));
        assertEquals("swiftly_calm_otter", generator.generate(options(3, "_")));
        assertEquals("swiftly:swiftly:calm:otter", generator.generate(options(4, ":")));
    }

    @Test
    void filtersByUnicodeCodePointLength() throws IOException {
        writeWords(words, "aptly\npatiently", "calm\ntranquil", "ibis\nalpaca");
        var options = JnameOptions.builder()
                .wordDirectory(words)
                .words(3)
                .maxLetters(5)
                .build();

        assertEquals("aptly-calm-ibis", JnameGenerator.seeded(1).generate(options));
    }

    @Test
    void generatesTrueAlliteration() throws IOException {
        writeWords(words, "ably\nboldly", "agile\nbrisk", "alpaca\nbadger");
        var options = JnameOptions.builder()
                .wordDirectory(words)
                .words(4)
                .alliterate(true)
                .build();

        var generated = JnameGenerator.seeded(7).generate(options);
        var parts = generated.split("-");
        assertEquals(4, parts.length);
        assertTrue(java.util.Arrays.stream(parts)
                .allMatch(part -> part.codePointAt(0) == parts[0].codePointAt(0)));
    }

    @Test
    void supportsIndividualWordTypes() throws IOException {
        writeWords(words, "swiftly", "calm", "otter");
        var generator = JnameGenerator.seeded(1);

        assertEquals("swiftly", generator.generate(single(JnameType.ADVERB)));
        assertEquals("calm", generator.generate(single(JnameType.ADJECTIVE)));
        assertEquals("otter", generator.generate(single(JnameType.NAME)));
    }

    @Test
    void resolvesComplexityBelowCustomDirectory() throws IOException {
        writeWords(words.resolve("small"), "aptly", "calm", "ibis");
        var options = JnameOptions.builder()
                .wordDirectory(words)
                .complexity(Complexity.SMALL)
                .build();

        assertEquals("calm-ibis", JnameGenerator.seeded(1).generate(options));
    }

    @Test
    void rejectsImpossibleConfigurations() throws IOException {
        writeWords(words, "swiftly", "calm", "otter");
        var automaticWords = options(0, "-");
        var negativeWords = options(-1, "-");
        var tooShort = options(2, "-").toBuilder().maxLetters(2).build();

        assertEquals(2, JnameGenerator.seeded(1).generate(automaticWords).split("-").length);
        assertThrows(IllegalArgumentException.class, () -> Jname.generate(negativeWords));
        assertThrows(IllegalArgumentException.class, () -> Jname.generate(0));
        assertThrows(JnameException.class, () -> Jname.generate(tooShort));
    }

    private JnameOptions options(int count, String separator) {
        return JnameOptions.builder()
                .wordDirectory(words)
                .words(count)
                .separator(separator)
                .build();
    }

    private JnameOptions single(JnameType type) {
        return JnameOptions.builder().wordDirectory(words).type(type).build();
    }

    static void writeWords(Path directory, String adverbs, String adjectives, String names) throws IOException {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("adverbs.txt"), adverbs + "\n");
        Files.writeString(directory.resolve("adjectives.txt"), adjectives + "\n");
        Files.writeString(directory.resolve("names.txt"), names + "\n");
    }
}
