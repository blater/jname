// SPDX-License-Identifier: Apache-2.0
package blater.jname;

/** Selects the algorithm used to generate a jname. */
public enum JnameStrategy {
    /** Use the standard dictionaries. */
    DEFAULT,
    /** Use Tolkien names and modifiers. */
    TOLKIEN,
    /** Generate hexadecimal tokens, lowercase by default and uppercase with mixedCase. */
    HEX,
    /** Generate Crockford Base32 tokens, lowercase by default and uppercase with mixedCase. */
    BASE32,
    /** Generate a 26-character ULID token. */
    ULID
}
