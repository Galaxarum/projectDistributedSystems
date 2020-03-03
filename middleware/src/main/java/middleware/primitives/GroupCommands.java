package middleware.primitives;

import exceptions.BrokenProtocolException;

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
    LEAVE;


}
