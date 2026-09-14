// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

final class JnameAlliteration {
    private final JnameWordSelector selector;

    JnameAlliteration(JnameWordSelector selector) {
        this.selector = selector;
    }

    List<List<String>> select(List<List<String>> categories) {
        var initial = selectInitial(categories);
        var matches = new IdentityHashMap<List<String>, List<String>>();
        return categories.stream().map(category -> matches.computeIfAbsent(category,
                words -> beginningWith(words, initial))).toList();
    }

    private int selectInitial(List<List<String>> categories) {
        var common = initials(categories.getFirst());
        categories.stream().skip(1).map(this::initials).forEach(common::retainAll);
        requireCommonInitial(common);
        return selector.choose(new ArrayList<>(common));
    }

    private TreeSet<Integer> initials(List<String> words) {
        return words.stream().map(word -> caseFold(word.codePointAt(0)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private void requireCommonInitial(TreeSet<Integer> initials) {
        if (initials.isEmpty()) {
            throw new JnameException("no alliterative jname can be generated from the eligible words");
        }
    }

    private List<String> beginningWith(List<String> words, int initial) {
        return words.stream().filter(word -> caseFold(word.codePointAt(0)) == initial).toList();
    }

    private int caseFold(int codePoint) {
        return Character.toLowerCase(codePoint);
    }
}
