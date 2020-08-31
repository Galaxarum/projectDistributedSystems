package it.polimi.cs.ds.distributed_storage.functional_interfaces;

import it.polimi.cs.ds.distributed_storage.exceptions.ParsingException;
import it.polimi.cs.ds.distributed_storage.markers.Primitive;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@FunctionalInterface
public interface PrimitiveParser<P extends Primitive> {
    void parse(final P primitive, final ObjectOutputStream out, final ObjectInputStream in, final Socket socket) throws ParsingException;
}
