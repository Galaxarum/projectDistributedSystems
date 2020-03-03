package middleware;

import middleware.primitives.DataOperations;
import middleware.primitives.Primitive;

import java.util.Map;

public interface MessagingMiddleware <Key, Value, ApplicativePrimitive extends Enum<ApplicativePrimitive> & Primitive>{
    Map<Key, Value> join();
    void leave();
    void shareOperation(ApplicativePrimitive command, Key key, Value value);
}
