package middleware.messages;

import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@RequiredArgsConstructor
public class MessageProducerImpl<T> implements MessageProducer<T> {
    final HashSet<MessageConsumer<T>> consumers = new HashSet<>();
    final SortedSet<Message<T>> buffer = new TreeSet<>();
    final VectorClock localClock;

    public MessageProducerImpl(String localId){
        localClock = new VectorClock(localId);
    }

    @Override
    public final void addConsumer(MessageConsumer<T> consumer) {
        consumers.add(consumer);
    }

    @Override
    public final synchronized void shareMessage(Message<T> msg) {
        buffer.add(msg);
        flushBuffer();
    }

    /**
     * Iterate the buffer (that is sorted by default)
     * If a  message can be accepted, use it to update the current clock and deliver it
     */
    private void flushBuffer(){
        Set<Message<T>> toRemove = new HashSet<>();
        //TODO: check special cases when message ordering by timestamp as used may be not enough
        for ( Message<T> msg: buffer )
            if( localClock.canAccept(msg.getTimestamp())){
                localClock.update(msg.getTimestamp());
                consumers.forEach(c->c.consumeMessage(msg));
                toRemove.add(msg);
            }
        buffer.removeAll(toRemove);
    }


}
