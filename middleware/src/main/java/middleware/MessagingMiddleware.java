package middleware;

import middleware.group.GroupManager;
import markers.Primitive;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public interface MessagingMiddleware <Key, Value, ApplicativePrimitive extends Enum<ApplicativePrimitive> & Primitive> extends GroupManager<Key,Value> {
    /**
     * This lock can be used to disable any active behaviour (generation of new messages) from this replica.
     * Needed to ensure that the distributed execution pauses while a new replica is joining
     */
    Lock operativeLock = new ReentrantLock(true);
    int DEFAULT_PORT = 12345;
    void shareOperation(ApplicativePrimitive command, Key key, Value value);
}
