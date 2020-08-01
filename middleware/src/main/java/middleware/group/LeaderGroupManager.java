package middleware.group;

import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import middleware.MessagingMiddleware;
import middleware.messages.VectorClock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

public class LeaderGroupManager<K, V> extends GroupManager<K, V> {
	private static VectorClock vectorClock;

	public LeaderGroupManager(String id, int port, MessagingMiddleware<K, V, ?> owner) {
		super(id, port, owner);
		LeaderGroupManager.vectorClock = new VectorClock(id);
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
					out.writeObject(owner.getReplicasUnmodifiable());
					out.flush();
					owner.addReplica(replicaInfo,replicaId);
					break;
				case SYNC:
					out.writeObject(owner.getData());
					out.writeObject(vectorClock);
					break;
				case LEAVE:
					replicaId = ( String ) in.readObject();
					owner.removeReplica(replicaId);
					break;
				case JOINING:
				case ACK:
				default:  //ACK should be catched in the methods expecting them
					throw new ParsingException(command.toString());
			}
		}catch ( IOException | ClassNotFoundException e ){
			throw new BrokenProtocolException("Comething went wrong", e);
		}
	}
}
