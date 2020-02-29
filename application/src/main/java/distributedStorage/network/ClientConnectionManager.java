package distributedStorage.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Establishes new connections with clients
 */
public class ClientConnectionManager implements Runnable {
    /**
     * Used to accept new connections
     */
    private final ServerSocket serverSocket;
    /**
     * Default port to listen for clients if the port is not specified
     */
    public static final int DEFAULT_PORT = 12346;
    /**
     * Logger of the class
     */
    private static final Logger logger = Logger.getLogger(ClientConnectionManager.class.getName());

    /**
     * Creates a ClientConnectionManager listening over the default port
     * @see #ClientConnectionManager(int)
     * @throws IOException if an I/O error occurs when opening {@link #serverSocket}
     */
    public ClientConnectionManager() throws IOException {
        this(DEFAULT_PORT);
    }

    /**
     * Creates a ClientConnectionManager listening over the given port
     * @param port the port to listen to
     * @throws IOException if an I/O error occurs when opening {@link #serverSocket}
     */
    public ClientConnectionManager(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    /**
     * While {@link #serverSocket} is opened, accepts incoming connections starting a new Thread running a {@link ClientCommandListener} to manage the connected client
     * If some {@link IOException} is raised while accepting the connection, the request is skipped and da failure is logged
     * @see ClientCommandListener
     */
    @Override
    public void run() {
        while (!serverSocket.isClosed()){
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientCommandListener(clientSocket)).start();
            }catch (IOException e){
                logger.fine("Failed to establish connection with client");
            }
        }
    }
}
