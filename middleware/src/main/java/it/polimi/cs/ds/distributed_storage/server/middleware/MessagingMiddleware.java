package it.polimi.cs.ds.distributed_storage.server.middleware;

import it.polimi.cs.ds.distributed_storage.server.middleware.messages.MessageConsumer;

public interface MessagingMiddleware <Key, Value,T> {
    int DEFAULT_STARTING_PORT = 12345;
    int NEEDED_PORTS = 2;

    void leave();

    void sendMessage(T content);

    void addConsumer(MessageConsumer<T> consumer);
}
