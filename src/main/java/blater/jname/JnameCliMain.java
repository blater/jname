// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import java.io.PrintWriter;

class JnameCliMain {
    public static void main(String[] args) {
        var exitCode = new JnameCli().run(
                args,
                new PrintWriter(System.out, true),
                new PrintWriter(System.err, true));
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
