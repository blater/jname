// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import lombok.AccessLevel;
import lombok.experimental.StandardException;

/** Indicates that a jname cannot be generated from the supplied configuration. */
@StandardException(access = AccessLevel.PUBLIC)
public final class JnameException extends RuntimeException {}
