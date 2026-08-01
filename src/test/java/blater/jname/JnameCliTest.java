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
        var result = run("--complexity", "7");

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("complexity must be 0"));
    }

    @Test
    void printsSelfContainedHelp() {
        var result = run("--help");

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("--ubuntu"));
        assertTrue(result.stdout().contains("--adverb"));
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

    @Value
    @Accessors(fluent = true)
    private static class Result {
        int exitCode;
        String stdout;
        String stderr;
    }
}
