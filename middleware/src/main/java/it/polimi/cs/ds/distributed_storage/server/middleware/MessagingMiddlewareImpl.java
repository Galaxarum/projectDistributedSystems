package it.polimi.cs.ds.distributed_storage.server.middleware;

import it.polimi.cs.ds.distributed_storage.server.middleware.group.GroupManager;
import it.polimi.cs.ds.distributed_storage.server.middleware.group.LeaderGroupManager;
import it.polimi.cs.ds.distributed_storage.server.middleware.group.OrdinaryGroupManager;
import it.polimi.cs.ds.distributed_storage.server.middleware.messages.MessageBrokerImpl;
import it.polimi.cs.ds.distributed_storage.server.middleware.messages.MessageConsumer;

import java.net.Socket;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MessagingMiddlewareImpl<Key, Value, Content> implements MessagingMiddleware<Key, Value, Content> {

    private final GroupManager<Key, Value> groupManager;

    private final MessageBrokerImpl<Content> messageBroker;

    public MessagingMiddlewareImpl(String id, int port, Supplier<Map<Key,Value>> dataSupplier){
        this.messageBroker = new MessageBrokerImpl<>(id);
        this.groupManager = new LeaderGroupManager<>(id,port,messageBroker,dataSupplier);
    }

    public MessagingMiddlewareImpl(String id, int port, Socket leaderSocket, Consumer<Map<Key,Value>> dataInitializer) {
        this.messageBroker = new MessageBrokerImpl<>(id);
        this.groupManager = new OrdinaryGroupManager<>(id,port,leaderSocket, messageBroker,dataInitializer);

    }

    @Override
    public void sendMessage(Content content) {
        messageBroker.broadCastMessage(content);
    }

    @Override
    public void addConsumer(MessageConsumer<Content> consumer) {
        messageBroker.addConsumer(consumer);
    }

    @Override
    public synchronized void leave() {
        groupManager.leave();
    }

}
