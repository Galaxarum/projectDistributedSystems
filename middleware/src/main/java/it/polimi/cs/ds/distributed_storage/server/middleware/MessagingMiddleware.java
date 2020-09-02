package it.polimi.cs.ds.distributed_storage.server.middleware;

import it.polimi.cs.ds.distributed_storage.server.middleware.messages.MessageConsumer;

import java.io.Serializable;

public interface MessagingMiddleware <Key extends Serializable, Value extends Serializable,T extends Serializable> {
    int DEFAULT_STARTING_PORT = 12345;
    int NEEDED_PORTS = 2;

    void leave();

    void sendMessage(T content);

    void addConsumer(MessageConsumer<T> consumer);
}
