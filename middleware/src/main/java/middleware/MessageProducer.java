package middleware;

public interface MessageProducer {
    public void addConsumer(MessageConsumer consumer);
    public void addMessageToBuffer(Message msg);
    public void shareMessage(Message msg);
}
