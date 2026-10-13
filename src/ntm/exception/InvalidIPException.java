package ntm.exception;

/**
 * Thrown when an IP address is not in the form a.b.c.d with each part between 0 and 255.
 */
public class InvalidIPException extends Exception {

    public InvalidIPException(String ip) {
        super("Invalid IP address: \"" + ip + "\".\nUse four numbers from 0 to 255, for example 192.168.1.10");
    }
}
