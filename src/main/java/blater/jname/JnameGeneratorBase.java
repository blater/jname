// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import lombok.NonNull;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;

/** Implements the shared generation workflow for a configured random source. */
abstract class JnameGeneratorBase {
    private static final long MAX_ULID_TIMESTAMP = (1L << 48) - 1;
    private static final String CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final BigInteger BASE_32 = BigInteger.valueOf(32);

    private final RandomGenerator defaultRandom = new SecureRandom();
    private final LongSupplier clock;
    private boolean hasLastUlid;
    private long lastUlidTimestamp;
    private byte[] lastUlidEntropy;

    JnameGeneratorBase(LongSupplier clock) {
        this.clock = clock;
    }

    /** Returns a deterministic generator, useful for repeatable workloads and tests. */
    public static JnameGenerator seeded(long seed) {
        return new JnameGenerator(new Random(seed));
    }

    public String generate(@NonNull JnameOptions options) {
        JnameOptionsValidator.validate(options);
        var adapter = JnameAdapterFactory.forStrategy(options.getStrategy());
        var words = options.getWords() == 0 ? adapter.defaultWords() : options.getWords();
        var resolved = options.toBuilder().words(words).build();
        var generated = adapter.generate(this, resolved);
        if (options.getPrefix().isEmpty()) {
            return generated;
        }
        return options.getPrefix() + options.getSeparator() + generated;
    }

    abstract RandomGenerator random();

    final RandomGenerator defaultRandom() {
        return defaultRandom;
    }

    /** Allocates and encodes the next monotonic ULID for this generator. */
    final synchronized String nextUlid(boolean mixedCase) {
        long now = clock.getAsLong();
        if (now < 0 || now > MAX_ULID_TIMESTAMP) {
            throw new JnameException("ULID timestamp is outside the 48-bit Unix millisecond range: " + now);
        }

        long timestamp;
        byte[] entropy;
        if (!hasLastUlid || now > lastUlidTimestamp) {
            timestamp = now;
            entropy = drawUlidEntropy();
        } else {
            timestamp = lastUlidTimestamp;
            entropy = Arrays.copyOf(lastUlidEntropy, lastUlidEntropy.length);
            if (!incrementUnsigned(entropy)) {
                throw new JnameException("ULID entropy exhausted for timestamp " + lastUlidTimestamp);
            }
        }

        var encoded = encodeUlid(timestamp, entropy);
        lastUlidTimestamp = timestamp;
        lastUlidEntropy = entropy;
        hasLastUlid = true;
        return mixedCase ? encoded : encoded.toLowerCase(java.util.Locale.ROOT);
    }

    private byte[] drawUlidEntropy() {
        var bytes = new byte[10];
        var source = random();
        synchronized (source) {
            long first = source.nextLong();
            long second = source.nextLong();
            for (int index = 0; index < 8; index++) {
                bytes[index] = (byte) (first >>> (56 - 8 * index));
            }
            bytes[8] = (byte) (second >>> 56);
            bytes[9] = (byte) (second >>> 48);
        }
        return bytes;
    }

    private boolean incrementUnsigned(byte[] value) {
        for (int index = value.length - 1; index >= 0; index--) {
            value[index]++;
            if (value[index] != 0) {
                return true;
            }
        }
        return false;
    }

    private String encodeUlid(long timestamp, byte[] entropy) {
        var bytes = new byte[16];
        for (int index = 0; index < 6; index++) {
            bytes[5 - index] = (byte) (timestamp >>> (8 * index));
        }
        System.arraycopy(entropy, 0, bytes, 6, entropy.length);

        var remaining = new BigInteger(1, bytes);
        var output = new char[26];
        for (int index = output.length - 1; index >= 0; index--) {
            var division = remaining.divideAndRemainder(BASE_32);
            output[index] = CROCKFORD.charAt(division[1].intValue());
            remaining = division[0];
        }
        return new String(output);
    }
}
