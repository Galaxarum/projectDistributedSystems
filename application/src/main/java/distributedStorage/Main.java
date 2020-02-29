package distributedStorage;

import distributedStorage.database.DatabaseManager;
import distributedStorage.network.ClientConnectionManager;
import lombok.Getter;
import middleware.MessagingMiddleware;
import middleware.MessagingMiddlewareImpl;

import java.io.IOException;
import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());
    public static final int ID_INDEX = 0;
    public static final int KNOWN_HOST_INDEX = 1;
    public static final int MIDDLEWARE_PORT_INDEX = 2;
    public static final int CLIENT_PORT_INDEX = 3;
    public static final int ILLEGAL_PORT = -1;
    public static final String FIRST_REPLICA_DISCRIMINATOR = "first";
    @Getter
    private static MessagingMiddleware<String,Object> messagingMiddleware;
    @Getter
    private static DatabaseManager databaseManager = new DatabaseManager();

    public static void main(String[] args) {
        if(args.length<=Integer.max(ID_INDEX,KNOWN_HOST_INDEX)){
            final String error = "Id or known host missing";
            logger.severe(error);
            throw new IllegalArgumentException(error);
        }

        final String id = args[ID_INDEX];
        final String knownHost = args[KNOWN_HOST_INDEX];
        final int middlewarePort = parsePortArgs(args,MIDDLEWARE_PORT_INDEX);
        final int clientPort = parsePortArgs(args,CLIENT_PORT_INDEX);
        messagingMiddleware = messagingMiddleware(id, middlewarePort);

        new Thread(clientConnectionManager(clientPort)).start();

        //Very first replica has no group to join
        if(!FIRST_REPLICA_DISCRIMINATOR.equals(knownHost)) try {
            messagingMiddleware.join(knownHost);
        } catch (IOException e) {
            logger.severe("Unable to join the group");
            return;
        }

        //Add a shutdownHook to leave before shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.finest("Leaving the group");
            messagingMiddleware.leave();
            logger.finest("Shutdown completed");
        }));
    }

    private static int parsePortArgs(String[] args, int index){
        try{
            return Integer.parseInt(args[index]);
        }catch (ArrayIndexOutOfBoundsException | NumberFormatException e){
            return ILLEGAL_PORT;
        }
    }

    private static MessagingMiddleware<String,Object> messagingMiddleware(String id, int port){
        return port==ILLEGAL_PORT?
                new MessagingMiddlewareImpl(id):
                new MessagingMiddlewareImpl(id,port);
    }

    private static ClientConnectionManager clientConnectionManager(int port) {
        try {
            return port == ILLEGAL_PORT ?
                    new ClientConnectionManager() :
                    new ClientConnectionManager(port);
        }catch (IOException e){
            logger.severe("cannot start ClientConnectionManager");
            System.exit(0);
            return null;
        }
    }

}
