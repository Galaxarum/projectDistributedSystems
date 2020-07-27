package middleware.group;

import exceptions.BrokenProtocolException;
import functional_interfaces.PrimitiveParser;
import middleware.messages.VectorClock;
import runnables.ServerSocketRunnable;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.logging.Logger;


public abstract class GroupManager <K,V> implements PrimitiveParser<GroupCommands> {
    private static final Logger logger = Logger.getLogger(GroupManager.class.getName());
    protected static String id;
    protected static Map<String, NodeInfo> replicas;
    protected static Map data;
    private static ServerSocketRunnable<GroupCommands> socketListener;

    public abstract void join(VectorClock vectorClock);
    public void leave(){
        socketListener.close();
    };

    GroupManager(String id, int port, Map<String, NodeInfo> replicas,Map<K,V> data){
        logger.entering(GroupManager.class.getName(),"GroupManager");
        GroupManager.id = id;
        GroupManager.replicas = replicas;
        GroupManager.data = data;
        try {
            socketListener = new ServerSocketRunnable<>(new ServerSocket(port), this);
            new Thread(socketListener).start();
        } catch ( IOException e ) {
            BrokenProtocolException e1 = new BrokenProtocolException("Impossible to initialize the connection", e);
            logger.throwing(GroupManager.class.getName(),"GroupManager",e1);
            throw e1;
        }
        logger.exiting(GroupManager.class.getName(),"GroupManager");
    }
}