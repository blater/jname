// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import java.io.PrintWriter;

final class JnameCliHelp {
    private static final String TEXT = """
            Generate human-readable random names

            Usage: jname [-w|--words INT] [-l|--letters INT]
                           [-s|--separator STR] [-p|--prefix STR] [-d|--dir STR]
                           [-c|--complexity INT] [-t|--strategy STR] [-u|--ubuntu]
                           [-m|--mixedcase]

              -w, --words INT       number of words; default: 2 (1 for token strategies)
              -l, --letters INT     word limit / hex-base32 width; ulid is fixed at 26
                                     default: unlimited / 4; explicit 0, 1, or 2 becomes 3
              -s, --separator STR   separator between words; default: -
              -p, --prefix STR      prefix before the generated name
              -d, --dir DIR         custom word-list directory
              -c, --complexity INT  0=small, 1=medium, 2=large
              -t, --strategy STR    generation strategy: tolkien, hex, base32, ulid
              -u, --ubuntu          generate an alliterative name
              -m, --mixedcase       preserve dictionary case; uppercase generated tokens
              --adverb             generate one adverb
              --adjective          generate one adjective
              --name             generate one name word
              -h, --help            show this help
            """;

    private JnameCliHelp() {
    }

    static boolean isHelpOption(String option) {
        return option.equals("-h") || option.equals("--help");
    }

    static int print(PrintWriter out) {
        out.print(TEXT);
        out.flush();
        return 0;
    }
}
