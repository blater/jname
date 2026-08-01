// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
class WordListLoader {
    private final String RESOURCE_ROOT = "/blater/jname/words/";
    private final Map<Complexity, WordLists> BUILT_INS = new ConcurrentHashMap<>();

    WordLists load(JnameOptions options) {
        if (options.getWordDirectory() == null) {
            var complexity = options.getComplexity() == Complexity.DEFAULT
                    ? Complexity.MEDIUM
                    : options.getComplexity();
            return BUILT_INS.computeIfAbsent(complexity, WordListLoader::loadBuiltIn);
        }

        var directory = options.getWordDirectory().toAbsolutePath().normalize();
        if (options.getComplexity() != Complexity.DEFAULT) {
            directory = directory.resolve(options.getComplexity().getDirectory());
        }
        return loadDirectory(directory);
    }

    private WordLists loadBuiltIn(Complexity complexity) {
        var root = RESOURCE_ROOT + complexity.getDirectory() + "/";
        return new WordLists(
                readResource(root + "adverbs.txt"),
                readResource(root + "adjectives.txt"),
                readResource(root + "names.txt"));
    }

    private WordLists loadDirectory(Path directory) {
        if (!Files.isDirectory(directory) || !Files.isReadable(directory)) {
            throw new JnameException("word directory does not exist or is not readable: " + directory);
        }
        return new WordLists(
                readFile(directory.resolve("adverbs.txt")),
                readFile(directory.resolve("adjectives.txt")),
                readFile(directory.resolve("names.txt")));
    }

    private List<String> readResource(String resource) {
        try (var input = WordListLoader.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new JnameException("missing built-in word list: " + resource);
            }
            return readLines(input);
        } catch (IOException e) {
            throw new JnameException("cannot read built-in word list: " + resource, e);
        }
    }

    private List<String> readFile(Path file) {
        try (var input = Files.newInputStream(file)) {
            return readLines(input);
        } catch (IOException e) {
            throw new JnameException("cannot read word list: " + file, e);
        }
    }

    private List<String> readLines(InputStream input) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            var lines = reader.lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty())
                    .toList();
            if (lines.isEmpty()) {
                throw new JnameException("word list is empty");
            }
            return lines;
        }
    }
}
