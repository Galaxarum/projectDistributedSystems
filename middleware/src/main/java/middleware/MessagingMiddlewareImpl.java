package middleware;

import middleware.group.GroupManager;

public class MessagingMiddlewareImpl implements MessagingMiddleware {

    public static final int DEFAULT_PORT = 12345;

    private final GroupManager groupManager;
    private final VectorClockedMessageManager messageManager;

    public MessagingMiddlewareImpl(String id, int port){
        this.groupManager = new GroupManager(id, port);
    }

    public MessagingMiddlewareImpl(String id){
        this(id,DEFAULT_PORT);
    }

    @Override
    public void join(String knownHost) {
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
