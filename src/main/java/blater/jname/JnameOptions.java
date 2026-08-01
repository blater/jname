// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.nio.file.Path;

/** Immutable input to {@link JnameGenerator}. */
@Value
@Builder(toBuilder = true)
public class JnameOptions {
    /** Number of words in a complete jname. */
    @Builder.Default
    int words = 2;

    /** Maximum Unicode code points per word, or zero for unlimited. */
    @Builder.Default
    int maxLetters = 0;

    /** Text inserted between words. */
    @NonNull
    @Builder.Default
    String separator = "-";

    /** Built-in dictionary tier, or subdirectory tier for custom words. */
    @NonNull
    @Builder.Default
    Complexity complexity = Complexity.DEFAULT;

    /** Whether every generated word must begin with the same letter. */
    boolean alliterate;

    /** Complete jname or a single word category. */
    @NonNull
    @Builder.Default
    JnameType type = JnameType.JNAME;

    /** Optional directory containing adverbs.txt, adjectives.txt, and names.txt. */
    Path wordDirectory;

    public static JnameOptions defaults() {
        return builder().build();
    }
}
