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

import static distributedStorage.primitives.DataOperations.DELETE;
import static distributedStorage.primitives.DataOperations.PUT;

public class Main {

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
     */
    public static void main(String[] args) throws IOException, IllegalAccessException {
        initClientListener();

        isLeader = args[LEADER_HOST_INDEX].equals(FIRST_REPLICA_DISCRIMINATOR);

        parse(args);

        if ( client_port >= middleware_port && client_port < middleware_port + MessagingMiddleware.NEEDED_PORTS ) {
            System.out.println("Conflicting ports. Client port cannot be between [middleware_port,middleware_port+" + (MessagingMiddleware.NEEDED_PORTS - 1) + "]" + System.lineSeparator() +
                    "Actually, " + client_port + " is in [" + middleware_port + "," + (middleware_port + MessagingMiddleware.NEEDED_PORTS - 1) + "]");
            return;
        }

        databaseManager = DatabaseManager.getInstance(storage_path);
        final Socket leaderSocket  = isLeader?
                null:
                new Socket(leader_host,leader_port);
        messagingMiddleware = new MessagingMiddlewareImpl<>(id, middleware_port, leaderSocket, databaseManager.getDatabase());

        new Thread(clientListener).start();

        messagingMiddleware.join();

        setShutdownOperation();

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
            System.out.println(ARGS_DIGEST);
            throw e1;
        }
    }

    @Contract(pure = true)
    private static boolean isValidArg(@NotNull String[] args, int index){
        return args.length>index && !args[index].equals(DEFAULT_STRING);
    }

    private static void setShutdownOperation(){
        //Add a shutdownHook to leave before shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            clientListener.close();
            databaseManager.close();
            messagingMiddleware.leave();
        }));
    }
}
