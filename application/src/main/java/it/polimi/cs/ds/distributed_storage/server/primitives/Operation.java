package it.polimi.cs.ds.distributed_storage.server.primitives;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data @AllArgsConstructor
public class Operation<K extends Serializable,V extends Serializable> implements Serializable {
	DataOperations primitive;
	K key;
	V value;
}
