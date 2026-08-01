// SPDX-License-Identifier: Apache-2.0
package blater.jname;

/** Indicates that a jname cannot be generated from the supplied configuration. */
public final class JnameException extends RuntimeException {
    public JnameException(String message) {
        super(message);
    }

    public JnameException(String message, Throwable cause) {
        super(message, cause);
    }
}
