package distributedStorage.network;

import distributedStorage.Main;
import distributedStorage.database.DatabaseManager;
import exceptions.ParsingException;
import middleware.MessagingMiddleware;
import middleware.primitives.DataOperations;
import templates.PrimitiveParser;

import java.io.IOException;
import java.net.Socket;

import static middleware.primitives.DataOperations.*;

public class ClientCommandListener extends PrimitiveParser<DataOperations> {

    /**
     * Used to communicate with other replicas
     */
    private final MessagingMiddleware<String,Object> messagingMiddleware = Main.getMessagingMiddleware();
    /**
     * The actual storage
     */
    private final DatabaseManager databaseManager = Main.getDatabaseManager();

    public ClientCommandListener(Socket clientSocket) throws IOException {
        super(clientSocket);
    }

    @Override
    protected void parseCommand(DataOperations command) throws ParsingException {
        //TODO: actuate operations
        switch (command) {
            case GET:

                messagingMiddleware.shareOperation(GET, null, null);
                break;
            case PUT:

                messagingMiddleware.shareOperation(PUT, null, null);
                break;
            case DELETE:

                messagingMiddleware.shareOperation(DELETE, null, null);
                break;
            default:
                throw new ParsingException(command.toString());
        }
    }
}
