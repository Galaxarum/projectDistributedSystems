package it.polimi.cs.ds.distributed_storage.server.runnables;

import it.polimi.cs.ds.distributed_storage.Primitive;
import it.polimi.cs.ds.distributed_storage.server.functional_interfaces.PrimitiveParser;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketRunnable<T extends Primitive> implements Runnable, Closeable {
    /**
     * Used to accept connections with the other replicas
     */
    private final ServerSocket serverSocket;
    private final PrimitiveParser<T> primitiveParser;

    /**
     * Creates a ConnectionAcceptor listening to the given port
     * @param serverSocket the ServerSocket that will listen for incoming connections
     */
    public ServerSocketRunnable(ServerSocket serverSocket, PrimitiveParser<T> primitiveParser) {
        this.serverSocket = serverSocket;
        this.primitiveParser = primitiveParser;
    }

    /**
     * While {@link #serverSocket} is opened, accepts incoming connections and starts a {@link PrimitiveParserRunnable} using the created Socket
     */
    @Override
    public void run() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(new PrimitiveParserRunnable<>(clientSocket, primitiveParser)).start();
            } catch (IOException ignored) { }
        }

    }

    @Override
    public void close(){
        try {
            serverSocket.close();
        } catch ( IOException ignored ) { }
    }

}

