package functional_interfaces;

import exceptions.ParsingException;
import middleware.primitives.Primitive;

@FunctionalInterface
public interface ParsingFunction<P extends Primitive> {
    void parse(P primitive, NetworkWriter writer, NetworkReader reader) throws ParsingException;
}
