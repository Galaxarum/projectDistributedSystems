package distributedStorage.network;

import distributedStorage.Main;
import distributedStorage.database.DatabaseManager;
import middleware.MessagingMiddleware;
import middleware.primitives.DataOperations;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.logging.Logger;

import static middleware.primitives.DataOperations.*;

public class ClientCommandListener implements Runnable {

    /**
     * The connection to the client
     */
    private final Socket clientSocket;
    /**
     * Used to read messages from the client
     */
    private final ObjectInputStream in;
    /**
     * Used to write messages to the client
     */
    private final ObjectOutputStream out;
    /**
     * Used to communicate with other replicas
     */
    private final MessagingMiddleware<String,Object> messagingMiddleware = Main.getMessagingMiddleware();
    /**
     * The actual storage
     */
    private final DatabaseManager databaseManager = Main.getDatabaseManager();
    /**
     * A logger
     */
    private static final Logger logger  = Logger.getLogger(ClientCommandListener.class.getName());

    /**
     * Initializes {@link #clientSocket}, {@link #in}, {@link #out} using the given socket
     * @param clientSocket the socket with the client pc
     * @throws IOException if raised by {@link Socket#getInputStream()} or by {@link Socket#getOutputStream()}
     */
    public ClientCommandListener(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.in = new ObjectInputStream(clientSocket.getInputStream());
        this.out = new ObjectOutputStream(clientSocket.getOutputStream());
    }

    /**
     * As long as {@link #clientSocket} is opened, uses {@link #readCommand()} to acquire a new command and
     * TODO: parse the command
     */
    @Override
    public void run() {
        logger.finest("Listening to client at address: "+clientSocket.getInetAddress());
        while (!clientSocket.isClosed()){
            Optional<DataOperations> optionalCommand = readCommand();
            optionalCommand.ifPresent(command-> {
                if(GET.equals(command)){

                    messagingMiddleware.shareOperation(GET,null,null);
                }else if(PUT.equals(command)){

                    messagingMiddleware.shareOperation(PUT,null,null);
                }else if(DELETE.equals(command)){

                    messagingMiddleware.shareOperation(DELETE,null,null);
                }else {
                    logger.warning("Unexpected command (" + command + ") received by the address " + clientSocket.getInetAddress() + "." + System.lineSeparator() +
                            "The connection will be interrupted");
                    stop();
                }
            });
        }

    }

    /**
     * Reads a command from {@link #in} and returns it wrapped into an Optional.
     * If {@link ObjectInputStream#readObject()} throws an exception, it's logged, {@link #stop()} is called, and {@link Optional#empty()} is returned.
     * @return An optional containing a {@link DataOperations}, if it existed and no exception was thrown by {@link #in}.
     * An empty optional otherwise
     */
    private Optional<DataOperations> readCommand(){
        try {
            return Optional.ofNullable((DataOperations) in.readObject());
        } catch (IOException e) {
            logger.fine("IOException happened when reading the following stream: "+in.toString()+System.lineSeparator()+
                    "The connection will be interrupted");
            stop();
        } catch (ClassNotFoundException e) {
            logger.warning("Unable to deserialize an object received by the following stream: "+in.toString()+System.lineSeparator()+
                    "The connection will be interrupted");
            stop();
        }
        return Optional.empty();
    }

    /**
     * Closes {@link #in}, {@link #out}, {@link #clientSocket}.
     */
    private void stop(){
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            logger.finer("IOException raised while closing an IO channel. Not a problem");
        }
        logger.finest("Stopped listening to the client at the address: "+clientSocket.getInetAddress());
    }
}
