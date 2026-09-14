// SPDX-License-Identifier: Apache-2.0
package blater.jname;

final class JnameAdapterFactory {
    private static final JnameAdapter DICTIONARY = new DictionaryAdapter();
    private static final JnameAdapter HEX = new AlphabetTokenAdapter("0123456789abcdef");
    private static final JnameAdapter BASE32 = new AlphabetTokenAdapter("0123456789ABCDEFGHJKMNPQRSTVWXYZ");
    private static final JnameAdapter ULID = new UlidAdapter();

    private JnameAdapterFactory() {
    }

    static JnameAdapter forStrategy(JnameStrategy strategy) {
        return switch (strategy) {
            case DEFAULT, TOLKIEN -> DICTIONARY;
            case HEX -> HEX;
            case BASE32 -> BASE32;
            case ULID -> ULID;
        };
    }

    private static final class DictionaryAdapter implements JnameAdapter {
        @Override
        public int defaultWords() {
            return 2;
        }

        @Override
        public String generate(JnameGeneratorBase generator, JnameOptions options) {
            var words = WordListLoader.load(options);
            var selector = new JnameWordSelector(generator.random());
            return switch (options.getType()) {
                case ADVERB -> single(selector.chooseEligible(words.getAdverbs(), options.getMaxLetters()), options);
                case ADJECTIVE -> single(selector.chooseEligible(words.getAdjectives(), options.getMaxLetters()), options);
                case NAME -> single(selector.chooseEligible(words.getNames(), options.getMaxLetters()), options);
                case JNAME -> new JnameComposer(selector).compose(words, options);
            };
        }

        private String single(String word, JnameOptions options) {
            return JnameComposer.caseWord(word, options);
        }
    }

    private static final class AlphabetTokenAdapter implements JnameAdapter {
        private final String alphabet;

        private AlphabetTokenAdapter(String alphabet) {
            this.alphabet = alphabet;
        }

        @Override
        public int defaultWords() {
            return 1;
        }

        @Override
        public String generate(JnameGeneratorBase generator, JnameOptions options) {
            int width = options.getMaxLetters() == 0 ? 4 : options.getMaxLetters();
            var random = generator.random();
            var result = new StringBuilder();
            synchronized (random) {
                for (int word = 0; word < options.getWords(); word++) {
                    if (word > 0) {
                        result.append(options.getSeparator());
                    }
                    var token = new StringBuilder(width);
                    for (int character = 0; character < width; character++) {
                        token.append(alphabet.charAt(random.nextInt(alphabet.length())));
                    }
                    var generatedToken = token.toString();
                    result.append(options.isMixedCase()
                            ? generatedToken.toUpperCase(java.util.Locale.ROOT)
                            : generatedToken.toLowerCase(java.util.Locale.ROOT));
                }
            }
            return result.toString();
        }
    }

    private static final class UlidAdapter implements JnameAdapter {
        @Override
        public int defaultWords() {
            return 1;
        }

        @Override
        public String generate(JnameGeneratorBase generator, JnameOptions options) {
            var result = new StringBuilder(26 * options.getWords());
            for (int word = 0; word < options.getWords(); word++) {
                if (word > 0) {
                    result.append(options.getSeparator());
                }
                result.append(generator.nextUlid(options.isMixedCase()));
            }
            return result.toString();
        }
    }
}
