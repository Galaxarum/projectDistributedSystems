package it.polimi.cs.ds.distributed_storage.middleware.messages;

import it.polimi.cs.ds.distributed_storage.functional_interfaces.Procedure;
import it.polimi.cs.ds.distributed_storage.middleware.group.NodeInfo;

import java.util.Map;

public interface MessageBroker<Content> {
    void addConsumer(MessageConsumer<Content> consumer);
    void receiveMessage(Message<Content> msg);
    void broadCastMessage(Content messageContent);
    void addReplica(NodeInfo replica, String id);
    void removeReplica(String id);
    Map<String, NodeInfo> getReplicasUnmodifiable();
    void init(VectorClock initialClock, Map<String,NodeInfo> replicas);
    void runBlocking(Procedure procedure);
    VectorClock getLocalClock();
}
