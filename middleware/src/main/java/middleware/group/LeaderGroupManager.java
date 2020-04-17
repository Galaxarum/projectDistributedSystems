package middleware.group;

import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import middleware.messages.VectorClocks;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Logger;

class LeaderGroupManager<K, V> extends GroupManager<K, V> {
	private static final Logger logger = Logger.getLogger(LeaderGroupManager.class.getName());
	private static VectorClocks vectorClocks;

	LeaderGroupManager(String id, int port, Map<String, NodeInfo> replicas,Map<K,V> data) {
		super(id, port, replicas, data);
		LeaderGroupManager.vectorClocks = new VectorClocks(id);
	}

	@Override
	public void join(VectorClocks vectorClocks) {
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void leave() {
		throw new UnsupportedOperationException("The leader cannot leave the group");
	}

	@Override
	public void parse(GroupCommands command, ObjectOutputStream out, ObjectInputStream in, Socket socket) throws ParsingException {
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
					replicas.put(replicaId, replicaInfo);
					break;
				case SYNC:
					out.writeObject(data);
					out.writeObject(vectorClocks);
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
			//TODO
			throw new BrokenProtocolException("", e);
		}
	}
}
