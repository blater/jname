// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Selects one of the upstream petname dictionaries. */
@Getter
@RequiredArgsConstructor
public enum Complexity {
    /** Medium for built-in words, or the root of a custom word directory. */
    DEFAULT(null),
    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large");

    private final String directory;
}
