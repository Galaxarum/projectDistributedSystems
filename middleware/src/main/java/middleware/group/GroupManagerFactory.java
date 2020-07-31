package middleware.group;

import middleware.MessagingMiddleware;
import middleware.messages.VectorClock;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

public class GroupManagerFactory {
	private static GroupManager instance;
	public static synchronized <K,V> GroupManager<K,V> factory(String id,
	                                                           int port,
	                                                           Socket leaderGroupSocket,
	                                                           Map<String, NodeInfo> replicas,
	                                                           Map<K,V> data,
	                                                           VectorClock initialClock,
	                                                           MessagingMiddleware<K,V,?> owner) throws IOException {
		if(instance == null)
			instance = leaderGroupSocket == null ?
					new LeaderGroupManager<>(id, port, replicas,data) :
					new OrdinaryGroupManager<>(id, port, leaderGroupSocket, replicas,data, initialClock,owner);
		return instance;
	}
}
