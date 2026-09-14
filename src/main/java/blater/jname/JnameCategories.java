// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import java.util.ArrayList;
import java.util.List;

final class JnameCategories {
    private final JnameWordSelector selector;

    JnameCategories(JnameWordSelector selector) {
        this.selector = selector;
    }

    List<List<String>> create(WordLists words, JnameOptions options) {
        var categories = new ArrayList<List<String>>(options.getWords());
        addAdverbs(categories, words, options);
        addAdjective(categories, words, options);
        categories.add(selector.eligible(words.getNames(), options.getMaxLetters()));
        return categories;
    }

    private void addAdverbs(List<List<String>> categories, WordLists words, JnameOptions options) {
        if (options.getWords() > 2) {
            addRepeated(categories, selector.eligible(words.getAdverbs(), options.getMaxLetters()), options.getWords() - 2);
        }
    }

    private void addAdjective(List<List<String>> categories, WordLists words, JnameOptions options) {
        if (options.getWords() > 1) {
            categories.add(selector.eligible(words.getAdjectives(), options.getMaxLetters()));
        }
    }

    private void addRepeated(List<List<String>> categories, List<String> words, int count) {
        for (var index = 0; index < count; index++) {
            categories.add(words);
        }
    }
}
