package middleware.messages;

public interface MessageConsumer<T> {
    void consumeMessage(Message<T> msg);
}
