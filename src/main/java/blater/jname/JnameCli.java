// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Arrays;

/** Jname command-line entry point, compatible with the upstream petname options. */
public final class JnameCli {
    private static final String HELP = """
            Generate human-readable random names

            Usage: jname [-w|--words INT] [-l|--letters INT]
                           [-s|--separator STR] [-d|--dir STR]
                           [-c|--complexity INT] [-u|--ubuntu]

              -w, --words INT       number of words; default: 2
              -l, --letters INT     maximum letters in each word; default: unlimited
              -s, --separator STR   separator between words; default: -
              -d, --dir DIR         custom word-list directory
              -c, --complexity INT  0=small, 1=medium, 2=large
              -u, --ubuntu          generate an alliterative name
                  --adverb          generate one adverb
                  --adjective       generate one adjective
                  --name            generate one animal name
              -h, --help            show this help
            """;

    public static void main(String[] args) {
        var exitCode = new JnameCli().run(
                args,
                new PrintWriter(System.out, true),
                new PrintWriter(System.err, true));
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public int run(String[] args, PrintWriter out, PrintWriter err) {
        if (Arrays.stream(args).anyMatch(argument -> argument.equals("-h") || argument.equals("--help"))) {
            out.print(HELP);
            out.flush();
            return 0;
        }

        try {
            var options = parse(args);
            out.println(new JnameGenerator().generate(options));
            out.flush();
            return 0;
        } catch (CliException | IllegalArgumentException | JnameException e) {
            err.println("ERROR: " + e.getMessage());
            err.flush();
            return 1;
        }
    }

    private JnameOptions parse(String[] args) {
        var builder = JnameOptions.builder();
        var name = false;
        var adjective = false;
        var adverb = false;

        for (var index = 0; index < args.length; index++) {
            var option = args[index];
            switch (option) {
                case "-w", "--words" -> builder.words(parsePositive(value(args, ++index, option), "words"));
                case "-l", "--letters" ->
                        builder.maxLetters(Math.max(3, parseNonNegative(value(args, ++index, option), "letters")));
                case "-s", "--separator" -> builder.separator(value(args, ++index, option));
                case "-d", "--dir" -> builder.wordDirectory(Path.of(value(args, ++index, option)));
                case "-c", "--complexity" ->
                        builder.complexity(parseComplexity(value(args, ++index, option)));
                case "-u", "--ubuntu" -> builder.alliterate(true);
                case "--adverb" -> adverb = true;
                case "--adjective" -> adjective = true;
                case "--name" -> name = true;
                default -> throw new CliException("Unknown options [" + option + "]");
            }
        }

        builder.type(name ? JnameType.NAME
                : adjective ? JnameType.ADJECTIVE
                : adverb ? JnameType.ADVERB
                : JnameType.JNAME);
        return builder.build();
    }

    private String value(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new CliException("missing value for " + option);
        }
        return args[index];
    }

    private int parsePositive(String value, String label) {
        var parsed = parseNonNegative(value, label);
        if (parsed == 0) {
            throw new CliException(label + " must be a positive integer, got: " + value);
        }
        return parsed;
    }

    private int parseNonNegative(String value, String label) {
        if (value.isEmpty() || !value.chars().allMatch(Character::isDigit)) {
            throw new CliException(label + " must be a positive integer, got: " + value);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CliException(label + " is too large, got: " + value);
        }
    }

    private Complexity parseComplexity(String value) {
        return switch (value) {
            case "0" -> Complexity.SMALL;
            case "1" -> Complexity.MEDIUM;
            case "2" -> Complexity.LARGE;
            default -> throw new CliException(
                    "complexity must be 0 (small), 1 (medium), or 2 (large), got: " + value);
        };
    }

    private static final class CliException extends RuntimeException {
        private CliException(String message) {
            super(message);
        }
    }
}
