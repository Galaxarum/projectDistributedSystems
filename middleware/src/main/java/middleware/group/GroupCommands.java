package middleware.group;

public enum GroupCommands {
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
     * Sent to all the replicas at join completion to unlock them
     */
    READY
}
