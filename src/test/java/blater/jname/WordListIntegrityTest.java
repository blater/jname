// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WordListIntegrityTest {
    private static final String ROOT = "/blater/jname/words/";
    private static final Map<String, String> HASHES = Map.ofEntries(
            Map.entry("large/adjectives.txt", "8a36e132c66fc2a879770dfc1f73f2a11b454194ca75f3f0fde3e6922965608c"),
            Map.entry("large/adverbs.txt", "95b8d19579711acc199a3a6a6e102d5abd3d2df57bc052166d6009f651241591"),
            Map.entry("large/names.txt", "6e0db1b0619462388115fb2ae26277db4391052f6161842c15f8d8189078a14c"),
            Map.entry("medium/adjectives.txt", "b1cd54d8f27d4514aaa9bbf4a8248b696867b63b75ffcbe49b277227e2875a13"),
            Map.entry("medium/adverbs.txt", "c19e6fc3d06acf6d5a3812772f60012e443cc96f41e63c5e88e0007e7111646c"),
            Map.entry("medium/names.txt", "0f37daddd7e68ba01240032188b542d32a838f2352197d04c1bb0f46a22f647a"),
            Map.entry("small/adjectives.txt", "5f934ce94a217b85aec434d6803c6fc5b74628d10d83c4d0ddfdec9256e062b0"),
            Map.entry("small/adverbs.txt", "f22baacc9c281e6e13daea8dcc8d204c5ad08b26d0c4cd5a1ea0e6e1e782b445"),
            Map.entry("small/names.txt", "a15d352808bda5b983dad1d68d71f97dd5819c28fe5779169e8ce9c82f0aff6b"));

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
}
