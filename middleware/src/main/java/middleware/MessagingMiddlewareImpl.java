package middleware;

import markers.Primitive;
import middleware.group.GroupManager;
import middleware.group.LeaderGroupManager;
import middleware.group.NodeInfo;
import middleware.group.OrdinaryGroupManager;

import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

public class MessagingMiddlewareImpl<Key, Value, ApplicationPrimitive extends Enum<ApplicationPrimitive> & Primitive> implements MessagingMiddleware<Key, Value, ApplicationPrimitive> {

    public static final Map<String, NodeInfo> replicas = new Hashtable<>();
    private final GroupManager<Key, Value> groupManager;
    private static final Logger logger = Logger.getLogger(MessagingMiddlewareImpl.class.getName());


    public MessagingMiddlewareImpl(String id, int port, String leaderHost) {
        logger.info("creating group manager");
        this.groupManager = leaderHost == null ?
                new LeaderGroupManager<>(id, port, replicas) :
                new OrdinaryGroupManager<>(id, port, leaderHost, replicas);
    }

    public MessagingMiddlewareImpl(String id, String leaderHost) {
        this(id, DEFAULT_PORT, leaderHost);
    }

    @Override
    public Map<Key, Value> join() {
        logger.info("joining group");
        try {
            operativeLock.lock();
            logger.fine("locked ordinary operations");
            return groupManager.join();
        }finally {
            operativeLock.unlock();
            logger.fine("unlocked ordinary operations");
            logger.info("joined");
        }
    }

    @Override
    public void leave() {
        try {
            logger.info("leaving the group");
            operativeLock.lock();
            logger.fine("locked ordinary operations");
            groupManager.leave();
            logger.info("left");
        }finally {
            operativeLock.unlock();
            logger.fine("unlocked ordinary operations");
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
