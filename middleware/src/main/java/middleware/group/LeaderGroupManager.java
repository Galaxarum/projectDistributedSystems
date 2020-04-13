package middleware.group;

import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import runnables.ServerSocketRunnable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class LeaderGroupManager<K, V> implements GroupManager<K, V> {

	private static final Logger logger = Logger.getLogger(LeaderGroupManager.class.getName());
	protected final String MY_ID;
	// OPT: this is needed only for the leader replica
	protected final Map<String, NodeInfo> replicas;

	public LeaderGroupManager(String id, int port, Map<String, NodeInfo> replicas) {
		try {
			this.MY_ID = id;
			this.replicas = replicas;
			logger.info("Starting socket listener");
			new Thread(new ServerSocketRunnable<GroupCommands>(port, (command, writer, reader, socket) -> {
				final String replicaId;
				final NodeInfo replicaInfo;
				switch ( command ) {
					case JOIN:
						//Register the replica
						replicaId = ( String ) reader.readObject();
						replicaInfo = new NodeInfo(socket.getInetAddress().getHostName(), socket.getPort());
						replicaInfo.setSocket(socket);
						writer.writeObject(MY_ID);
						//Write replica list to out
						writer.writeObject(replicas);
						replicas.put(replicaId, replicaInfo);
						break;
					case SYNC:
						//TODO: Send a copy of the local data
						writer.writeObject(new HashMap<>());
						//TODO: Send a copy of the local vector clock
						break;
					case LEAVE:
						replicaId = ( String ) reader.readObject();
						try {
							replicas.get(replicaId).getSocket().close();
						} catch ( IOException e ) {/*Ignored*/}
						replicas.remove(replicaId);
						break;
					case JOINING:
					case ACK:
					default:  //ACK should be catched in the methods expecting them
						throw new ParsingException(command.toString());
				}
			})).start();
			logger.info("Socket listener started");
		} catch ( IOException e ) {
			throw new BrokenProtocolException("Impossible to initialize the connection", e);
		}
	}


	/**
	 * @return null immediately
	 */
	@Override
	public Map<K, V> join() {
		return null;
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void leave() {
		throw new UnsupportedOperationException("The leader cannot leave the group");
	}
}
