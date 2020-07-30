package runnables;

import functional_interfaces.PrimitiveParser;
import markers.Primitive;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketRunnable<T extends Primitive> implements Runnable {
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

    public void close(){
        try {
            serverSocket.close();
        } catch ( IOException ignored ) { }
    }

}

