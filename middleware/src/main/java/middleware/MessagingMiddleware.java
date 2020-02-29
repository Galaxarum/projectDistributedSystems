package middleware;

import middleware.primitives.DataOperations;

import java.io.IOException;
import java.util.List;

public interface MessagingMiddleware <K,V>{
    void join(String knownHost) throws IOException;
    void leave();
    void shareOperation(DataOperations command, K key, V value);
}
