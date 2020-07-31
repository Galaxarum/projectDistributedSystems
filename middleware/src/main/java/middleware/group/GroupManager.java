package middleware.group;

import exceptions.BrokenProtocolException;
import functional_interfaces.PrimitiveParser;
import middleware.messages.VectorClock;
import runnables.ServerSocketRunnable;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;


public abstract class GroupManager <K,V> implements PrimitiveParser<GroupCommands> {

    protected static String id;
    protected static Map<String, NodeInfo> replicas;
    protected static Map data;
    private static ServerSocketRunnable<GroupCommands> socketListener;

    public void leave(){
        socketListener.close();
    };

    GroupManager(String id, int port, Map<String, NodeInfo> replicas,Map<K,V> data){
        GroupManager.id = id;
        GroupManager.replicas = replicas;
        GroupManager.data = data;
        try {
            socketListener = new ServerSocketRunnable<>(new ServerSocket(port), this);
            new Thread(socketListener).start();
        } catch ( IOException e ) {
            throw  new BrokenProtocolException("Impossible to initialize the connection", e);
        }
    }
}