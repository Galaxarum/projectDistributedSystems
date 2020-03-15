package runnables;

import functional_interfaces.ParsingFunction;
import markers.Primitive;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class ServerSocketRunnable<T extends Primitive> implements Runnable {
    /**
     * Used to accept connections with the other replicas
     */
    private final ServerSocket serverSocket;
    private final ParsingFunction<T> parsingFunction;
    /**
     * A logger
     */
    private static final Logger logger = Logger.getLogger(ServerSocketRunnable.class.getName());

    /**
     * Creates a ConnectionAcceptor listening to the given port
     *
     * @param port The port to listen to
     * @throws IOException If thrown by {@linkplain ServerSocket#ServerSocket(int)}
     */
    public ServerSocketRunnable(int port,ParsingFunction<T> parsingFunction) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.parsingFunction = parsingFunction;
    }

    /**
     * While {@link #serverSocket} is opened, accepts incoming connections and starts a {@link PrimitiveParser} using the created Socket
     */
    @Override
    public void run() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(new PrimitiveParser<>(clientSocket, parsingFunction)).start();
            } catch (IOException e) {
                logger.warning("An error occurred while accepting a connection by " + serverSocket);
            }
        }

    }

}

