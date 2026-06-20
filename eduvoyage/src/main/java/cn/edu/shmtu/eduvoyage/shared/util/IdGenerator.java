package cn.edu.shmtu.eduvoyage.shared.util;

import org.springframework.stereotype.Component;

/**
 * Twitter-style Snowflake 64-bit id generator. Produces roughly time-ordered,
 * globally-unique {@code long} ids used as primary keys across MySQL tables
 * (application-generated, R2DBC/sharding friendly).
 *
 * <pre>
 *   1 bit  unused (sign)
 *  41 bits millisecond timestamp (epoch offset)
 *  10 bits worker id (datacenter + machine)
 *  12 bits per-ms sequence
 * </pre>
 *
 * Thread-safe via synchronisation on {@link #nextId()}.
 */
@Component
public class IdGenerator {

    /** Custom epoch: 2024-01-01T00:00:00Z in millis. */
    private static final long EPOCH = 1_704_067_200_000L;

    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS); // 1023
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);  // 4095

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public IdGenerator() {
        this(resolveWorkerId());
    }

    public IdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
        }
        this.workerId = workerId;
    }

    /** Derives a stable worker id from {@code EDUVOYAGE_WORKER_ID} or the hostname hash. */
    private static long resolveWorkerId() {
        String env = System.getenv("EDUVOYAGE_WORKER_ID");
        if (env != null && !env.isBlank()) {
            try {
                return Long.parseLong(env.trim()) & MAX_WORKER_ID;
            } catch (NumberFormatException ignored) {
                // fall through to hostname-based derivation
            }
        }
        String host = System.getenv().getOrDefault("HOSTNAME", "eduvoyage-node");
        return ((long) host.hashCode() & 0x7fffffffL) & MAX_WORKER_ID;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            // clock moved backwards; wait until we catch up to avoid duplicates
            timestamp = waitUntil(lastTimestamp);
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitUntil(lastTimestamp + 1);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /** Convenience for callers that prefer string ids (e.g. object keys, tokens). */
    public String nextIdString() {
        return Long.toString(nextId());
    }

    private long waitUntil(long target) {
        long ts = System.currentTimeMillis();
        while (ts < target) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }
}
