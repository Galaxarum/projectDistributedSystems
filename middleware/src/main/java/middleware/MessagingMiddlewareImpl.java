package middleware;

import functional_interfaces.Procedure;
import markers.Primitive;
import middleware.group.GroupManager;
import middleware.group.GroupManagerFactory;
import middleware.group.NodeInfo;
import middleware.messages.VectorClock;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class MessagingMiddlewareImpl<Key, Value, ApplicationPrimitive extends Enum<ApplicationPrimitive> & Primitive> implements
        MessagingMiddleware<Key, Value, ApplicationPrimitive> {

    private static final int GROUP_PORT_OFFSET = 0;
    private final Map<String, NodeInfo> replicas = new Hashtable<>();
    private final Map<Key,Value> data;
    private final VectorClock vectorClock;
    private final GroupManager<Key, Value> groupManager;


    public MessagingMiddlewareImpl(String id, int port, Socket leaderGroupSocket, Map<Key, Value> data) throws IOException {
        this.data = data;
        this.vectorClock = new VectorClock(id);
        this.groupManager = GroupManagerFactory.factory(id,port+GROUP_PORT_OFFSET,leaderGroupSocket,replicas,data,vectorClock,this);
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
    public void runCriticalOperation(Procedure procedure) {
        procedure.execute();
    }
}
