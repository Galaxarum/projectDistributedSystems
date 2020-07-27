package middleware.messages;

import java.util.HashSet;

public abstract class MessageProducerImpl<T> implements MessageProducer<T> {
    HashSet<MessageConsumer<T>> consumers;

    @Override
    public void addConsumer(MessageConsumer<T> consumer) {
        consumers.add(consumer);
    }

    @Override
    public synchronized void shareMessage(Message<T> msg) {
        consumers.forEach(c->c.consumeMessage(msg));
    }

}
