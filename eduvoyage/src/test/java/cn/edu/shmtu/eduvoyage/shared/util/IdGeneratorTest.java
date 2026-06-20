package cn.edu.shmtu.eduvoyage.shared.util;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Snowflake {@link IdGenerator}: uniqueness (incl. under
 * concurrency) and monotonic-ish ordering.
 */
class IdGeneratorTest {

    @Test
    void generatesUniqueIdsSequentially() {
        IdGenerator gen = new IdGenerator(1);
        Set<Long> ids = new java.util.HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            assertThat(ids.add(gen.nextId())).as("duplicate at %d", i).isTrue();
        }
    }

    @Test
    void generatesUniqueIdsUnderConcurrency() {
        IdGenerator gen = new IdGenerator(2);
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        IntStream.range(0, 16).parallel().forEach(t ->
                IntStream.range(0, 10_000).forEach(i -> ids.add(gen.nextId())));
        assertThat(ids).hasSize(16 * 10_000);
    }

    @Test
    void idsAreRoughlyTimeOrdered() {
        IdGenerator gen = new IdGenerator(3);
        long first = gen.nextId();
        long second = gen.nextId();
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void rejectsOutOfRangeWorkerId() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> new IdGenerator(2048));
    }
}
