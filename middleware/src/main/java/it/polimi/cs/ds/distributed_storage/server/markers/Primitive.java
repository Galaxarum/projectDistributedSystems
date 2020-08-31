package it.polimi.cs.ds.distributed_storage.server.markers;

import it.polimi.cs.ds.distributed_storage.server.exceptions.BrokenProtocolException;

/**
 * Marker interface to mark classes encoding primitives (both at it.polimi.cs.ds.distributed_storage.server.middleware or application level)
 */
public interface Primitive {
    /**
     * Check if the given {@link Primitive} equals the given object, throwing {@link BrokenProtocolException} in case they differ
     * @param expected the expected {@link Primitive}
     * @param actual the object to check
     * @param <T> Must be an enum
     */
    static <T extends Enum<T> & Primitive> void checkEquals(T expected, Object actual){
        if(!expected.equals(actual))
            throw new BrokenProtocolException("Expected " + expected + " but " + actual + " was received.");
    }
}
