package middleware;

import middleware.primitives.DataOperations;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface MessagingMiddleware <K,V>{
    Map<K,V> join(String knownHost) throws IOException;
    void leave();
    void shareOperation(DataOperations command, K key, V value);
}
