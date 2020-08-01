package middleware;

import functional_interfaces.Procedure;
import markers.Primitive;
import middleware.group.NodeInfo;
import middleware.messages.MessageBroker;

import java.util.Map;

public interface MessagingMiddleware <Key, Value, ApplicativePrimitive extends Enum<ApplicativePrimitive> & Primitive>
        extends MessageBroker<ApplicativePrimitive> {
    int DEFAULT_STARTING_PORT = 12345;
    int NEEDED_PORTS = 2;

    void leave();

    void shareOperation(ApplicativePrimitive command, Key key, Value value);

    void runCriticalOperation(Procedure procedure);

    Map<Key,Value> getData();
}
