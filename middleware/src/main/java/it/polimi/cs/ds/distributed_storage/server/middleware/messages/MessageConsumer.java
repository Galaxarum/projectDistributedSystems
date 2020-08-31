package it.polimi.cs.ds.distributed_storage.server.middleware.messages;

public interface MessageConsumer<T> {
    void consumeMessage(Message<T> msg);
}
