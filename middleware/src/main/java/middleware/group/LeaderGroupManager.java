package middleware.group;

import exceptions.ParsingException;
import functional_interfaces.NetworkReader;
import functional_interfaces.NetworkWriter;
import middleware.messages.VectorClocks;

import java.io.IOException;
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
	public void parse(GroupCommands command, NetworkWriter writer, NetworkReader reader, Socket socket) throws ParsingException {
		final String replicaId;
		final NodeInfo replicaInfo;
		switch ( command ) {
			case JOIN:
				//Register the replica
				replicaId = ( String ) reader.readObject();
				replicaInfo = new NodeInfo(socket.getInetAddress().getHostName(), socket.getPort());
				replicaInfo.setSocket(socket);
				writer.writeObject(id);
				//Write replica list to out
				writer.writeObject(replicas);
				replicas.put(replicaId, replicaInfo);
				break;
			case SYNC:
				writer.writeObject(data);
				writer.writeObject(vectorClocks);
				break;
			case LEAVE:
				replicaId = ( String ) reader.readObject();
				try {
					replicas.get(replicaId).getSocket().close();
				} catch ( IOException e ) {/*Ignored: the connection is already closed*/} finally {
					replicas.remove(replicaId);
				}
				break;
			case JOINING:
			case ACK:
			default:  //ACK should be catched in the methods expecting them
				throw new ParsingException(command.toString());
		}
	}
}
