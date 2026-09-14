// SPDX-License-Identifier: Apache-2.0
package blater.jname;

final class JnameOptionsValidator {
    private JnameOptionsValidator() {
    }

    static void validate(JnameOptions options) {
        requireNonnegativeWordCount(options);
        requireNonnegativeMaximumLength(options);
        requireShortSeparator(options);
        requireCompatibleStrategyAndComplexity(options);
    }

    private static void requireNonnegativeWordCount(JnameOptions options) {
        if (options.getWords() < 0) {
            throw new IllegalArgumentException("words must be zero or a positive integer");
        }
    }

    private static void requireNonnegativeMaximumLength(JnameOptions options) {
        if (options.getMaxLetters() < 0) {
            throw new IllegalArgumentException("maxLetters must be zero or a positive integer");
        }
    }

    private static void requireShortSeparator(JnameOptions options) {
        if (options.getSeparator().codePointCount(0, options.getSeparator().length()) > 100) {
            throw new IllegalArgumentException("separator must be 100 characters or less");
        }
    }

    private static void requireCompatibleStrategyAndComplexity(JnameOptions options) {
        if (options.getStrategy() != JnameStrategy.DEFAULT
                && options.getComplexity() != Complexity.DEFAULT) {
            throw new IllegalArgumentException("a non-default strategy cannot be combined with a complexity tier");
        }
    }
}
