package it.polimi.cs.ds.distributed_storage.server.middleware;

import it.polimi.cs.ds.distributed_storage.server.middleware.group.GroupManager;
import it.polimi.cs.ds.distributed_storage.server.middleware.group.LeaderGroupManager;
import it.polimi.cs.ds.distributed_storage.server.middleware.group.OrdinaryGroupManager;
import it.polimi.cs.ds.distributed_storage.server.middleware.messages.MessageBrokerImpl;
import it.polimi.cs.ds.distributed_storage.server.middleware.messages.MessageConsumer;

import java.io.Serializable;
import java.net.Socket;
import java.util.Hashtable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class MessagingMiddlewareImpl<K extends Serializable, V extends Serializable, C extends Serializable> implements MessagingMiddleware<K, V, C> {

    public static Logger logger = Logger.getLogger(MessagingMiddlewareImpl.class.getName());
	private final GroupManager groupManager;
	public final int port;

    private final MessageBrokerImpl<C> messageBroker;

    public <Key extends Serializable, Value extends Serializable> MessagingMiddlewareImpl(String id, int port, Supplier<Hashtable<Key, Value>> dataSupplier){
        this.messageBroker = new MessageBrokerImpl<>(id);
        this.groupManager = new LeaderGroupManager<>(id,port,messageBroker,dataSupplier);
        this.port=port;
    }

    public <Key extends Serializable, Value extends Serializable> MessagingMiddlewareImpl(String id, int port, Socket leaderSocket, Consumer<Hashtable<Key, Value>> dataInitializer) {
        this.messageBroker = new MessageBrokerImpl<>(id);
        this.groupManager = new OrdinaryGroupManager<>(id,port,leaderSocket, messageBroker, dataInitializer);
        this.port=port;

    }

    @Override
    public void sendMessage(C content) {
        messageBroker.broadCastMessage(content);
    }

    @Override
    public void addConsumer(MessageConsumer<C> consumer) {
        messageBroker.addConsumer(consumer);
    }

    @Override
    public synchronized void leave() {
        groupManager.leave();
    }

}
