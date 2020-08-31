package it.polimi.cs.ds.distributed_storage.middleware.group;

import it.polimi.cs.ds.distributed_storage.exceptions.BrokenProtocolException;
import it.polimi.cs.ds.distributed_storage.exceptions.ParsingException;
import it.polimi.cs.ds.distributed_storage.middleware.messages.MessageBroker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.function.Supplier;

public class LeaderGroupManager<K, V> extends GroupManager<K, V> {
	private final Supplier<Map<K, V>> dataSupplier;

	public LeaderGroupManager(String id, int port, MessageBroker<?> broker,Supplier<Map<K,V>> dataSupplier) {
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
			switch ( command ) {
				case JOIN:
					//Register the replica
					replicaInfo = new NodeInfo(socket,out,in);
					replicaId = ( String ) in.readObject();
					out.writeObject(id);
					//Write replica list to out
					out.writeObject(broker.getReplicasUnmodifiable());
					out.flush();
					broker.addReplica(replicaInfo,replicaId);
					break;
				case SYNC:
					out.writeObject(dataSupplier.get());
					out.writeObject(broker.getLocalClock());
					break;
				case LEAVE:
					replicaId = ( String ) in.readObject();
					broker.removeReplica(replicaId);
					break;
				case JOINING:
				case ACK:
				default:  //ACK should be catched in the methods expecting them
					throw new ParsingException(command.toString());
			}
		}catch ( IOException | ClassNotFoundException e ){
			throw new BrokenProtocolException("Something went wrong", e);
		}
	}
}
