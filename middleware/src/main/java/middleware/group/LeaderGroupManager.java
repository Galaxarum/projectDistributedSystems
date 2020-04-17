package middleware.group;

import exceptions.BrokenProtocolException;
import exceptions.ParsingException;
import functional_interfaces.ParsingFunction;
import lombok.Getter;
import middleware.messages.VectorClocks;
import runnables.ServerSocketRunnable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

class LeaderGroupManager<K, V> implements GroupManager<K, V> {

	private static final Logger logger = Logger.getLogger(LeaderGroupManager.class.getName());
	private static String MY_ID;
	private static Map<String, NodeInfo> replicas;
	@Getter
	private static VectorClocks vectorClocks;
	private static final ParsingFunction<GroupCommands> parser = (command, writer, reader, socket) -> {
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
				writer.writeObject(vectorClocks);
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
	};

	LeaderGroupManager(String id, int port, Map<String, NodeInfo> replicas) {
		try {
			LeaderGroupManager.MY_ID =id;
			LeaderGroupManager.replicas = replicas;
			LeaderGroupManager.vectorClocks = new VectorClocks(id);
			logger.info("Starting socket listener");
			new Thread(new ServerSocketRunnable<>(port, parser)).start();
			logger.info("Socket listener started");
		} catch ( IOException e ) {
			throw new BrokenProtocolException("Impossible to initialize the connection", e);
		}
	}


	/**
	 */
	@Override
	public void join(Map<K,V> data, VectorClocks vectorClocks) {
	}

	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void leave() {
		throw new UnsupportedOperationException("The leader cannot leave the group");
	}
}
