// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JnameStrategyTest {
    @TempDir
    Path words;

    @Test
    void tokenStrategiesUseExactWidthsAndKeepSeparatorsAndPrefixesVerbatim() {
        var generator = new JnameGenerator(new FixedRandom(0, 0));
        var hex = JnameOptions.builder()
                .strategy(JnameStrategy.HEX)
                .words(2)
                .maxLetters(3)
                .separator("Ab")
                .prefix("Pre")
                .build();
        var upperHex = hex.toBuilder().mixedCase(true).build();
        var base32 = hex.toBuilder().strategy(JnameStrategy.BASE32).separator("MiX").prefix("").build();
        var upperBase32 = base32.toBuilder().mixedCase(true).build();

        assertEquals("PreAbfffAbfff", generator.generate(hex));
        assertEquals("PreAbFFFAbFFF", generator.generate(upperHex));
        assertEquals("zzzzMiXzzzz", generator.generate(base32.toBuilder().maxLetters(4).build()));
        assertEquals("ZZZZMiXZZZZ", generator.generate(upperBase32.toBuilder().maxLetters(4).build()));
    }

    @Test
    void tokensIgnoreDictionarySettingsAndUseStrategyDefaultWidths() {
        var generator = new JnameGenerator(new FixedRandom(0, 0));
        var options = JnameOptions.builder()
                .strategy(JnameStrategy.HEX)
                .wordDirectory(words.resolve("missing"))
                .type(JnameType.ADVERB)
                .alliterate(true)
                .build();

        assertEquals("ffff", generator.generate(options));
        assertEquals("zzzz", generator.generate(options.toBuilder().strategy(JnameStrategy.BASE32).build()));
        assertEquals(1, generator.generate(options.toBuilder().maxLetters(1).build()).length());
        assertEquals(2, generator.generate(options.toBuilder().maxLetters(2).build()).length());
        assertEquals(1, generator.generate(options.toBuilder().strategy(JnameStrategy.BASE32).maxLetters(1).build()).length());
        assertEquals(2, generator.generate(options.toBuilder().strategy(JnameStrategy.BASE32).maxLetters(2).build()).length());
        assertThrows(IllegalArgumentException.class, () -> generator.generate(options.toBuilder()
                .complexity(Complexity.SMALL).build()));
    }

    @Test
    void ulidsKeepFixedWidthAndEncodeTimestampAndEntropyBoundaries() {
        var zeroClock = new AtomicLong(0);
        var zeroGenerator = new JnameGenerator(new FixedRandom(0, 0), zeroClock::get);
        var upper = JnameOptions.builder().strategy(JnameStrategy.ULID).mixedCase(true).build();

        assertEquals("00000000000000000000000000", zeroGenerator.generate(upper.toBuilder().mixedCase(false).build()));
        assertEquals(26, zeroGenerator.generate(upper).length());
        assertTrue(zeroGenerator.generate(upper).matches("[0-7][0-9A-Z]{25}"));

        var maxTimestamp = (1L << 48) - 1;
        var maxGenerator = new JnameGenerator(new FixedRandom(-1, -1), () -> maxTimestamp);
        var maxValue = maxGenerator.generate(upper);
        assertEquals("7" + "Z".repeat(25), maxValue);

        assertThrows(JnameException.class, () -> new JnameGenerator(new FixedRandom(0), () -> -1L)
                .generate(JnameOptions.builder().strategy(JnameStrategy.ULID).build()));
        assertThrows(JnameException.class, () -> new JnameGenerator(new FixedRandom(0), () -> 1L << 48)
                .generate(JnameOptions.builder().strategy(JnameStrategy.ULID).build()));
    }

    @Test
    void ulidMatchesKnownEncodingVectorAndOrdersInBothCases() {
        var vectorGenerator = new JnameGenerator(
                new FixedRandom(0xd6764c61efb99302L, 0xbd5b000000000000L),
                () -> 1469922850259L);
        var uppercase = JnameOptions.builder().strategy(JnameStrategy.ULID).mixedCase(true).build();
        assertEquals("01ARZ3NDEKTSV4RRFFQ69G5FAV", vectorGenerator.generate(uppercase));

        var upperGenerator = new JnameGenerator(new FixedRandom(0, 0), () -> 1L);
        var upperFirst = upperGenerator.generate(uppercase);
        var upperNext = upperGenerator.generate(uppercase);
        assertTrue(upperFirst.compareTo(upperNext) < 0);

        var lowerGenerator = new JnameGenerator(new FixedRandom(0, 0), () -> 1L);
        var lowerFirst = lowerGenerator.generate(uppercase.toBuilder().mixedCase(false).build());
        var lowerNext = lowerGenerator.generate(uppercase.toBuilder().mixedCase(false).build());
        assertTrue(lowerFirst.compareTo(lowerNext) < 0);
    }

    @Test
    void ulidsIncreaseAcrossCallsTokensAndStrategySwitches() {
        var clock = new AtomicLong(123);
        var generator = new JnameGenerator(new FixedRandom(0, 0), clock::get);
        var ulids = JnameOptions.builder().strategy(JnameStrategy.ULID).build();
        var first = generator.generate(ulids);
        var pair = generator.generate(ulids.toBuilder().words(2).separator(":").build()).split(":");
        var duringSwitch = generator.generate(JnameOptions.builder().strategy(JnameStrategy.HEX).build());
        var afterSwitch = generator.generate(ulids);

        assertTrue(first.compareTo(pair[0]) < 0);
        assertTrue(pair[0].compareTo(pair[1]) < 0);
        assertEquals("ffff", duringSwitch);
        assertTrue(pair[1].compareTo(afterSwitch) < 0);
        assertEquals(first.substring(0, 10), afterSwitch.substring(0, 10));
    }

    @Test
    void ulidEntropyIncrementCarriesAndOverflowDoesNotWrap() {
        var clock = new AtomicLong(7);
        var carryGenerator = new JnameGenerator(new FixedRandom(0, 0xFEFE000000000000L), clock::get);
        var ulids = JnameOptions.builder().strategy(JnameStrategy.ULID).build();
        var beforeCarry = carryGenerator.generate(ulids);
        var afterCarry = carryGenerator.generate(ulids);
        var furtherCarry = carryGenerator.generate(ulids);
        assertEquals(decode(beforeCarry).add(BigInteger.ONE), decode(afterCarry));
        assertEquals(decode(afterCarry).add(BigInteger.ONE), decode(furtherCarry));

        var overflowGenerator = new JnameGenerator(new FixedRandom(-1, -1), clock::get);
        var maxEntropy = overflowGenerator.generate(ulids);
        assertThrows(JnameException.class, () -> overflowGenerator.generate(ulids));
        assertThrows(JnameException.class, () -> overflowGenerator.generate(ulids));
        clock.incrementAndGet();
        var recovered = overflowGenerator.generate(ulids);
        assertTrue(recovered.compareTo(maxEntropy) > 0);
        assertEquals("0000000008", recovered.substring(0, 10));
    }

    @Test
    void ulidUsesFreshEntropyOnAdvanceAndRetainsTheTimestampOnRollback() {
        var clock = new AtomicLong(100);
        var random = new FixedRandom(0, 0, 0x0102030405060708L, 0x090A000000000000L);
        var generator = new JnameGenerator(random, clock::get);
        var options = JnameOptions.builder().strategy(JnameStrategy.ULID).build();

        var first = generator.generate(options);
        clock.set(101);
        var freshEntropy = generator.generate(options);
        clock.set(99);
        var rollback = generator.generate(options);

        assertTrue(first.compareTo(freshEntropy) < 0);
        var entropyMask = BigInteger.ONE.shiftLeft(80).subtract(BigInteger.ONE);
        assertEquals(new BigInteger("0102030405060708090a", 16), decode(freshEntropy).and(entropyMask));
        assertEquals(freshEntropy.substring(0, 10), rollback.substring(0, 10));
        assertEquals(decode(freshEntropy).add(BigInteger.ONE), decode(rollback));
    }

    @Test
    void failedFreshEntropyReadDoesNotPublishANewTimestamp() {
        var clock = new AtomicLong(5);
        var generator = new JnameGenerator(new ThrowOnThirdLongRandom(), clock::get);
        var options = JnameOptions.builder().strategy(JnameStrategy.ULID).build();
        var first = generator.generate(options);

        clock.set(6);
        assertThrows(IllegalStateException.class, () -> generator.generate(options));
        clock.set(5);
        var next = generator.generate(options);

        assertTrue(first.compareTo(next) < 0);
        assertEquals(first.substring(0, 10), next.substring(0, 10));
    }

    @Test
    void ulidAllocationIsSerializedAndEachGeneratorHasIndependentState() throws Exception {
        LongSupplier clock = () -> 99L;
        var concurrentGenerator = new JnameGenerator(new FixedRandom(0, 0), clock);
        var options = JnameOptions.builder().strategy(JnameStrategy.ULID).build();
        List<String> concurrent;
        try (var executor = Executors.newFixedThreadPool(8)) {
            var futures = IntStream.range(0, 64)
                    .mapToObj(index -> executor.submit(() -> concurrentGenerator.generate(options)))
                    .toList();
            concurrent = new ArrayList<>();
            for (var future : futures) {
                concurrent.add(future.get());
            }
        }
        assertEquals(64, new TreeSet<>(concurrent).size());

        var sequentialGenerator = new JnameGenerator(new FixedRandom(0, 0), clock);
        var sequential = IntStream.range(0, 64)
                .mapToObj(index -> sequentialGenerator.generate(options))
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        assertEquals(sequential, new TreeSet<>(concurrent));

        var sharedRandom = new FixedRandom(0, 0);
        var firstGenerator = new JnameGenerator(sharedRandom, clock);
        var secondGenerator = new JnameGenerator(sharedRandom, clock);
        var firstId = firstGenerator.generate(options);
        var secondId = firstGenerator.generate(options);
        var independentFirstId = secondGenerator.generate(options);
        assertEquals(firstId, independentFirstId);
        assertTrue(firstId.compareTo(secondId) < 0);
        assertFalse(firstId.equals(secondId));
    }

    @Test
    void generatorsSharingARandomSourceSerializeRandomDraws() throws Exception {
        var random = new OverlapDetectingRandom();
        var firstGenerator = new JnameGenerator(random, () -> 50L);
        var secondGenerator = new JnameGenerator(random, () -> 50L);
        var hexOptions = JnameOptions.builder().strategy(JnameStrategy.HEX).words(3).build();
        var ulidOptions = JnameOptions.builder().strategy(JnameStrategy.ULID).build();

        try (var executor = Executors.newFixedThreadPool(8)) {
            var futures = IntStream.range(0, 32)
                    .mapToObj(index -> executor.submit(() -> {
                        if (index % 2 == 0) {
                            firstGenerator.generate(hexOptions);
                        } else {
                            secondGenerator.generate(ulidOptions);
                        }
                    }))
                    .toList();
            for (var future : futures) {
                future.get();
            }
        }

        assertEquals(1, random.maxConcurrentDraws.get());
    }

    private static class FixedRandom extends Random {
        private final long[] values;
        private int index;

        FixedRandom(long... values) {
            this.values = values.length == 0 ? new long[]{0} : values;
        }

        @Override
        public synchronized long nextLong() {
            return values[Math.min(index++, values.length - 1)];
        }

        @Override
        public int nextInt(int bound) {
            return bound - 1;
        }
    }

    private BigInteger decode(String ulid) {
        var value = BigInteger.ZERO;
        for (var character : ulid.toUpperCase(java.util.Locale.ROOT).toCharArray()) {
            value = value.shiftLeft(5).add(BigInteger.valueOf("0123456789ABCDEFGHJKMNPQRSTVWXYZ".indexOf(character)));
        }
        return value;
    }

    private static final class ThrowOnThirdLongRandom extends FixedRandom {
        private int calls;

        ThrowOnThirdLongRandom() {
            super(0, 0);
        }

        @Override
        public synchronized long nextLong() {
            if (++calls == 3) {
                throw new IllegalStateException("test entropy failure");
            }
            return 0;
        }
    }

    private static final class OverlapDetectingRandom extends Random {
        private final AtomicInteger activeDraws = new AtomicInteger();
        private final AtomicInteger maxConcurrentDraws = new AtomicInteger();

        @Override
        public int nextInt(int bound) {
            requireSourceLock();
            return duringDraw(() -> 0);
        }

        @Override
        public long nextLong() {
            requireSourceLock();
            return duringDraw(() -> 0L);
        }

        private void requireSourceLock() {
            if (!Thread.holdsLock(this)) {
                throw new AssertionError("random source draw was not synchronized on its source");
            }
        }

        private <T> T duringDraw(java.util.function.Supplier<T> operation) {
            int active = activeDraws.incrementAndGet();
            maxConcurrentDraws.accumulateAndGet(active, Math::max);
            try {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                return operation.get();
            } finally {
                activeDraws.decrementAndGet();
            }
        }
    }
}
