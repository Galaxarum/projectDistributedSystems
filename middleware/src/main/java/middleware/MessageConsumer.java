package middleware;

public interface MessageConsumer<T> {
    public void consumeMessage(Message<T> msg);
}
