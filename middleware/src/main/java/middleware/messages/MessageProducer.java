package middleware.messages;

public interface MessageProducer<T> {
    void addConsumer(MessageConsumer<T> consumer);
    void acceptMessage(Message<T> msg);
}
