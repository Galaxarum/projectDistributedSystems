package distributedStorage;

import distributedStorage.database.DatabaseManager;
import distributedStorage.primitives.DataOperations;
import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import lombok.Getter;
import middleware.MessagingMiddleware;
import middleware.MessagingMiddlewareImpl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import runnables.ServerSocketRunnable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static distributedStorage.primitives.DataOperations.DELETE;
import static distributedStorage.primitives.DataOperations.PUT;

public class Main {

    /**
     * A logger
     */
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    public static final Level LOG_LEVEL = Level.ALL;
    /**
     * The index in args where the replica Id is expected
     */
    public static final int ID_INDEX = 0;
    private static String id;
    /**
     * The index in args where the address of a known replica is expected
     */
    public static final int LEADER_HOST_INDEX = 1;
    private static String leader_host;
    public static final int LEADER_PORT_INDEX = 2;
    private static int leader_port;
    /**
     * The index in args where the port to use for middleware communications is expected
     */
    public static final int MIDDLEWARE_PORT_INDEX = 3;
    private static int middleware_port;
    /**
     * The index in args where the port to use for communications with the client is expected
     */
    public static final int CLIENT_PORT_INDEX = 4;
    private static int client_port;
    public static final int STORAGE_PATH_INDEX = 5;
    private static String storage_path;
    private static boolean isLeader;

    /**
     * The value that will be assigned to an illegal or missing port specification
     */
    public static final String DEFAULT_STRING = "d";
    /**
     * The string to use in the position {@value LEADER_HOST_INDEX} when running the very first replica of a group
     */
    public static final String FIRST_REPLICA_DISCRIMINATOR = "first";
    public static final int DEFAULT_CLIENT_PORT = 12350;

    public static final String ARGS_DIGEST = "To start the application provide the following arguments in the following order"+System.lineSeparator()+
            "Id_of_this_node, " +
            "Address_of_the_leader_replica(use \""+FIRST_REPLICA_DISCRIMINATOR+"\" when starting the first replica), " +
            "[Port_of_the_leader_replica | " + DEFAULT_STRING + "], "+
            "[Port_for_communication_with_other_replicas | " + DEFAULT_STRING +"], " +
            "Port_for_communication_with_clients | " + DEFAULT_STRING +"], " +
            "Path_to_file_used_for_data_persistence | " + DEFAULT_STRING +"]";
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
     *         <td>{@value LEADER_HOST_INDEX}</td>
     *         <td>The address of the leader replica</td>
     *     </tr>
     *     <tr>
     *         <td>{@value MIDDLEWARE_PORT_INDEX}</td>
     *         <td>Custom port for communication between replicas. Use {@value DEFAULT_STRING} to use a default port</td>
     *     </tr>
     *     <tr>
     *         <td>{@value CLIENT_PORT_INDEX}</td>
     *         <td>Custom port for client-server communication. Use {@value DEFAULT_STRING} to use a default port</td>
     *     </tr>
     * </table>
     */
    public static void main(String[] args) throws IOException, IllegalAccessException {
        try {
            initClientListener();

            logger.setLevel(LOG_LEVEL);
            ConsoleHandler handler = new ConsoleHandler();
            handler.setLevel(LOG_LEVEL);
            logger.addHandler(handler);

            isLeader = args[LEADER_HOST_INDEX].equals(FIRST_REPLICA_DISCRIMINATOR);

            parse(args);

            if ( client_port >= middleware_port && client_port < middleware_port + MessagingMiddleware.NEEDED_PORTS ) {
                logger.severe("Conflicting ports. Client port cannot be between [middleware_port,middleware_port+" + (MessagingMiddleware.NEEDED_PORTS - 1) + "]" + System.lineSeparator() +
                        "Actually, " + client_port + " is in [" + middleware_port + "," + (middleware_port + MessagingMiddleware.NEEDED_PORTS - 1) + "]");
                return;
            }

            databaseManager = DatabaseManager.getInstance(storage_path);
            final Socket leaderSocket  = isLeader?
                    null:
                    new Socket(leader_host,leader_port);
            messagingMiddleware = new MessagingMiddlewareImpl<>(id, middleware_port, leaderSocket, databaseManager.getDatabase());

            try {
                startClientListener();
            } catch ( IOException e ) {
                logger.throwing(Main.class.getName(), "startClientListener", e);
                return;
            }
            messagingMiddleware.join();

            setShutdownOperation();

            logger.exiting(Main.class.getName(), "main", "Started successful");
        }catch ( BrokenProtocolException e ){
            e.printStackTrace();
            logger.severe(ARGS_DIGEST);
        }
        logger.info("Running on ports: "+client_port+", "+middleware_port+"-"+(middleware_port+MessagingMiddleware.NEEDED_PORTS-1));
    }

