package interfaces;

/**
 * Operations must enforce causal consistency -> subclasses must implement vector clock
 * @param <K>
 * @param <V>
 */
public interface DataStoreInterface<K,V> {
    void put(K key, V value);
    V delete(K key);
    V get(K key);
}
