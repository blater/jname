// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import java.io.PrintWriter;
import java.util.Arrays;

class JnameCliRunner {
    int run(String[] args, PrintWriter out, PrintWriter err) {
        if (requestsHelp(args)) {
            return JnameCliHelp.print(out);
        }
        try {
            return generate(args, out);
        } catch (CliException | IllegalArgumentException | JnameException e) {
            return printError(err, e);
        }
    }

    private boolean requestsHelp(String[] args) {
        return Arrays.stream(args).anyMatch(JnameCliHelp::isHelpOption);
    }

    private int generate(String[] args, PrintWriter out) {
        var options = JnameCliParser.parse(args);
        out.println(new JnameGenerator().generate(options));
        out.flush();
        return 0;
    }

    private int printError(PrintWriter err, RuntimeException error) {
        err.println("ERROR: " + error.getMessage());
        err.flush();
        return 1;
    }
}
