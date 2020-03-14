package middleware;

import middleware.group.GroupManager;
import middleware.group.GroupManagerImpl;
import middleware.primitives.Primitive;

import java.util.Map;

public interface MessagingMiddleware <Key, Value, ApplicativePrimitive extends Enum<ApplicativePrimitive> & Primitive> extends GroupManager {
    Map<Key, Value> join();
    void leave();
    void shareOperation(ApplicativePrimitive command, Key key, Value value);
}
