package middleware;

import markers.Primitive;
import middleware.group.*;
import middleware.messages.VectorClocks;

import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

public class MessagingMiddlewareImpl<Key, Value, ApplicationPrimitive extends Enum<ApplicationPrimitive> & Primitive> implements
        MessagingMiddleware<Key, Value, ApplicationPrimitive> {

    public static final Map<String, NodeInfo> replicas = new Hashtable<>();
    private final Map<Key,Value> data;
    private VectorClocks vectorClocks;
    private final GroupManager<Key, Value> groupManager;
    private static final Logger logger = Logger.getLogger(MessagingMiddlewareImpl.class.getName());


    public MessagingMiddlewareImpl(String id, int port, String leaderHost, Map<Key, Value> data) {
        this.data = data;
        logger.info("creating group manager");
        this.groupManager = GroupManagerFactory.factory(id,port,leaderHost,replicas,data);
        this.vectorClocks = new VectorClocks(id);
    }

    @Override
    public void join() {
        logger.info("joining group");
        try {
            operativeLock.lock();
            logger.fine("locked ordinary operations");
            groupManager.join(vectorClocks);
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
