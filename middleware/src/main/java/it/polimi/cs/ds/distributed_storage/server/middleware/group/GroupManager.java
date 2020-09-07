package it.polimi.cs.ds.distributed_storage.server.middleware.group;

import it.polimi.cs.ds.distributed_storage.exceptions.BrokenProtocolException;
import it.polimi.cs.ds.distributed_storage.server.functional_interfaces.PrimitiveParser;
import it.polimi.cs.ds.distributed_storage.server.middleware.MessagingMiddlewareImpl;
import it.polimi.cs.ds.distributed_storage.server.middleware.messages.MessageBroker;
import it.polimi.cs.ds.distributed_storage.server.runnables.ServerSocketRunnable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;


public abstract class GroupManager implements PrimitiveParser<GroupCommands> {

    protected final String id;
    protected final int port;
    private static ServerSocketRunnable<GroupCommands> socketListener;
    protected final MessageBroker<?> broker;
    public static final Logger logger = Logger.getLogger(GroupManager.class.getName());
    static {
        logger.setParent(MessagingMiddlewareImpl.logger);
    }

    public void leave(){
        socketListener.close();
    }

    GroupManager(String id, int port, MessageBroker<?> broker){
        this.id = id;
        this.port=port;
        this.broker = broker;
        try {
            socketListener = new ServerSocketRunnable<>(new ServerSocket(port), this);
            final ServerSocket messageSocketAcceptor = new ServerSocket(port+NodeInfo.MESSAGES_PORT_OFFSET);
            new Thread(()->{
                logger.info("Started message socket acceptor thread on local port "+messageSocketAcceptor.getLocalPort());
                try {
                    while ( !messageSocketAcceptor.isClosed() ) {
                        final Socket messageSocket = messageSocketAcceptor.accept();
                        logger.info("accepting message socket at address " + messageSocket.getInetAddress().toString());
                        final ObjectOutputStream mOut = new ObjectOutputStream(messageSocket.getOutputStream());
                        final ObjectInputStream mIn = new ObjectInputStream(messageSocket.getInputStream());
                        mOut.flush();
                        final String mId = ( String ) mIn.readObject();
                        broker.getReplicasUnmodifiable().get(mId).setMessageSocket(messageSocket, mOut, mIn);
                        logger.info("created message connection at address "+messageSocket.getInetAddress().toString());
                    }
                    logger.info("closed message socket acceptor thread");
                } catch ( IOException | ClassNotFoundException e ) {
                    e.printStackTrace();
                }

            }).start();
            new Thread(socketListener).start();

        } catch ( IOException e ) {
            throw new BrokenProtocolException("Impossible to initialize the connection", e);
        }
    }
}