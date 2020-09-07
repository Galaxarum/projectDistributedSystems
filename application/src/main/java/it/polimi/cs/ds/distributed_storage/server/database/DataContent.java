package it.polimi.cs.ds.distributed_storage.server.database;

import it.polimi.cs.ds.distributed_storage.DataOperations;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data @AllArgsConstructor @NoArgsConstructor
public class DataContent<K extends Serializable, V extends Serializable> implements Serializable{
    K key;
    V value;
    DataOperations operations;
}
