package middleware.messages;

public interface MessageProducer<T> {
    void addConsumer(MessageConsumer<T> consumer);
    void addMessageToBuffer(Message<T> msg);
    void shareMessage(Message<T> msg);
}
