// SPDX-License-Identifier: Apache-2.0
package blater.jname;

import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;

/** Generates jnames using a configurable source of randomness. */
public final class JnameGenerator extends JnameGeneratorBase {
    private final RandomGenerator random;

    /** Creates a generator that uses secure randomness and the system clock. */
    public JnameGenerator() {
        this(null, System::currentTimeMillis);
    }

    /** Creates a generator using the supplied random source and the system clock. */
    public JnameGenerator(RandomGenerator random) {
        this(Objects.requireNonNull(random, "random"), System::currentTimeMillis);
    }

    /** Package-private clock hook used by deterministic ULID tests. */
    JnameGenerator(RandomGenerator random, LongSupplier clock) {
        super(Objects.requireNonNull(clock, "clock"));
        this.random = random;
    }

    @Override
    RandomGenerator random() {
        return random == null ? defaultRandom() : random;
    }
}
