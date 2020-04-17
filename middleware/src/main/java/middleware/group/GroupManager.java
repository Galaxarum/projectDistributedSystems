package middleware.group;

import exceptions.BrokenProtocolException;
import functional_interfaces.PrimitiveParser;
import middleware.messages.VectorClocks;
import runnables.ServerSocketRunnable;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;


public abstract class GroupManager <K,V> implements PrimitiveParser<GroupCommands> {
    private static final Logger logger = Logger.getLogger(GroupManager.class.getName());
    protected static String id;
    protected static Map<String, NodeInfo> replicas;
    protected static Map data;

    public abstract void join(VectorClocks vectorClocks);
    public abstract void leave();

    GroupManager(String id, int port, Map<String, NodeInfo> replicas,Map<K,V> data){
        GroupManager.id = id;
        GroupManager.replicas = replicas;
        GroupManager.data = data;
        try {
            logger.info("Starting socket listener");
            new Thread(new ServerSocketRunnable<>(port, this)).start();
            logger.info("Socket listener started");
        } catch ( IOException e ) {
            throw new BrokenProtocolException("Impossible to initialize the connection", e);
        }
    }
}