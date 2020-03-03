package distributedStorage.network;

import distributedStorage.Main;
import distributedStorage.database.DatabaseManager;
import exceptions.ParsingException;
import middleware.MessagingMiddleware;
import primitives.DataOperations;
import templates.PrimitiveParser;

import java.io.IOException;
import java.net.Socket;

import static primitives.DataOperations.*;

public class ClientCommandListener extends PrimitiveParser<DataOperations> {

    /**
     * Used to communicate with other replicas
     */
    private final MessagingMiddleware<String,Object, DataOperations> messagingMiddleware;
    /**
     * The actual storage
     */
    private final DatabaseManager<String,Object> databaseManager;

    public ClientCommandListener(Socket clientSocket) throws IOException {
        super(clientSocket);
        this.messagingMiddleware = Main.getMessagingMiddleware();
        this.databaseManager = Main.getDatabaseManager();
    }

    @Override
    protected void parseCommand(DataOperations command) throws ParsingException {
        String key = (String) readObjectSafe();
        Object value;
        Object result;
        switch (command) {
            case GET:
                result = databaseManager.getDatabase().get(key);
                //OPT: not needed?
                messagingMiddleware.shareOperation(GET, key, null);
                break;
            case PUT:
                value = readObjectSafe();
                result = databaseManager.getDatabase().put(key, value);
                messagingMiddleware.shareOperation(PUT, key, value);
                break;
            case DELETE:
                result = databaseManager.getDatabase().remove(key);
                messagingMiddleware.shareOperation(DELETE, key, null);
                break;
            default:
                throw new ParsingException(command.toString());
        }
        writeObjectSafe(result);
    }

}
