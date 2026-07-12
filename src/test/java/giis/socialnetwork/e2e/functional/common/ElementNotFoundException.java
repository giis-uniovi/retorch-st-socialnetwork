package giis.socialnetwork.e2e.functional.common;

/**
 * Unchecked exception raised when an element cannot be interacted with even after
 * the JavaScript-click fallback. Unchecked so page objects and tests do not need
 * {@code throws} clauses for an unrecoverable UI failure.
 */
public class ElementNotFoundException extends RuntimeException {
    public ElementNotFoundException(String message) {
        super(message);
    }
}
