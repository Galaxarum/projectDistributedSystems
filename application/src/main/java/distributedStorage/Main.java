package distributedStorage;

import distributedStorage.database.DatabaseManager;
import distributedStorage.network.ClientCommandListener;
import lombok.Getter;
import lombok.SneakyThrows;
import middleware.MessagingMiddleware;
import middleware.MessagingMiddlewareImpl;
import middleware.primitives.Primitive;
import primitives.DataOperations;
import templates.ServerSocketRunnable;

import java.io.IOException;
import java.util.logging.Logger;

public class Main {

    /**
     * A logger
     */
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    /**
     * The index in args where the replica Id is expected
     */
    public static final int ID_INDEX = 0;
    /**
     * The index in args where the address of a known replica is expected
     */
    public static final int KNOWN_HOST_INDEX = 1;
    /**
     * The index in args where the port to use for middleware communications is expected
     */
    public static final int MIDDLEWARE_PORT_INDEX = 2;
    /**
     * The index in args where the port to use for communications with the client is expected
     */
    public static final int CLIENT_PORT_INDEX = 3;
    public static final int STORAGE_PATH_INDEX = 4;
    /**
     * The value that will be assigned to an illegal or missing port specification
     */
    public static final int ILLEGAL_PORT = -1;
    /**
     * The string to use in the position {@value KNOWN_HOST_INDEX} when running the very first replica of a group
     */
    public static final String FIRST_REPLICA_DISCRIMINATOR = "first";
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

    /**
     * Starts a {@link ServerSocketRunnable} and (if not the first replica) calls {@link MessagingMiddleware#join()}.
     * Adds {@link MessagingMiddleware#leave()} during the {@link Runtime#addShutdownHook(Thread)} method
     * @param args the command line arguments
     * <table>
     *     <caption>Command line arguments description</caption>
     *     <tr>
     *         <td><b>Index</b></td>
     *         <td><b>Content</b></td>
     *         <td><b>Mandatory</b></td>
     *     </tr>
     *     <tr>
     *         <td>{@value ID_INDEX}</td>
     *         <td>The id of this replica</td>
     *         <td>Y</td>
     *     </tr>
     *     <tr>
     *         <td>{@value KNOWN_HOST_INDEX}</td>
     *         <td>The address of the leader replica</td>
     *         <td>Y</td>
     *     </tr>
     *     <tr>
     *         <td>{@value MIDDLEWARE_PORT_INDEX}</td>
     *         <td>Custom port for communication between replicas. Use {@value ILLEGAL_PORT} to use a default port</td>
     *         <td>N</td>
     *     </tr>
     *     <tr>
     *         <td>{@value CLIENT_PORT_INDEX}</td>
     *         <td>Custom port for client-server communication. Use {@value ILLEGAL_PORT} to use a default port</td>
     *         <td>Y</td>
     *     </tr>
     * </table>
     */
    public static void main(String[] args) throws IOException {
        //Stop if some mandatory parameter is missing
        if(args.length<=Integer.max(ID_INDEX,KNOWN_HOST_INDEX)){
            final String error = "Id or known host missing";
            logger.severe(error);
            throw new IllegalArgumentException(error);
        }

        final String id = args[ID_INDEX];
        final String leaderHost = args[KNOWN_HOST_INDEX];
        final int middlewarePort = parsePortArgs(args,MIDDLEWARE_PORT_INDEX);
        final int clientPort = Integer.parseInt(args[CLIENT_PORT_INDEX]);
        final String persistencePath = args[STORAGE_PATH_INDEX];

        messagingMiddleware = middlewarePort==ILLEGAL_PORT?
                new MessagingMiddlewareImpl<>(id,leaderHost):
                new MessagingMiddlewareImpl<>(id,middlewarePort,leaderHost);
        databaseManager = DatabaseManager.getInstance(persistencePath,String.class,Object.class);

        try {
            new Thread(new ServerSocketRunnable<ClientCommandListener>(clientPort)).start();
            //Very first replica has no group to join
            if(!FIRST_REPLICA_DISCRIMINATOR.equals(leaderHost))
                messagingMiddleware.join();
        }catch (IOException e){
            logger.severe("IO exception occurred at startup");
            e.printStackTrace();
            return;
        }

        //Add a shutdownHook to leave before shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.finest("Leaving the group");
            messagingMiddleware.leave();
            logger.finest("Closing db state");
            databaseManager.close();
            logger.finest("Shutdown completed");
        }));
    }

    /**
     * Handles the possible exceptions raised by calling {@link Integer#parseInt(String)} with {@code args[index]}, returning {@value ILLEGAL_PORT} in case of error
     * @param args Contains the String to parse
     * @param index The index of the String to parse
     * @return {@code Integer.parseInt(args[index])} if the line raises no exception
     * {@value ILLEGAL_PORT} if an {@link ArrayIndexOutOfBoundsException} or a {@link NumberFormatException} is thrown by that line
     */
    private static int parsePortArgs(String[] args, int index){
        try{
            return Integer.parseInt(args[index]);
        }catch (ArrayIndexOutOfBoundsException | NumberFormatException e){
            return ILLEGAL_PORT;
        }
    }

}
