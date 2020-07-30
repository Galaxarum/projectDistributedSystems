package distributedStorage;

import distributedStorage.database.DatabaseManager;
import distributedStorage.primitives.DataOperations;
import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import lombok.Getter;
import middleware.MessagingMiddleware;
import middleware.MessagingMiddlewareImpl;
import runnables.ServerSocketRunnable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import static distributedStorage.Args.*;
import static distributedStorage.primitives.DataOperations.DELETE;
import static distributedStorage.primitives.DataOperations.PUT;

public class Main {

    /**
     * Used to communicate with the other replicas end enforce the required communication properties
     */
    @Getter
    private static MessagingMiddleware<String,Object,DataOperations> messagingMiddleware;
    /**
     * Used to manage the local data
     */
    @Getter
    private static DatabaseManager<String,Object> databaseManager;

    private static ServerSocketRunnable<DataOperations> clientListener;

    /**
     * Starts a {@link ServerSocketRunnable} and (if not the first replica) calls {@link MessagingMiddleware#join()}.
     * Adds {@link MessagingMiddleware#leave()} during the {@link Runtime#addShutdownHook(Thread)} method
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, IllegalAccessException {

        final Map<Integer,String> argsMap = Args.parse(args);

        final String id = argsMap.get(ID_INDEX);
        final String leader_host = argsMap.get(LEADER_HOST_INDEX);
        final int leader_port = Integer.parseInt(argsMap.get(LEADER_PORT_INDEX));
        final int middleware_port = Integer.parseInt(argsMap.get(MIDDLEWARE_PORT_INDEX));
        final int client_port = Integer.parseInt(argsMap.get(CLIENT_PORT_INDEX));
        final String storage_path = argsMap.get(STORAGE_PATH_INDEX);

        clientListener = new ClientListener(client_port,messagingMiddleware,databaseManager);

        if ( client_port >= middleware_port && client_port < middleware_port + MessagingMiddleware.NEEDED_PORTS ) {
            System.out.println("Conflicting ports. Client port cannot be between [middleware_port,middleware_port+" + (MessagingMiddleware.NEEDED_PORTS - 1) + "]" + System.lineSeparator() +
                    "Actually, " + client_port + " is in [" + middleware_port + "," + (middleware_port + MessagingMiddleware.NEEDED_PORTS - 1) + "]");
            return;
        }

        databaseManager = DatabaseManager.getInstance(storage_path);
        final Socket leaderSocket  = leader_host ==null?
                null:
                new Socket(leader_host,leader_port);
        messagingMiddleware = new MessagingMiddlewareImpl<>(id, middleware_port, leaderSocket, databaseManager.getDatabase());

        new Thread(clientListener).start();

        messagingMiddleware.join();

        //Add a shutdownHook to leave before shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            clientListener.close();
            databaseManager.close();
            messagingMiddleware.leave();
        }));

    }

}
