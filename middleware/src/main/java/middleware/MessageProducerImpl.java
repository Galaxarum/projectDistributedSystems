package middleware;

import java.util.HashSet;

public class MessageProducerImpl implements MessageProducer{
    HashSet<MessageConsumer> consumers;
    HashSet<Message> buffer;

    @Override
    public void addConsumer(MessageConsumer consumer) {
        consumers.add(consumer);
    }

    @Override
    public void addMessageToBuffer(Message msg) {
        buffer.add(msg);
    }

    @Override
    public void shareMessage(Message msg) {
        //TODO
    }

    private void notifyConsumers(Message msg) {
        consumers.forEach(c->c.consumeMessage(msg));
    }
}
