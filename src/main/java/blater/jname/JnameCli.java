// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import java.io.PrintWriter;

/** Jname command-line entry point, compatible with the upstream petname options. */
public final class JnameCli extends JnameCliMain {
    public int run(String[] args, PrintWriter out, PrintWriter err) {
        return new JnameCliRunner().run(args, out, err);
    }
}
