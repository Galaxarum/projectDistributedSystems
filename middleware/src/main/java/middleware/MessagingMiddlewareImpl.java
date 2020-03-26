package middleware;

import middleware.group.GroupManager;
import middleware.group.GroupManagerImpl;
import markers.Primitive;

import java.util.Map;

public class MessagingMiddlewareImpl<Key,Value,ApplicationPrimitive extends Enum<ApplicationPrimitive> & Primitive> implements MessagingMiddleware<Key,Value,ApplicationPrimitive> {

    private final GroupManager<Key,Value> groupManager;


    public MessagingMiddlewareImpl(String id, int port,String leaderHost) {
        this.groupManager = new GroupManagerImpl<>(id, port, leaderHost);
    }

    public MessagingMiddlewareImpl(String id,String leaderHost) {
        this(id,DEFAULT_PORT,leaderHost);
    }

    @Override
    public Map<Key, Value> join() {
        try {
            operativeLock.lock();
            return groupManager.join();
        }finally {
            operativeLock.unlock();
        }
    }

    @Override
    public void leave() {
        try {
            operativeLock.lock();
            groupManager.leave();
        }finally {
            operativeLock.unlock();
        }
    }

    @Override
    public void shareOperation(ApplicationPrimitive command, Key key, Value value) {
        try{
            operativeLock.lock();
            //TODO
        }finally {
            operativeLock.unlock();
        }
    }


}
