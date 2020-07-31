package middleware;

import functional_interfaces.Procedure;
import markers.Primitive;

public interface MessagingMiddleware <Key, Value, ApplicativePrimitive extends Enum<ApplicativePrimitive> & Primitive>{
    int DEFAULT_STARTING_PORT = 12345;
    int NEEDED_PORTS = 2;

    void leave();

    void shareOperation(ApplicativePrimitive command, Key key, Value value);

    void runCriticalOperation(Procedure procedure);
}
