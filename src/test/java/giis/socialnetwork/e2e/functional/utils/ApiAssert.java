package giis.socialnetwork.e2e.functional.utils;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/** Assertions over DTO lists; failure messages include the actual list. */
public final class ApiAssert {

    private ApiAssert() {}

    /** Fails with "{what} must return HTTP {expected} but was {actual}". */
    public static void assertStatus(int expected, int actual, String what) {
        assertEquals(expected, actual, what + " must return HTTP " + expected + " but was " + actual);
    }

    public static <T> boolean anyMatch(List<T> items, Predicate<? super T> condition) {
        for (T item : items) {
            if (condition.test(item)) {
                return true;
            }
        }
        return false;
    }

    public static <T> void assertAnyMatch(List<T> items, Predicate<? super T> condition, String message) {
        if (!anyMatch(items, condition)) {
            fail(message + " (no match in " + items + ")");
        }
    }

    public static <T> void assertNoneMatch(List<T> items, Predicate<? super T> condition, String message) {
        if (anyMatch(items, condition)) {
            fail(message + " (unexpected match in " + items + ")");
        }
    }
}
