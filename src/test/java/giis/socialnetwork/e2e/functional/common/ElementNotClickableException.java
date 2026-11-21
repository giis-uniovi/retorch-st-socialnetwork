package giis.socialnetwork.e2e.functional.common;

/** Unchecked: element not clickable even after the JS-click fallback. */
public class ElementNotClickableException extends RuntimeException {
    public ElementNotClickableException(String message, Throwable cause) {
        super(message, cause);
    }
}
