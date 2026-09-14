// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import java.util.List;
import java.util.random.RandomGenerator;

final class JnameWordSelector {
    private final RandomGenerator random;

    JnameWordSelector(RandomGenerator random) {
        this.random = random;
    }

    String chooseEligible(List<String> words, int maxLetters) {
        return choose(eligible(words, maxLetters));
    }

    <T> T choose(List<T> candidates) {
        if (candidates.isEmpty()) {
            throw new JnameException("no eligible words");
        }
        synchronized (random) {
            return candidates.get(random.nextInt(candidates.size()));
        }
    }

    List<String> eligible(List<String> words, int maxLetters) {
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
}
