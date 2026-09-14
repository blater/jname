// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import java.nio.file.Path;

final class JnameCliParseState {
    private final JnameOptions.JnameOptionsBuilder builder = JnameOptions.builder();
    private final String[] arguments;
    private JnameType type = JnameType.JNAME;
    private int index;

    JnameCliParseState(String[] arguments) {
        this.arguments = arguments;
    }

    void apply(String option) {
        switch (option) {
            case "-w", "--words" -> words(option);
            case "-l", "--letters" -> letters(option);
            case "-s", "--separator" -> builder.separator(value(option));
            case "-p", "--prefix" -> builder.prefix(value(option));
            case "-d", "--dir" -> directory(option);
            case "-c", "--complexity" -> complexity(option);
            case "-t", "--strategy" -> strategy(option);
            case "-u", "--ubuntu" -> builder.alliterate(true);
            case "-m", "--mixedcase" -> builder.mixedCase(true);
            case "--adverb" -> type(JnameType.ADVERB);
            case "--adjective" -> type(JnameType.ADJECTIVE);
            case "--name" -> type(JnameType.NAME);
            default -> throw new CliException("Unknown options [" + option + "]");
        }
    }

    JnameOptions build() {
        return builder.type(type).build();
    }

    boolean hasNext() {
        return index < arguments.length;
    }

    String next() {
        return arguments[index++];
    }

    private void words(String option) {
        builder.words(CliValueParser.positive(value(option), "words"));
    }

    private void letters(String option) {
        var letters = CliValueParser.nonNegative(value(option), "letters");
        builder.maxLetters(Math.max(3, letters));
    }

    private void directory(String option) {
        builder.wordDirectory(Path.of(value(option)));
    }

    private void complexity(String option) {
        builder.complexity(CliValueParser.complexity(value(option)));
    }

    private void strategy(String option) {
        builder.strategy(CliValueParser.strategy(value(option)));
    }

    private void type(JnameType requested) {
        if (requested.compareTo(type) > 0) {
            type = requested;
        }
    }

    private String value(String option) {
        if (!hasNext()) {
            throw new CliException("missing value for " + option);
        }
        return next();
    }
}
