package middleware.group;

import java.util.Map;

public class GroupManagerFactory {
	private static GroupManager instance;
	public static <K,V> GroupManager<K,V> factory(String id, int port, String leaderHost, Map<String, NodeInfo> replicas) {
		if(instance == null)
			instance = leaderHost == null ?
					new LeaderGroupManager<>(id, port, replicas) :
					new OrdinaryGroupManager<>(id, port, leaderHost, replicas);
		return instance;
	}
}
