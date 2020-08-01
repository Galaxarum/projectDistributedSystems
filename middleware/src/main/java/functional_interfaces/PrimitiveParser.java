package functional_interfaces;

import exceptions.ParsingException;
import markers.Primitive;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@FunctionalInterface
public interface PrimitiveParser<P extends Primitive> {
    void parse(final P primitive, final ObjectOutputStream out, final ObjectInputStream in, final Socket socket) throws ParsingException;
}
