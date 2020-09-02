package it.polimi.cs.ds.distributed_storage.server.middleware.messages;

import it.polimi.cs.ds.distributed_storage.server.middleware.group.NodeInfo;

import java.io.Serializable;
import java.util.Map;

public interface MessageBroker<Content extends Serializable> {
    void addConsumer(MessageConsumer<Content> consumer);
    void receiveMessage(Message<Content> msg);
    void broadCastMessage(Content messageContent);
    void addReplica(NodeInfo replica, String id);
    void removeReplica(String id);
    Map<String, NodeInfo> getReplicasUnmodifiable();
    void initReplicas(Map<String, NodeInfo> replicas);
    void runBlocking(Runnable procedure);
    VectorClock getLocalClock();
    void initClock(VectorClock initialClock);
}
