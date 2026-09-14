// SPDX-License-Identifier: Apache-2.0
package blater.jname;

final class JnameCliParser {
    private JnameCliParser() {
    }

    static JnameOptions parse(String[] args) {
        var state = new JnameCliParseState(args);
        while (state.hasNext()) {
            state.apply(state.next());
        }
        return state.build();
    }
}
