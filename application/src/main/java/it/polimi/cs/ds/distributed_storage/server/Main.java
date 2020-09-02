package it.polimi.cs.ds.distributed_storage.server;

import it.polimi.cs.ds.distributed_storage.server.database.DatabaseManager;
import it.polimi.cs.ds.distributed_storage.server.middleware.MessagingMiddleware;
import it.polimi.cs.ds.distributed_storage.server.middleware.MessagingMiddlewareImpl;
import it.polimi.cs.ds.distributed_storage.server.primitives.DataOperations;
import it.polimi.cs.ds.distributed_storage.server.primitives.Operation;
import it.polimi.cs.ds.distributed_storage.server.runnables.ServerSocketRunnable;
import lombok.Getter;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.Map;

import static it.polimi.cs.ds.distributed_storage.server.Args.*;

public class Main {

    /**
     * Used to communicate with the other replicas end enforce the required communication properties
     */
    @Getter
    private static MessagingMiddleware<String,Serializable, Operation<String,Serializable>> messagingMiddleware;
    /**
     * Used to manage the local data
     */
    @Getter
    private static DatabaseManager<String,Serializable> databaseManager;

    private static ServerSocketRunnable<DataOperations> clientListener;

    /**
     * Starts a {@link ServerSocketRunnable}.
     * Adds {@link MessagingMiddleware#leave()} during the {@link Runtime#addShutdownHook(Thread)} method
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {

        final Map<Integer,String> argsMap = Args.parse(args);

        final String id = argsMap.get(ID_INDEX);
        final String leader_host = argsMap.get(LEADER_HOST_INDEX);
        final int leader_port = Integer.parseInt(argsMap.get(LEADER_PORT_INDEX));
        final int middleware_port = Integer.parseInt(argsMap.get(MIDDLEWARE_PORT_INDEX));
        final int client_port = Integer.parseInt(argsMap.get(CLIENT_PORT_INDEX));
        final String storage_path = argsMap.get(STORAGE_PATH_INDEX);

        if ( client_port >= middleware_port && client_port < middleware_port + MessagingMiddleware.NEEDED_PORTS ) {
            System.out.println("Conflicting ports. Client port cannot be between [middleware_port,middleware_port+" + (MessagingMiddleware.NEEDED_PORTS - 1) + "]" + System.lineSeparator() +
                    "Actually, " + client_port + " is in [" + middleware_port + "," + (middleware_port + MessagingMiddleware.NEEDED_PORTS - 1) + "]");
            return;
        }

        databaseManager = new DatabaseManager<>(storage_path);


        messagingMiddleware = leader_host==null?
                new MessagingMiddlewareImpl<>(id, middleware_port, () -> databaseManager.getDatabase()):
                new MessagingMiddlewareImpl<>(id, middleware_port, new Socket(leader_host, leader_port), data -> databaseManager.putAll(data));

        clientListener = new ClientListener<>(client_port, messagingMiddleware, databaseManager);

        new Thread(clientListener).start();

        //Add a shutdownHook to leave before shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            clientListener.close();
            databaseManager.close();
            messagingMiddleware.leave();
        }));

    }

}
