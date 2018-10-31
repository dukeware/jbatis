package org.ibatis.persist;

/**
 * Thrown by the persistence provider when a problem occurs.
 *
 * @since iBatis Persistence 1.0
 */
@SuppressWarnings("serial")
public class PersistenceException extends RuntimeException {

    /**
     * Constructs a new <code>PersistenceException</code> exception with <code>null</code> as its detail message.
     */
    public PersistenceException() {
        super();
    }

    /**
     * Constructs a new <code>PersistenceException</code> exception with the specified detail message.
     * 
     * @param message
     *            the detail message.
     */
    public PersistenceException(String message) {
        super(message);
    }

    /**
     * Constructs a new <code>PersistenceException</code> exception with the specified detail message and cause.
     * 
     * @param message
     *            the detail message.
     * @param cause
     *            the cause.
     */
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new <code>PersistenceException</code> exception with the specified cause.
     * 
     * @param cause
     *            the cause.
     */
    public PersistenceException(Throwable cause) {
        super(cause);
    }
}
