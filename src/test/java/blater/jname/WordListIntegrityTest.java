// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordListIntegrityTest {
    private static final String ROOT = "/blater/jname/words/";
    private static final Map<String, String> HASHES = Map.ofEntries(
            Map.entry("large/adjectives.txt", "8a36e132c66fc2a879770dfc1f73f2a11b454194ca75f3f0fde3e6922965608c"),
            Map.entry("large/adverbs.txt", "95b8d19579711acc199a3a6a6e102d5abd3d2df57bc052166d6009f651241591"),
            Map.entry("large/names.txt", "6e0db1b0619462388115fb2ae26277db4391052f6161842c15f8d8189078a14c"),
            Map.entry("medium/adjectives.txt", "3b91020cc035b00b912f7cfb0b77a66a8465ee1e3dd1880b246699b57e703f2b"),
            Map.entry("medium/adverbs.txt", "c19e6fc3d06acf6d5a3812772f60012e443cc96f41e63c5e88e0007e7111646c"),
            Map.entry("medium/names.txt", "2d929c68b1e1e431ab7eff914514e8ea7f70d05e54d2fb3d9a9d6c90deb7a0c9"),
            Map.entry("small/adjectives.txt", "5f934ce94a217b85aec434d6803c6fc5b74628d10d83c4d0ddfdec9256e062b0"),
            Map.entry("small/adverbs.txt", "f22baacc9c281e6e13daea8dcc8d204c5ad08b26d0c4cd5a1ea0e6e1e782b445"),
            Map.entry("small/names.txt", "a15d352808bda5b983dad1d68d71f97dd5819c28fe5779169e8ce9c82f0aff6b"),
            Map.entry("tolkien/adjectives.txt", "ac7a58038f0be2b98af1358fd8d74e11438b1fbbca54a06ad904b5f95e7792a4"),
            Map.entry("tolkien/adverbs.txt", "04c531229d1b26f31254b581f04fd4b053e37f5e5ecb01e71743522205593127"),
            Map.entry("tolkien/names.txt", "29d226288a0552229efc8bcb39d06d0e29bac92aff22175c40879eb66b511350"));

    @Test
    void matchesPinnedUpstreamFiles() throws IOException, NoSuchAlgorithmException {
        var digest = MessageDigest.getInstance("SHA-256");
        for (var expected : HASHES.entrySet()) {
            try (var input = getClass().getResourceAsStream(ROOT + expected.getKey())) {
                assertNotNull(input, expected.getKey());
                var actual = HexFormat.of().formatHex(digest.digest(input.readAllBytes()));
                assertEquals(expected.getValue(), actual, expected.getKey());
            }
        }
    }

    @Test
    void allTwelvePinnedListsArePresentAndMediumContainsSmall() throws IOException {
        assertEquals(12, HASHES.size());
        for (var category : List.of("adverbs.txt", "adjectives.txt", "names.txt")) {
            var small = readLines("small/" + category);
            var medium = new HashSet<>(readLines("medium/" + category));
            assertTrue(medium.containsAll(small), category);
        }
        assertTrue(readLines("medium/adjectives.txt").contains("funky"));
        assertTrue(readLines("medium/names.txt").contains("man"));
    }

    @Test
    void tolkienNamesUseAsciiSpellings() throws IOException {
        assertTrue(readLines("tolkien/names.txt").stream()
                .flatMapToInt(String::codePoints)
                .allMatch(codePoint -> codePoint < 128));
    }

    private List<String> readLines(String path) throws IOException {
        try (var input = getClass().getResourceAsStream(ROOT + path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .lines().toList();
        }
    }
}
