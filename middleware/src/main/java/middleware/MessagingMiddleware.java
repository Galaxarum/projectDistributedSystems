package middleware;

import middleware.group.GroupManager;
import markers.Primitive;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public interface MessagingMiddleware <Key, Value, ApplicativePrimitive extends Enum<ApplicativePrimitive> & Primitive> extends GroupManager<Key,Value> {
    Lock operativeLock = new ReentrantLock(true);
    void shareOperation(ApplicativePrimitive command, Key key, Value value);
}
