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
    /** Number of words in a complete jname, or zero to use the strategy default. */
    @Builder.Default
    int words = 0;

    /**
     * Maximum Unicode code points per dictionary word. For HEX and BASE32 this is the exact token
     * width (zero selects four); ULID is always 26 characters. Zero is unlimited for dictionary words.
     */
    @Builder.Default
    int maxLetters = 0;

    /** Text inserted between words. */
    @NonNull
    @Builder.Default
    String separator = "-";

    /** Built-in dictionary tier, or subdirectory tier for custom words; only valid with DEFAULT strategy. */
    @NonNull
    @Builder.Default
    Complexity complexity = Complexity.DEFAULT;

    /** Whether every generated dictionary word must begin with the same letter; tokens ignore this. */
    boolean alliterate;

    /** Complete jname or a single word category. */
    @NonNull
    @Builder.Default
    JnameType type = JnameType.JNAME;

    /** Generation algorithm. */
    @NonNull
    @Builder.Default
    JnameStrategy strategy = JnameStrategy.DEFAULT;

    /** Optional verbatim text prepended to the generated result. */
    @NonNull
    @Builder.Default
    String prefix = "";

    /** Preserve source spelling or emit uppercase tokens. */
    boolean mixedCase;

    /** Optional directory containing strategy word lists. */
    Path wordDirectory;

    public static JnameOptions defaults() {
        return builder().build();
    }
}
