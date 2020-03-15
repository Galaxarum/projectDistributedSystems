package middleware.messages;

import exceptions.BrokenProtocolException;
import exceptions.ParsingException;

import java.util.HashSet;

public class MessageProducerImpl<T> implements MessageProducer<T>{
    HashSet<MessageConsumer<T>> consumers;
    HashSet<Message<T>> buffer;

    @Override
    public void addConsumer(MessageConsumer<T> consumer) {
        consumers.add(consumer);
    }

    @Override
    public void addMessageToBuffer(Message<T> msg) {
        buffer.add(msg);
    }

    @Override
    public void shareMessage(Message<T> msg) {
        //TODO
    }

    private void notifyConsumers(Message<T> msg) {
        consumers.forEach(c-> {
            try {
                c.consumeMessage(msg);
            } catch (ParsingException e) {
                throw new BrokenProtocolException("Parsing failed",e);
            }
        });
    }
}
