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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
class WordListLoader {
    private final String RESOURCE_ROOT = "/blater/jname/words/";
    private final Map<Complexity, WordLists> BUILT_INS = new ConcurrentHashMap<>();
    private final Map<String, WordLists> BUILT_IN_TOLKIEN = new ConcurrentHashMap<>();

    WordLists load(JnameOptions options) {
        if (options.getStrategy() == JnameStrategy.TOLKIEN) {
            if (options.getWordDirectory() == null) {
                return BUILT_IN_TOLKIEN.computeIfAbsent("tolkien", ignored -> loadBuiltInTolkien());
            }
            var directory = options.getWordDirectory().toAbsolutePath().normalize().resolve("tolkien");
            return loadDirectory(directory);
        }

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

    private WordLists loadBuiltInTolkien() {
        var modifiers = loadBuiltIn(Complexity.SMALL);
        var tolkien = loadBuiltInDirectory("tolkien");
        return new WordLists(
                mergeWords(modifiers.getAdverbs(), tolkien.getAdverbs()),
                mergeWords(modifiers.getAdjectives(), tolkien.getAdjectives()),
                tolkien.getNames());
    }

    private WordLists loadBuiltInDirectory(String directory) {
        var root = RESOURCE_ROOT + directory + "/";
        return new WordLists(
                readResource(root + "adverbs.txt"),
                readResource(root + "adjectives.txt"),
                readResource(root + "names.txt"));
    }

    static List<String> mergeWords(List<String> base, List<String> extra) {
        Set<String> seen = new HashSet<>();
        var merged = new ArrayList<String>(base.size() + extra.size());
        for (var source : List.of(base, extra)) {
            for (var word : source) {
                if (seen.add(word)) {
                    merged.add(word);
                }
            }
        }
        merged.sort((left, right) -> {
            var byCaseInsensitive = left.toLowerCase(Locale.ROOT).compareTo(right.toLowerCase(Locale.ROOT));
            return byCaseInsensitive != 0 ? byCaseInsensitive : left.compareTo(right);
        });
        return List.copyOf(merged);
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
