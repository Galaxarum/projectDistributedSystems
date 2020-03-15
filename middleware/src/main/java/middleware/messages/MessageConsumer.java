package middleware.messages;

import exceptions.ParsingException;

public interface MessageConsumer<T> {
    public void consumeMessage(Message<T> msg);
}
