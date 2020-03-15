package middleware.messages;

import exceptions.ParsingException;

public interface MessageConsumer<T> {
    void consumeMessage(Message<T> msg);
}
