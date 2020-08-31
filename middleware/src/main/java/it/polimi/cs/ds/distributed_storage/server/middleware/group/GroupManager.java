package it.polimi.cs.ds.distributed_storage.server.middleware.group;

import it.polimi.cs.ds.distributed_storage.server.exceptions.BrokenProtocolException;
import it.polimi.cs.ds.distributed_storage.server.functional_interfaces.PrimitiveParser;
import it.polimi.cs.ds.distributed_storage.server.middleware.messages.MessageBroker;
import it.polimi.cs.ds.distributed_storage.server.runnables.ServerSocketRunnable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public abstract class GroupManager <K,V> implements PrimitiveParser<GroupCommands> {

    protected String id;
    private static ServerSocketRunnable<GroupCommands> socketListener;
    protected final MessageBroker<?> broker;

    public void leave(){
        socketListener.close();
    }

    GroupManager(String id, int port, MessageBroker<?> broker){
        this.id = id;
        this.broker = broker;
        try {
            socketListener = new ServerSocketRunnable<>(new ServerSocket(port), this);
            final ServerSocket messageSocketAcceptor = new ServerSocket(port+NodeInfo.MESSAGES_PORT_OFFSET);
            new Thread(()->{
                try {
                    final Socket messageSocket = messageSocketAcceptor.accept();
                    final ObjectOutputStream mOut = new ObjectOutputStream(messageSocket.getOutputStream());
                    final ObjectInputStream mIn = new ObjectInputStream(messageSocket.getInputStream());
                    final String mId = mIn.readUTF();
                    broker.getReplicasUnmodifiable().get(mId).setMessageSocket(messageSocket,mOut,mIn);
                } catch ( IOException e ) {
                    e.printStackTrace();
                }

            }).start();
            new Thread(socketListener).start();

        } catch ( IOException e ) {
            throw new BrokenProtocolException("Impossible to initialize the connection", e);
        }
    }
}