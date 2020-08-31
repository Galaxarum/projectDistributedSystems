package it.polimi.cs.distributed_storage.database;

import lombok.Data;
import it.polimi.cs.distributed_storage.primitives.DataOperations;

@Data
public class DataContent<K, V> {
    K key;
    V value;
    DataOperations operations;
}
