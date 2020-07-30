package distributedStorage;

import distributedStorage.database.DatabaseManager;
import distributedStorage.primitives.DataOperations;
import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import middleware.MessagingMiddleware;
import runnables.ServerSocketRunnable;

import java.io.IOException;
import java.net.ServerSocket;

import static distributedStorage.primitives.DataOperations.DELETE;
import static distributedStorage.primitives.DataOperations.PUT;

public class ClientListener extends ServerSocketRunnable<DataOperations> {
	/**
	 * Creates a ConnectionAcceptor listening to the given port
	 */
	public ClientListener(int port,
	                      MessagingMiddleware<String,Object,DataOperations> messagingMiddleware,
	                      DatabaseManager<String,Object> databaseManager) throws IOException {
		super(new ServerSocket(port), (operation, writer, reader, socket)->{
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
	}
}
