package middleware.messages;

import middleware.group.NodeInfo;

import java.util.Map;

public interface MessageBroker<T> {
    void addConsumer(MessageConsumer<T> consumer);
    void receiveMessage(Message<T> msg);
    void broadCastMessage(Message<T> msg);
    void addReplica(NodeInfo replica, String id);
    void addAllReplicas(Map<String,NodeInfo> replicas);
    void removeReplica(String id);
    Map<String, NodeInfo> getReplicasUnmodifiable();
}
