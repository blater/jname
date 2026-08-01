// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import lombok.Value;

import java.util.List;

@Value
class WordLists {
    List<String> adverbs;
    List<String> adjectives;
    List<String> names;

    WordLists(List<String> adverbs, List<String> adjectives, List<String> names) {
        this.adverbs = List.copyOf(adverbs);
        this.adjectives = List.copyOf(adjectives);
        this.names = List.copyOf(names);
    }
}
