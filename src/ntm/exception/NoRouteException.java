package ntm.exception;

/**
 * Thrown when a packet cannot reach its destination because no usable path exists.
 */
public class NoRouteException extends Exception {

    public NoRouteException(String message) {
        super(message);
    }
}
