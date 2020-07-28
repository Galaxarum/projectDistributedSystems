package middleware.group;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

public class GroupManagerFactory {
	private static GroupManager instance;
	public static synchronized <K,V> GroupManager<K,V> factory(String id,
	                                              int port,
	                                              Socket leaderGroupSocket,
	                                              Map<String, NodeInfo> replicas,
	                                              Map<K,V> data) throws IOException {
		if(instance == null)
			instance = leaderGroupSocket == null ?
					new LeaderGroupManager<>(id, port, replicas,data) :
					new OrdinaryGroupManager<>(id, port, leaderGroupSocket, replicas,data);
		return instance;
	}
}
