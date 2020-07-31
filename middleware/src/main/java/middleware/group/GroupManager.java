package middleware.group;

import exceptions.BrokenProtocolException;
import functional_interfaces.PrimitiveParser;
import middleware.MessagingMiddleware;
import runnables.ServerSocketRunnable;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;


public abstract class GroupManager <K,V> implements PrimitiveParser<GroupCommands> {

    protected String id;
    private static ServerSocketRunnable<GroupCommands> socketListener;
    protected final MessagingMiddleware<K,V,?> owner;

    public void leave(){
        socketListener.close();
    }

    GroupManager(String id, int port, MessagingMiddleware<K, V, ?> owner){
        this.id = id;
        this.owner = owner;
        try {
            socketListener = new ServerSocketRunnable<>(new ServerSocket(port), this);
            new Thread(socketListener).start();
        } catch ( IOException e ) {
            throw  new BrokenProtocolException("Impossible to initialize the connection", e);
        }
    }
}