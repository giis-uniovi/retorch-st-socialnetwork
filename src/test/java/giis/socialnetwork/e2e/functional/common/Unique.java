package giis.socialnetwork.e2e.functional.common;

import java.util.concurrent.atomic.AtomicLong;

/** Strictly increasing, clock-seeded identifiers for unique usernames and post texts. */
public final class Unique {

    private static final AtomicLong LAST = new AtomicLong();

    private Unique() {}

    public static long next() {
        return LAST.updateAndGet(prev -> Math.max(prev + 1, System.currentTimeMillis()));
    }
}
