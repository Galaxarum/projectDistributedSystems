package it.polimi.cs.distributed_storage.primitives;

import it.polimi.cs.ds.distributed_storage.markers.Primitive;

/**
 * This enum contains the applicative primitives
 */
public enum DataOperations implements Primitive {
    PUT,
    GET,
    DELETE
}
