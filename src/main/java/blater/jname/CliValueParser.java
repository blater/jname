// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import java.util.Map;

final class CliValueParser {
    private static final Map<String, Complexity> COMPLEXITIES = Map.of(
            "0", Complexity.SMALL,
            "1", Complexity.MEDIUM,
            "2", Complexity.LARGE);

    private CliValueParser() {
    }

    static int positive(String value, String label) {
        var parsed = nonNegative(value, label);
        if (parsed == 0) {
            throw new CliException(label + " must be a positive integer, got: " + value);
        }
        return parsed;
    }

    static int nonNegative(String value, String label) {
        if (value.isEmpty()
                || !value.chars().allMatch(character -> character >= '0' && character <= '9')) {
            throw new CliException(label + " must be a positive integer, got: " + value);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CliException(label + " is too large, got: " + value);
        }
    }

    static Complexity complexity(String value) {
        var result = COMPLEXITIES.get(value);
        if (result == null) {
            throw new CliException(
                    "complexity must be 0 (small), 1 (medium), or 2 (large), got: " + value);
        }
        return result;
    }

    static JnameStrategy strategy(String value) {
        return switch (value) {
            case "tolkien" -> JnameStrategy.TOLKIEN;
            case "hex" -> JnameStrategy.HEX;
            case "base32" -> JnameStrategy.BASE32;
            case "ulid" -> JnameStrategy.ULID;
            default -> throw new CliException(
                    "strategy must be tolkien, hex, base32, or ulid, got: " + value);
        };
    }
}
