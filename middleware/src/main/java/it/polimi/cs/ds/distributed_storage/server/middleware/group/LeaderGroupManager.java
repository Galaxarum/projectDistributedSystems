package it.polimi.cs.ds.distributed_storage.server.middleware.group;

import it.polimi.cs.ds.distributed_storage.server.exceptions.BrokenProtocolException;
import it.polimi.cs.ds.distributed_storage.server.exceptions.ParsingException;
import it.polimi.cs.ds.distributed_storage.server.middleware.messages.MessageBroker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Hashtable;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class LeaderGroupManager<K, V> extends GroupManager {
	private final Supplier<Hashtable<K, V>> dataSupplier;
	public static final Logger logger = Logger.getLogger(LeaderGroupManager.class.getName());
	static {
		logger.setParent(GroupManager.logger);
	}

	public LeaderGroupManager(String id, int port, MessageBroker<?> broker,Supplier<Hashtable<K,V>> dataSupplier) {
		super(id, port, broker);
		this.dataSupplier = dataSupplier;
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public synchronized void leave() {
		throw new UnsupportedOperationException("The leader cannot leave the group");
	}

	@Override
	public synchronized void parse(GroupCommands command, ObjectOutputStream out, ObjectInputStream in, Socket socket) throws ParsingException {
		try {
			final String replicaId;
			final NodeInfo replicaInfo;
			logger.info("Parsing "+command);
			switch ( command ) {
				case JOIN:
					//Register the replica
					replicaInfo = new NodeInfo(socket,out,in);
					replicaId = ( String ) in.readObject();
					out.writeObject(id);
					logger.info("Wrote leader id");
					//Write replica list to out
					out.writeObject(broker.getReplicasUnmodifiable());
					logger.info("sent list of replicas");
					broker.addReplica(replicaInfo,replicaId);
					out.flush();
					break;
				case SYNC:
					out.writeObject(dataSupplier.get());
					logger.info("wrote datas");
					out.writeObject(broker.getLocalClock());
					logger.info("wrote clock");
					break;
				case LEAVE:
					replicaId = ( String ) in.readObject();
					logger.info("Removing replica having id "+replicaId);
					broker.removeReplica(replicaId);
					break;
				case JOINING:
				case ACK:
				default:  //ACK should be catched in the methods expecting them
					logger.severe("Invalid command "+command);
					throw new ParsingException(command.toString());
			}
		}catch ( IOException | ClassNotFoundException e ){
			throw new BrokenProtocolException("Something went wrong", e);
		}
	}
}
