package it.polimi.cs.ds.distributed_storage.server.middleware.messages;

import java.io.Serializable;

public interface MessageConsumer<T extends Serializable> {
    void consumeMessage(Message<T> msg);
}
