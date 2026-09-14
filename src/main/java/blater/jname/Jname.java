// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Concise, thread-safe facade for generating jnames with the built-in dictionaries. */
@UtilityClass
public class Jname {
    private final JnameGenerator GENERATOR = new JnameGenerator();

    public String generate() {
        return GENERATOR.generate(JnameOptions.defaults());
    }

    public String generate(int words) {
        return generate(words, "-");
    }

    public String generate(int words, @NonNull String separator) {
        if (words < 1) {
            throw new IllegalArgumentException("words must be a positive integer");
        }
        return GENERATOR.generate(JnameOptions.builder()
                .words(words)
                .separator(separator)
                .build());
    }

    public String generate(@NonNull JnameOptions options) {
        return GENERATOR.generate(options);
    }
}
