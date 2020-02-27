package middleware;

import java.util.List;

public interface MessagingMiddleware {
    void join(String knownHost);
    void leave();
    void sendVectorClockedMessage(VectorClockedMessage message);
}