    private static void initClientListener(){
        try {
            clientListener = new ServerSocketRunnable<>(new ServerSocket(client_port), (operation, writer, reader, socket) -> {
                try {
                    String key = ( String ) reader.readObject();
                    Object value;
                    Object result;
                    switch ( operation ) {
                        case GET:
                            result = databaseManager.getDatabase().get(key);
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
                } catch ( IOException | ClassNotFoundException e ) {
                    throw new BrokenProtocolException("Something went wrong with communication",e);
                }
            });
        }catch ( IOException e ){
            throw new BrokenProtocolException("Cannot start Client Listener", e);
        }
    }

    private static void parse(@NotNull String[] args){
        logger.entering(Main.class.getName(),"parse",args);
        try{
            id = args[ID_INDEX];
            leader_host = isLeader ?
                    null :
                    args[LEADER_HOST_INDEX];
            leader_port = isValidArg(args,LEADER_PORT_INDEX)?
                            Integer.parseInt(args[LEADER_PORT_INDEX]):
                            MessagingMiddleware.DEFAULT_STARTING_PORT;
            middleware_port = isValidArg(args,MIDDLEWARE_PORT_INDEX) ?
                    Integer.parseInt(args[MIDDLEWARE_PORT_INDEX]):
                    MessagingMiddleware.DEFAULT_STARTING_PORT;
            client_port = isValidArg(args,CLIENT_PORT_INDEX) ?
                    Integer.parseInt(args[CLIENT_PORT_INDEX]) :
                    DEFAULT_CLIENT_PORT;
            storage_path = isValidArg(args,STORAGE_PATH_INDEX) ?
                    args[STORAGE_PATH_INDEX] :
                    DatabaseManager.DEFAULT_PATH;
        }catch (ArrayIndexOutOfBoundsException | NumberFormatException e){
            IllegalArgumentException e1 = new IllegalArgumentException(e);
            logger.throwing(Main.class.getName(),"parse",e1);
            logger.severe(ARGS_DIGEST);
            System.out.println(ARGS_DIGEST);
            throw e1;
        }
        logger.exiting(Main.class.getName(),"parse");
    }

    @Contract(pure = true)
    private static boolean isValidArg(@NotNull String[] args, int index){
        return args.length>index && !args[index].equals(DEFAULT_STRING);
    }

    private static void startClientListener() throws IOException {
        logger.entering(Main.class.getName(),"startClientListener",client_port);
        new Thread(clientListener).start();
        logger.exiting(Main.class.getName(),"startClientListener");
    }

    private static void setShutdownOperation(){
        logger.entering(Main.class.getName(),"setShutdownOperation");
        //Add a shutdownHook to leave before shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping client listener");
            clientListener.close();
            logger.info("Closing db state");
            databaseManager.close();
            logger.info("Leaving the group");
            messagingMiddleware.leave();
            logger.info("Shutdown completed");
        }));
        logger.exiting(Main.class.getName(),"setShutdownOperation");
    }
}
