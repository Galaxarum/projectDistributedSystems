package templates;

import middleware.networkThreads.P2PConnection;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class ServerSocketRunnable<T extends PrimitiveParser> implements Runnable {
    /**
     * Used to accept connections with the other replicas
     */
    private final ServerSocket serverSocket;
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
    public ServerSocketRunnable(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    /**
     * While {@link #serverSocket} is opened, accepts incoming connections and starts a {@link P2PConnection} using the created Socket
     */
    @Override
    public void run() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(getInstanceOfT(clientSocket)).start();
            } catch (IOException e) {
                logger.warning("An error occurred while accepting a connection by " + serverSocket);
            }
        }

    }

    private T getInstanceOfT(Socket socket) throws IOException {
        try {
            ParameterizedType superClass = (ParameterizedType) getClass().getGenericSuperclass();
            Class<T> type = (Class<T>) superClass.getActualTypeArguments()[0];
            return type.getConstructor(Socket.class).newInstance(socket);
        } catch (Exception e) {
            String errorMsg = "Unable to reflectively construct an instance of generic parameter T. Printing exception stack trace";
            logger.severe(errorMsg);
            e.printStackTrace();
            throw new IOException(errorMsg,e);
        }
    }
}

