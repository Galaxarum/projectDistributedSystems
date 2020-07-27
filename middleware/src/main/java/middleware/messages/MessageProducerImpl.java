package middleware.messages;

import java.util.HashSet;
import java.util.Set;

public abstract class MessageProducerImpl<T> implements MessageProducer<T> {
    HashSet<MessageConsumer<T>> consumers = new HashSet<>();
    Set<Message<T>> buffer = new HashSet<>();
    VectorClock currentClock;

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
        for ( Message<T> msg: buffer )
            if(currentClock.canAccept(msg.getTimestamp())){
                currentClock.update(msg.getTimestamp());
                consumers.forEach(c->c.consumeMessage(msg));
            }
    }


}
