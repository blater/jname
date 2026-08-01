// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/** Generates jnames using a configurable source of randomness. */
@RequiredArgsConstructor
public final class JnameGenerator {
    @NonNull
    private final RandomGenerator random;

    public JnameGenerator() {
        this(new SecureRandom());
    }

    /** Returns a deterministic generator, useful for repeatable workloads and tests. */
    public static JnameGenerator seeded(long seed) {
        return new JnameGenerator(new Random(seed));
    }

    public String generate(@NonNull JnameOptions options) {
        validate(options);
        var words = WordListLoader.load(options);
        return switch (options.getType()) {
            case ADVERB -> choose(eligible(words.getAdverbs(), options.getMaxLetters()));
            case ADJECTIVE -> choose(eligible(words.getAdjectives(), options.getMaxLetters()));
            case NAME -> choose(eligible(words.getNames(), options.getMaxLetters()));
            case JNAME -> generateJname(words, options);
        };
    }

    private String generateJname(WordLists words, JnameOptions options) {
        var categories = new ArrayList<List<String>>(options.getWords());
        var adverbs = options.getWords() > 2
                ? eligible(words.getAdverbs(), options.getMaxLetters())
                : List.<String>of();
        var adjectives = options.getWords() > 1
                ? eligible(words.getAdjectives(), options.getMaxLetters())
                : List.<String>of();
        var names = eligible(words.getNames(), options.getMaxLetters());

        for (var i = 2; i < options.getWords(); i++) {
            categories.add(adverbs);
        }
        if (options.getWords() > 1) {
            categories.add(adjectives);
        }
        categories.add(names);

        Integer initial = options.isAlliterate() ? chooseInitial(categories) : null;
        var alliterativeWords = new IdentityHashMap<List<String>, List<String>>();
        return categories.stream()
                .map(category -> choose(initial == null
                        ? category
                        : alliterativeWords.computeIfAbsent(category, wordsForInitial ->
                                beginningWith(wordsForInitial, initial))))
                .collect(Collectors.joining(options.getSeparator()));
    }

    private List<String> eligible(List<String> words, int maxLetters) {
        if (maxLetters == 0) {
            return words;
        }
        var eligible = words.stream()
                .filter(word -> word.codePointCount(0, word.length()) <= maxLetters)
                .toList();
        if (eligible.isEmpty()) {
            throw new JnameException("no words satisfy the maximum length of " + maxLetters);
        }
        return eligible;
    }

    private int chooseInitial(List<List<String>> categories) {
        TreeSet<Integer> common = null;
        for (var category : categories) {
            var initials = category.stream()
                    .map(word -> word.codePointAt(0))
                    .collect(Collectors.toCollection(TreeSet::new));
            if (common == null) {
                common = initials;
            } else {
                common.retainAll(initials);
            }
        }
        if (common == null || common.isEmpty()) {
            throw new JnameException("no alliterative jname can be generated from the eligible words");
        }
        return choose(new ArrayList<>(common));
    }

    private List<String> beginningWith(List<String> words, int initial) {
        return words.stream().filter(word -> word.codePointAt(0) == initial).toList();
    }

    private <T> T choose(List<T> candidates) {
        if (candidates.isEmpty()) {
            throw new JnameException("no eligible words");
        }
        synchronized (random) {
            return candidates.get(random.nextInt(candidates.size()));
        }
    }

    private void validate(JnameOptions options) {
        if (options.getWords() < 1) {
            throw new IllegalArgumentException("words must be a positive integer");
        }
        if (options.getMaxLetters() < 0) {
            throw new IllegalArgumentException("maxLetters must be zero or a positive integer");
        }
        if (options.getSeparator().codePointCount(0, options.getSeparator().length()) > 100) {
            throw new IllegalArgumentException("separator must be 100 characters or less");
        }
    }
}
