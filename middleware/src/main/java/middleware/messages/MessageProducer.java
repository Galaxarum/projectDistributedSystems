package middleware.messages;

public interface MessageProducer<T> {
    public void addConsumer(MessageConsumer<T> consumer);
    public void addMessageToBuffer(Message<T> msg);
    public void shareMessage(Message<T> msg);
}
