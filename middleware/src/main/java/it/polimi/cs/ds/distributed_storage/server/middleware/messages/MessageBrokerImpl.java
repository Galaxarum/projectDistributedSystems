package it.polimi.cs.ds.distributed_storage.server.middleware.messages;

import it.polimi.cs.ds.distributed_storage.server.middleware.group.NodeInfo;
import lombok.Getter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

public final class MessageBrokerImpl<T extends Serializable> implements MessageBroker<T> {
    final HashSet<MessageConsumer<T>> consumers = new HashSet<>();
    private final SortedSet<Message<T>> buffer = new TreeSet<>();
    @Getter
    private final VectorClock localClock;
    private final Map<String, NodeInfo> receivers = new Hashtable<>();
    public static final Logger logger = Logger.getLogger(MessageBroker.class.getName());

    public MessageBrokerImpl(String localId){
        localClock = new VectorClock(localId);
    }

    @Override
    public synchronized void addConsumer(MessageConsumer<T> consumer) {
        consumers.add(consumer);
        logger.info("Added consumer. New #consumer is "+consumers.size());
    }

    @Override
    public final synchronized void receiveMessage(Message<T> msg) {
        buffer.add(msg);
        flushBuffer();
        logger.info("Received message "+msg);
    }

    /**
     * Iterate the buffer (that is sorted by default)
     * If a  message can be accepted, use it to update the current clock and deliver it
     */
    private void flushBuffer(){
        final Set<Message<T>> toRemove = new HashSet<>();
        for ( Message<T> msg: buffer )
            if( localClock.canAccept(msg.getTimestamp())){
                localClock.update(msg.getTimestamp());
                consumers.forEach(c->c.consumeMessage(msg));
                toRemove.add(msg);
                logger.info("delivered message "+msg);
            }
        buffer.removeAll(toRemove);
    }

    @Override
    public synchronized void broadCastMessage(T content) {
        localClock.incrementLocal();
        final Set<String> toRemove = new HashSet<>();
        final Message<T> message = new Message<>(content,localClock);
        logger.info("Sending message "+message);
        receivers.forEach((id,node)->{
            try {
                node.getMessageOut().writeObject(message);
            } catch ( IOException e ) {
                e.printStackTrace();
                toRemove.add(id);
            }
        });
        toRemove.forEach(this::removeReplica);
    }

    @Override
    public synchronized void addReplica(NodeInfo replica,String id) {
        receivers.put(id,replica);
        startMessageListener(id,replica);
        logger.info("Saved replica "+id+" on "+replica.getHostname());
    }

    @Override
    public synchronized void removeReplica(String id) {
        if(receivers.containsKey(id)) {
            receivers.remove(id).close();
            logger.info("removed replica " + id);
        }
    }

    @Override
    public synchronized Map<String, NodeInfo> getReplicasUnmodifiable() {
        return Collections.unmodifiableMap(receivers);
    }

    @Override
    public synchronized void initClock(VectorClock initialClock) {
        localClock.update(initialClock);
    }

    @Override
    public synchronized void initReplicas(Map<String,NodeInfo> replicas){
        replicas.forEach(this::startMessageListener);
        receivers.putAll(replicas);
    }

    @Override
    public synchronized void runBlocking(Runnable procedure) {
        procedure.run();
    }

    private void startMessageListener(final String id,final NodeInfo replica){
        new Thread(()-> replica.executeOnFullConnection(()->{
            logger.info("Starting to listen to replica "+id+" at "+replica.getHostname());
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
        })).start();
    }

}
