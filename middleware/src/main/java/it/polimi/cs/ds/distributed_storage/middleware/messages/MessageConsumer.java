package it.polimi.cs.ds.distributed_storage.middleware.messages;

public interface MessageConsumer<T> {
    void consumeMessage(Message<T> msg);
}
