// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import lombok.Value;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JnameCliTest {
    @TempDir
    Path words;

    @Test
    void supportsLongOptions() throws IOException {
        JnameGeneratorTest.writeWords(words, "swiftly", "calm", "otter");

        var result = run("--words", "3", "--separator", ":", "--dir", words.toString());

        assertEquals(0, result.exitCode());
        assertEquals("swiftly:calm:otter\n", result.stdout());
        assertEquals("", result.stderr());
    }

    @Test
    void supportsShortOptionsAndAnEmptySeparator() throws IOException {
        JnameGeneratorTest.writeWords(words, "swiftly", "calm", "otter");

        var result = run("-w", "2", "-s", "", "-d", words.toString());

        assertEquals(0, result.exitCode());
        assertEquals("calmotter\n", result.stdout());
    }

    @Test
    void supportsHiddenSingleWordOptionsWithUpstreamPrecedence() throws IOException {
        JnameGeneratorTest.writeWords(words, "swiftly", "calm", "otter");

        var result = run("--adverb", "--adjective", "--name", "-d", words.toString());

        assertEquals(0, result.exitCode());
        assertEquals("otter\n", result.stdout());
    }

    @Test
    void supportsLengthComplexityAndUbuntuOptions() throws IOException {
        JnameGeneratorTest.writeWords(
                words.resolve("small"),
                "ably\nboldly",
                "agile\nbrisk",
                "alpaca\nbadger");

        var result = run("-d", words.toString(), "-c", "0", "-l", "6", "-u");

        assertEquals(0, result.exitCode());
        var parts = result.stdout().strip().split("-");
        assertEquals(2, parts.length);
        assertEquals(parts[0].codePointAt(0), parts[1].codePointAt(0));
        assertTrue(java.util.Arrays.stream(parts).allMatch(part -> part.length() <= 6));
    }

    @Test
    void reportsInvalidArguments() {
        for (var test : new InvalidCase[] {
                new InvalidCase(new String[] {"--complexity", "7"}, "complexity must be 0"),
                new InvalidCase(new String[] {"--words", "2147483648"}, "words is too large"),
                new InvalidCase(new String[] {"--letters", "-1"}, "letters must be a positive integer"),
                new InvalidCase(new String[] {"--separator"}, "missing value for --separator"),
                new InvalidCase(new String[] {"--unknown"}, "Unknown options [--unknown]")
        }) {
            var result = run(test.arguments());
            assertEquals(1, result.exitCode(), result.stderr());
            assertTrue(result.stderr().startsWith("ERROR: "), result.stderr());
            assertTrue(result.stderr().contains(test.message()), result.stderr());
        }
    }

    @Test
    void printsSelfContainedHelp() {
        var result = run("--bad", "--help");

        assertEquals(0, result.exitCode());
        assertEquals("", result.stderr());
        assertTrue(result.stdout().contains("Usage: jname"));
        assertTrue(result.stdout().contains("--ubuntu"));
        assertTrue(result.stdout().contains("--adverb"));
        assertTrue(result.stdout().contains("--strategy"));
        assertTrue(result.stdout().contains("--prefix"));
        assertTrue(result.stdout().contains("--mixedcase"));
    }

    @Test
    void supportsNewOptionAliasesAndLeavesOmittedLengthsAtTheApiDefault() {
        var options = JnameCliParser.parse(new String[] {
                "-t", "base32", "-p", "ticket", "-m"
        });

        assertEquals(JnameStrategy.BASE32, options.getStrategy());
        assertEquals("ticket", options.getPrefix());
        assertTrue(options.isMixedCase());
        assertEquals(0, options.getMaxLetters());
    }

    @Test
    void clampsEveryExplicitCliLengthToAtLeastThree() {
        for (var letters : new String[] {"0", "1", "2", "3"}) {
            var options = JnameCliParser.parse(new String[] {"--letters", letters});
            assertEquals(3, options.getMaxLetters(), "--letters " + letters);
        }

        assertEquals(0, JnameCliParser.parse(new String[0]).getMaxLetters());
    }

    @Test
    void supportsTolkienStrategyWithCustomTolkienDirectoryAndVerbatimPrefix() throws IOException {
        var tolkien = words.resolve("tolkien");
        JnameGeneratorTest.writeWords(tolkien, "swiftly", "Ancient", "Aragorn");

        var result = run("--strategy", "tolkien", "--dir", words.toString(),
                "--prefix", "Ticket", "--separator", "_", "--mixedcase");

        assertEquals(0, result.exitCode());
        assertEquals("Ticket_Ancient_Aragorn\n", result.stdout());
        assertEquals("", result.stderr());

        result = run("--strategy", "tolkien", "--dir", words.toString(),
                "--prefix", "Ticket", "--separator", "_");
        assertEquals(0, result.exitCode());
        assertEquals("Ticket_ancient_aragorn\n", result.stdout());
    }

    @Test
    void supportsTokenStrategiesDefaultCountsWidthsPrefixesAndCasing() {
        for (var test : new TokenCase[] {
                new TokenCase("hex", 4, "[0-9a-f]{4}", "[0-9A-F]{4}"),
                new TokenCase("base32", 4,
                        "[0-9abcdefghjkmnpqrstvwxyz]{4}", "[0-9ABCDEFGHJKMNPQRSTVWXYZ]{4}"),
                new TokenCase("ulid", 26,
                        "[0-7][0-9abcdefghjkmnpqrstvwxyz]{25}",
                        "[0-7][0-9ABCDEFGHJKMNPQRSTVWXYZ]{25}")
        }) {
            var result = run("--strategy", test.strategy());
            assertEquals(0, result.exitCode(), test.strategy());
            assertEquals(test.width(), result.stdout().strip().length(), test.strategy());
            assertTrue(result.stdout().strip().matches(test.lowerPattern()), result.stdout());

            result = run("-t", test.strategy(), "--words", "2", "-s", ":", "-p", "Job",
                    "--mixedcase", "--name");
            assertEquals(0, result.exitCode(), test.strategy());
            var parts = result.stdout().strip().split(":", -1);
            assertEquals(3, parts.length, result.stdout());
            assertEquals("Job", parts[0]);
            assertTrue(parts[1].matches(test.upperPattern()), parts[1]);
            assertTrue(parts[2].matches(test.upperPattern()), parts[2]);
            assertEquals("", result.stderr());
        }
    }

    @Test
    void cliTokenWidthsClampZeroOneAndTwoButUlidAlwaysRemainsFixedWidth() {
        for (var strategy : new String[] {"hex", "base32"}) {
            for (var letters : new String[] {"0", "1", "2", "3"}) {
                var result = run("--strategy", strategy, "--letters", letters);
                assertEquals(0, result.exitCode(), result.stderr());
                assertEquals(3, result.stdout().strip().length(), strategy + " --letters " + letters);
            }
        }

        for (var letters : new String[] {"0", "1", "2", "3"}) {
            var result = run("--strategy", "ulid", "--letters", letters);
            assertEquals(0, result.exitCode(), result.stderr());
            assertEquals(26, result.stdout().strip().length(), "ulid --letters " + letters);
        }
    }

    @Test
    void rejectsUnknownOrMissingStrategyValuesAndComplexityPairings() {
        var unknown = run("--strategy", "Tolkien");
        assertEquals(1, unknown.exitCode());
        assertTrue(unknown.stderr().contains("strategy must be tolkien, hex, base32, or ulid"));

        var missing = run("-t");
        assertEquals(1, missing.exitCode());
        assertTrue(missing.stderr().contains("missing value for -t"));

        var missingPrefix = run("--prefix");
        assertEquals(1, missingPrefix.exitCode());
        assertTrue(missingPrefix.stderr().contains("missing value for --prefix"));

        var zeroWords = run("--words", "0");
        assertEquals(1, zeroWords.exitCode());
        assertTrue(zeroWords.stderr().contains("words must be a positive integer"));

        for (var strategy : new String[] {"tolkien", "hex", "base32", "ulid"}) {
            for (var complexity : new String[] {"0", "1", "2"}) {
                var result = run("--strategy", strategy, "--complexity", complexity);
                assertEquals(1, result.exitCode(), strategy + " + complexity " + complexity);
                assertTrue(result.stderr().contains("cannot be combined with a complexity tier"),
                        result.stderr());
            }
        }
    }

    private Result run(String... args) {
        var stdout = new StringWriter();
        var stderr = new StringWriter();
        var exitCode = new JnameCli().run(
                args,
                new PrintWriter(stdout, true),
                new PrintWriter(stderr, true));
        return new Result(exitCode, stdout.toString(), stderr.toString());
    }

    private record TokenCase(String strategy, int width, String lowerPattern, String upperPattern) {
    }

    private record InvalidCase(String[] arguments, String message) {
    }

    @Value
    @Accessors(fluent = true)
    private static class Result {
        int exitCode;
        String stdout;
        String stderr;
    }
}
