package it.polimi.cs.ds.distributed_storage.server.database;

import lombok.Data;
import it.polimi.cs.ds.distributed_storage.server.primitives.DataOperations;

import java.io.Serializable;

@Data
public class DataContent<K extends Serializable, V extends Serializable> implements Serializable{
    K key;
    V value;
    DataOperations operations;
}
