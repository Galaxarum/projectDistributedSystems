package templates;

import middleware.primitives.Primitive;

@FunctionalInterface
public interface ParsingFunction<P extends Primitive> {
    void parse(P primitive, NetworkWriter writerFunction, NetworkReader readerFunction);
}
