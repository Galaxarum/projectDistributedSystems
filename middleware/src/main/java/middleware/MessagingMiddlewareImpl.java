package middleware;

import middleware.group.GroupManager;
import middleware.primitives.DataOperations;

import java.io.IOException;

public class MessagingMiddlewareImpl implements MessagingMiddleware<String,Object> {

    public static final int DEFAULT_PORT = 12345;

    private final GroupManager groupManager;


    public MessagingMiddlewareImpl(String id, int port) throws IOException {
        this.groupManager = new GroupManager(id, port);
    }

    public MessagingMiddlewareImpl(String id) throws IOException {
        this(id,DEFAULT_PORT);
    }

    @Override
    public void join(String knownHost) throws IOException {
        groupManager.join(knownHost);
    }

    @Override
    public void leave() {
        groupManager.leave();
    }

    @Override
    public void shareOperation(DataOperations command, String key, Object value) {

    }
}
