package middleware;

import middleware.database.DatabaseManager;
import middleware.group.GroupManager;

import java.io.IOException;

public class MessagingMiddlewareImpl implements MessagingMiddleware {

    public static final int DEFAULT_PORT = 12345;

    private final GroupManager groupManager;
    private final VectorClockedMessageManager messageManager;
    private DatabaseManager databaseManager;

    public MessagingMiddlewareImpl(String id, int port){
        databaseManager = new DatabaseManager();
        this.groupManager = new GroupManager(id, port, databaseManager);
    }

    public MessagingMiddlewareImpl(String id){
        this(id,DEFAULT_PORT);
    }

    @Override
    public void join(String knownHost) throws IOException, ClassNotFoundException {
        groupManager.join(knownHost);
    }

    @Override
    public void leave() {
        groupManager.leave();
    }

    @Override
    public void sendVectorClockedMessage(VectorClockedMessage message) {
        messageManager.send(message);
    }
}
