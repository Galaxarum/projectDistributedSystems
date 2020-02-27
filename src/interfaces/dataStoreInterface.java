package interfaces;

/**
 * Operations must enforce causal consistency
 * @param <K>
 * @param <V>
 */
public interface dataStoreInterface <K,V> {
    void put(K key, V value);
    V delete(K key);
    V get(K key);
}
