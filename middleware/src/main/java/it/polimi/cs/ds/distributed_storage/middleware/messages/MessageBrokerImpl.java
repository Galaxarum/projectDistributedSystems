package it.polimi.cs.ds.distributed_storage.middleware.messages;

import it.polimi.cs.ds.distributed_storage.functional_interfaces.Procedure;
import lombok.Getter;
import it.polimi.cs.ds.distributed_storage.middleware.group.NodeInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

public final class MessageBrokerImpl<T> implements MessageBroker<T> {
    final HashSet<MessageConsumer<T>> consumers = new HashSet<>();
    private final SortedSet<Message<T>> buffer = new TreeSet<>();
    @Getter
    private final VectorClock localClock;
    private final Map<String, NodeInfo> receivers = new Hashtable<>();
    private final ThreadGroup listenersGroup = new ThreadGroup(this.getClass().getSimpleName());

    public MessageBrokerImpl(String localId){
        localClock = new VectorClock(localId);
    }

    @Override
    public synchronized void addConsumer(MessageConsumer<T> consumer) {
        consumers.add(consumer);
    }

    @Override
    public final synchronized void receiveMessage(Message<T> msg) {
        buffer.add(msg);
        flushBuffer();
    }

    /**
     * Iterate the buffer (that is sorted by default)
     * If a  message can be accepted, use it to update the current clock and deliver it
     */
    private void flushBuffer(){
        Set<Message<T>> toRemove = new HashSet<>();
        for ( Message<T> msg: buffer )
            if( localClock.canAccept(msg.getTimestamp())){
                localClock.update(msg.getTimestamp());
                consumers.forEach(c->c.consumeMessage(msg));
                toRemove.add(msg);
            }
        buffer.removeAll(toRemove);
    }

    @Override
    public synchronized void broadCastMessage(T content) {
        receivers.forEach((id,node)->{
            try {
                localClock.incrementLocal();
                Message<T> message = new Message<>(content,localClock);
                node.getMessageOut().writeObject(message);
            } catch ( IOException e ) {
                removeReplica(id);
            }
        });
    }

    @Override
    public synchronized void addReplica(NodeInfo replica,String id) {
        startMessageListener(id,replica);
        receivers.put(id,replica);
    }

    @Override
    public synchronized void removeReplica(String id) {
        receivers.remove(id).close();
    }

    @Override
    public synchronized Map<String, NodeInfo> getReplicasUnmodifiable() {
        return Collections.unmodifiableMap(receivers);
    }

    @Override
    public synchronized void init(VectorClock initialClock, Map<String, NodeInfo> replicas) {
        localClock.update(initialClock);
        replicas.forEach(this::startMessageListener);
        receivers.putAll(replicas);
    }

    @Override
    public synchronized void runBlocking(Procedure procedure) {
        procedure.execute();
    }

    private void startMessageListener(final String id,final NodeInfo replica){
        new Thread(listenersGroup,()->{
            final ObjectInputStream rin = replica.getMessageIn();
            while ( !replica.isClosed() ){
                try {
                    Message<T> msg = ( Message<T> ) rin.readObject();
                    receiveMessage(msg);
                } catch ( IOException | ClassNotFoundException e ) {
                    removeReplica(id);
                    break;
                }
            }
        }).start();
    }

}
