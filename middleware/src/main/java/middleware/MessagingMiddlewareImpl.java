package middleware;

import functional_interfaces.Procedure;
import lombok.Getter;
import lombok.experimental.Delegate;
import markers.Primitive;
import middleware.group.*;
import middleware.messages.*;

import java.io.IOException;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;

public class MessagingMiddlewareImpl<Key, Value, ApplicationPrimitive extends Enum<ApplicationPrimitive> & Primitive> implements
        MessagingMiddleware<Key, Value, ApplicationPrimitive> {

    @Getter
    private final Map<Key,Value> data;
    private final VectorClock vectorClock;
    private final GroupManager<Key, Value> groupManager;
    @Delegate(types = {MessageBroker.class})
    private final MessageBrokerIImpl<?> messageBrokerImpl;


    public MessagingMiddlewareImpl(String id, int port, Socket leaderGroupSocket, Map<Key, Value> data) throws IOException {
        this.data = data;
        this.vectorClock = new VectorClock(id);
        this.groupManager = leaderGroupSocket==null?
                new LeaderGroupManager<>(id,port,this):
                new OrdinaryGroupManager<>(id,port,leaderGroupSocket,vectorClock,this);
        this.messageBrokerImpl = new MessageBrokerIImpl<>(id);
    }

    @Override
    public synchronized void leave() {
        groupManager.leave();
    }

    @Override
    public void shareOperation(ApplicationPrimitive command, Key key, Value value) {
        //TODO
    }

    @Override
    public synchronized void runCriticalOperation(Procedure procedure) {
        procedure.execute();
    }

}
