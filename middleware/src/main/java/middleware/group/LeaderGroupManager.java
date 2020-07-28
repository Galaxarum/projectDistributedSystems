package middleware.group;

import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import middleware.messages.VectorClock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Logger;

class LeaderGroupManager<K, V> extends GroupManager<K, V> {
	private static VectorClock vectorClock;

	LeaderGroupManager(String id, int port, Map<String, NodeInfo> replicas,Map<K,V> data) {
		super(id, port, replicas, data);
		LeaderGroupManager.vectorClock = new VectorClock(id);
	}

	@Override
	public synchronized void join(VectorClock vectorClock) {
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
					replicaInfo = new NodeInfo(socket.getInetAddress().getHostName(), socket.getPort());
					try {
						replicaInfo.setSocket(socket,false);
						replicaInfo.setOut(out);
						replicaInfo.setIn(in);
					} catch ( IOException e ) {
						throw new BrokenProtocolException("Cannot connect with joining replica", e);
					}
					replicaId = ( String ) in.readObject();
					out.writeObject(id);
					//Write replica list to out
					out.writeObject(replicas);
					out.flush();
					replicas.put(replicaId, replicaInfo);
					break;
				case SYNC:
					out.writeObject(data);
					out.writeObject(vectorClock);
					break;
				case LEAVE:
					replicaId = ( String ) in.readObject();
					replicas.get(replicaId).close();
					replicas.remove(replicaId);
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
