package it.polimi.cs.ds.distributed_storage.server.database;

import lombok.Data;
import it.polimi.cs.ds.distributed_storage.server.primitives.DataOperations;

@Data
public class DataContent<K, V> {
    K key;
    V value;
    DataOperations operations;
}
