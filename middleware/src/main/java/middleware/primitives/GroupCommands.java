package middleware.primitives;

/**
 * This {@linkplain Primitive} contains the commands used for group management
 */
public enum GroupCommands implements Primitive{
    /**
     * Sent to the known host when joining
     */
    JOIN,
    /**
     * Sent to the other replicas when joining
     */
    JOINING,
    /**
     * Sent to the known host when joining to ask for its data
     */
    SYNC,
    /**
     * Used to confirm the execution of some operations
     */
    ACK,
    /**
     * Sent to other replica to signal intention to leave
     */
    LEAVE;


}
