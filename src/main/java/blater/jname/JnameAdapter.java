// SPDX-License-Identifier: Apache-2.0
package blater.jname;

interface JnameAdapter {
    int defaultWords();

    String generate(JnameGeneratorBase generator, JnameOptions options);
}
