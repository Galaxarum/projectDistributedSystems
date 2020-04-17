package functional_interfaces;

import exceptions.ParsingException;
import markers.Primitive;

import java.net.Socket;

@FunctionalInterface
public interface PrimitiveParser<P extends Primitive> {
    void parse(P primitive, NetworkWriter writer, NetworkReader reader, Socket socket) throws ParsingException;
}
