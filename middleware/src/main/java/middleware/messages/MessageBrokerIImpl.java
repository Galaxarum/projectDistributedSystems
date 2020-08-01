package middleware.messages;

import lombok.RequiredArgsConstructor;
import middleware.group.NodeInfo;

import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
public class MessageBrokerIImpl<T> implements MessageBroker<T> {
    final HashSet<MessageConsumer<T>> consumers = new HashSet<>();
    final SortedSet<Message<T>> buffer = new TreeSet<>();
    final VectorClock localClock;
    //TODO: new replicas?
    final Map<String, NodeInfo> receivers = new Hashtable<>();
    final ThreadGroup listenersGroup = new ThreadGroup(this.getClass().getSimpleName());

    public MessageBrokerIImpl(String localId){
        localClock = new VectorClock(localId);
        receivers.forEach(this::startListeningReceiver);
    }

    public void startListeningReceiver(String id,NodeInfo node){
        new Thread(listenersGroup,()->{
           while ( !node.getGroupSocket().isClosed() ){
               try {
                   Message<T> msg = ( Message<T> ) node.getGroupIn().readObject();
                   receiveMessage(msg);
               } catch ( IOException | ClassNotFoundException e ) {
                   node.close();
                   receivers.remove(id);
               }
           }
        }).start();
    }

    @Override
    public final void addConsumer(MessageConsumer<T> consumer) {
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
    public void broadCastMessage(Message<T> msg) {
        receivers.forEach((id,node)->{
            try {
                node.getMessageOut().writeObject(msg);
            } catch ( IOException e ) {
                node.close();
                receivers.remove(id);
            }
        });
    }

    @Override
    public void addReplica(NodeInfo replica,String id) {
        receivers.put(id,replica);
    }

    @Override
    public void addAllReplicas(Map<String, NodeInfo> replicas) {
        receivers.putAll(replicas);
    }

    @Override
    public void removeReplica(String id) {
        receivers.remove(id).close();
    }

    @Override
    public Map<String, NodeInfo> getReplicasUnmodifiable() {
        return Collections.unmodifiableMap(receivers);
    }

    //URG: implement a new runnable to receive messages

}
