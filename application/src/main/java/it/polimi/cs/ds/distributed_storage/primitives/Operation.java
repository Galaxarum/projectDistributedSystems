package it.polimi.cs.ds.distributed_storage.primitives;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class Operation<K,V> {
	DataOperations primitive;
	K key;
	V value;
}
