package it.polimi.cs.ds.distributed_storage.server;

import it.polimi.cs.ds.distributed_storage.server.database.DatabaseManager;
import it.polimi.cs.ds.distributed_storage.server.exceptions.BrokenProtocolException;
import it.polimi.cs.ds.distributed_storage.server.middleware.MessagingMiddleware;
import it.polimi.cs.ds.distributed_storage.server.primitives.DataOperations;
import it.polimi.cs.ds.distributed_storage.server.primitives.Operation;
import it.polimi.cs.ds.distributed_storage.server.runnables.ServerSocketRunnable;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.util.logging.Logger;

import static it.polimi.cs.ds.distributed_storage.server.primitives.DataOperations.DELETE;
import static it.polimi.cs.ds.distributed_storage.server.primitives.DataOperations.PUT;

public class ClientListener<K extends Serializable, V extends Serializable> extends ServerSocketRunnable<DataOperations> {
	public static final Logger logger = Logger.getLogger(ClientListener.class.getName());
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
				logger.info("Received "+operation+" "+key);
				switch ( operation ) {
					case GET:
						result = databaseManager.getDatabase().get(key);
						logger.info("executed in db");
						break;
					case PUT:
						value = ( V ) reader.readObject();
						result = databaseManager.getDatabase().put(key, value);
						logger.info("executed in db");
						messagingMiddleware.sendMessage(new Operation<>(PUT, key, value));
						break;
					case DELETE:
						result = databaseManager.getDatabase().remove(key);
						logger.info("executed in db");
						messagingMiddleware.sendMessage(new Operation<>(DELETE, key, null));
						break;
					default:
						result = "Illegal command";
				}
				writer.writeObject(result);
			} catch ( IOException | ClassNotFoundException e ) {
				throw new BrokenProtocolException("Something went wrong with communication",e);
			}
		});
	}
}
