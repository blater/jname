// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import java.util.Locale;

final class JnameComposer {
    private final JnameWordSelector selector;

    JnameComposer(JnameWordSelector selector) {
        this.selector = selector;
    }

    String compose(WordLists words, JnameOptions options) {
        var categories = new JnameCategories(selector).create(words, options);
        var selected = options.isAlliterate()
                ? new JnameAlliteration(selector).select(categories)
                : categories;
        return selected.stream().map(selector::choose).map(word -> caseWord(word, options))
                .reduce((left, right) -> left + options.getSeparator() + right).orElseThrow();
    }

    static String caseWord(String word, JnameOptions options) {
        return options.isMixedCase() ? word : word.toLowerCase(Locale.ROOT);
    }
}
