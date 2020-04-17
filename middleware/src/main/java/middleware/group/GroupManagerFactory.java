package middleware.group;

import java.util.Map;

public class GroupManagerFactory {
	private static GroupManager instance;
	public static <K,V> GroupManager<K,V> factory(String id,
	                                              int port,
	                                              String leaderHost,
	                                              Map<String, NodeInfo> replicas,
	                                              Map<K,V> data) {
		if(instance == null)
			instance = leaderHost == null ?
					new LeaderGroupManager<>(id, port, replicas,data) :
					new OrdinaryGroupManager<>(id, port, leaderHost, replicas,data);
		return instance;
	}
}
