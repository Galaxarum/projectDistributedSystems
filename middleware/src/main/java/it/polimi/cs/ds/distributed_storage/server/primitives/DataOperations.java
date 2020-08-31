package it.polimi.cs.ds.distributed_storage.server.primitives;

import it.polimi.cs.ds.distributed_storage.server.markers.Primitive;

/**
 * This enum contains the applicative primitives
 */
public enum DataOperations implements Primitive {
    PUT,
    GET,
    DELETE
}
