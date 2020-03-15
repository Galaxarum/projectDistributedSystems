package distributedStorage;

import distributedStorage.database.DatabaseManager;
import exceptions.ParsingException;
import lombok.Getter;
import middleware.MessagingMiddleware;
import middleware.MessagingMiddlewareImpl;
import distributedStorage.primitives.DataOperations;
import runnables.ServerSocketRunnable;

import java.io.IOException;
import java.util.logging.Logger;

import static distributedStorage.primitives.DataOperations.*;

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

    public static final String ARGS_DIGEST = "To start the application provide the following arguments in the following order (values in square braces sets the actual value to a default one): "+System.lineSeparator()+
            "Id_of_this_node " +
            "Address_of_the_leader_replica(use \""+FIRST_REPLICA_DISCRIMINATOR+"\" when starting the first replica) " +
            "Port_for_communication_with_other_replicas["+ILLEGAL_PORT+"] " +
            "Port_for_communication_with_clients["+ILLEGAL_PORT+"] " +
            "Path_to_file_used_for_data_persistence";
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
     *     </tr>
     *     <tr>
     *         <td>{@value ID_INDEX}</td>
     *         <td>The id of this replica</td>
     *     </tr>
     *     <tr>
     *         <td>{@value KNOWN_HOST_INDEX}</td>
     *         <td>The address of the leader replica</td>
     *     </tr>
     *     <tr>
     *         <td>{@value MIDDLEWARE_PORT_INDEX}</td>
     *         <td>Custom port for communication between replicas. Use {@value ILLEGAL_PORT} to use a default port</td>
     *     </tr>
     *     <tr>
     *         <td>{@value CLIENT_PORT_INDEX}</td>
     *         <td>Custom port for client-server communication. Use {@value ILLEGAL_PORT} to use a default port</td>
     *     </tr>
     * </table>
     */
    public static void main(String[] args) throws IOException, IllegalAccessException {
        try {

            final String id = args[ID_INDEX];
            final String leaderHost = args[KNOWN_HOST_INDEX];
            final int middlewarePort = Integer.parseInt(args[MIDDLEWARE_PORT_INDEX]);
            final int clientPort = Integer.parseInt(args[CLIENT_PORT_INDEX]);
            final String persistencePath = args[STORAGE_PATH_INDEX];

            messagingMiddleware = middlewarePort == ILLEGAL_PORT ?
                    new MessagingMiddlewareImpl<>(id, leaderHost) :
                    new MessagingMiddlewareImpl<>(id, middlewarePort, leaderHost);
            databaseManager = DatabaseManager.getInstance(persistencePath);

            try {
                //TODO: this is just a template
                new Thread(new ServerSocketRunnable<DataOperations>(clientPort,(operation,writer,reader)->{
                    String key = (String) reader.readObject();
                    Object value;
                    Object result;
                    switch (operation) {
                        case GET:
                            result = databaseManager.getDatabase().get(key);
                            //OPT: not needed?
                            messagingMiddleware.shareOperation(GET, key, null);
                            break;
                        case PUT:
                            value = reader.readObject();
                            result = databaseManager.getDatabase().put(key, value);
                            messagingMiddleware.shareOperation(PUT, key, value);
                            break;
                        case DELETE:
                            result = databaseManager.getDatabase().remove(key);
                            messagingMiddleware.shareOperation(DELETE, key, null);
                            break;
                        default:
                            throw new ParsingException(operation.toString());
                    }
                    writer.writeObject(result);
                })).start();

                //Very first replica has no group to join
                if (!FIRST_REPLICA_DISCRIMINATOR.equals(leaderHost))
                    messagingMiddleware.join();
            } catch (IOException e) {
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
        }catch (ArrayIndexOutOfBoundsException | NumberFormatException e){
            logger.severe(ARGS_DIGEST);
            System.out.println(ARGS_DIGEST);
        }
    }
}
