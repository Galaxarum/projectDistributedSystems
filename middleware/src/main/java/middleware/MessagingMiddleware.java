package middleware;

import java.io.IOException;
import java.util.List;

public interface MessagingMiddleware {
    void join(String knownHost) throws IOException, ClassNotFoundException;
    void leave();
    void sendVectorClockedMessage(VectorClockedMessage message);
}
