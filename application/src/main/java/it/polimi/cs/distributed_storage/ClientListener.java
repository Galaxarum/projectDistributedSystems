package it.polimi.cs.distributed_storage;

import it.polimi.cs.distributed_storage.database.DatabaseManager;
import it.polimi.cs.distributed_storage.primitives.DataOperations;
import it.polimi.cs.distributed_storage.primitives.Operation;
import it.polimi.cs.ds.distributed_storage.exceptions.BrokenProtocolException;
import it.polimi.cs.ds.distributed_storage.exceptions.ParsingException;
import it.polimi.cs.ds.distributed_storage.middleware.MessagingMiddleware;
import it.polimi.cs.ds.distributed_storage.runnables.ServerSocketRunnable;

import java.io.IOException;
import java.net.ServerSocket;

import static it.polimi.cs.distributed_storage.primitives.DataOperations.DELETE;
import static it.polimi.cs.distributed_storage.primitives.DataOperations.PUT;

public class ClientListener<K,V> extends ServerSocketRunnable<DataOperations> {
	/**
	 * Creates a ConnectionAcceptor listening to the given port
	 */
	public ClientListener(int port,
	                      MessagingMiddleware<K,V,Operation<K,V>> messagingMiddleware,
	                      DatabaseManager<K,V> databaseManager) throws IOException {
		super(new ServerSocket(port), (operation, writer, reader, socket)->{
			try {
				K key = ( K ) reader.readObject();
				V value;
				Object result;
				switch ( operation ) {
					case GET:
						result = databaseManager.getDatabase().get(key);
						break;
					case PUT:
						value = ( V ) reader.readObject();
						result = databaseManager.getDatabase().put(key, value);
						messagingMiddleware.sendMessage(new Operation<K,V>(PUT,key,value));
						break;
					case DELETE:
						result = databaseManager.getDatabase().remove(key);
						messagingMiddleware.sendMessage(new Operation<K,V>(DELETE, key, null));
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
