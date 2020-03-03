package distributedStorage.database;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.*;
import java.util.Hashtable;

public class DatabaseManager <K,V>{
    /**
     * The actual database. Using {@link Hashtable} grants a thread-safe behaviour.
     */
    @Getter
    private Hashtable<K,V> database;
    private final ObjectOutputStream fileOut;
    private static DatabaseManager<?,?> instance;
    private static Class<?> keyClass;
    private static Class<?> valueClass;

    @SuppressWarnings("unchecked")
    @SneakyThrows(ClassNotFoundException.class)
    private DatabaseManager(File file, Class<K> keyClass, Class<V> valueClass) throws IOException {

        fileOut = new ObjectOutputStream(new FileOutputStream(file));

        try(ObjectInputStream fin = new ObjectInputStream(new FileInputStream(file))){
            database = (Hashtable<K, V>) fin.readObject();
        } catch (IOException e) {
            database = new Hashtable<>();
        }

        DatabaseManager.instance = this;
        DatabaseManager.keyClass = keyClass;
        DatabaseManager.valueClass = valueClass;
    }

    public void persist() throws IOException {
        fileOut.writeObject(database);
    }

    @SuppressWarnings("unchecked")
    public static <K,V> DatabaseManager<K,V> getInstance(String persistencePath,Class<K> keyClass, Class<V> valueClass) throws IOException {
        if(instance == null){
            return new DatabaseManager<>(new File(persistencePath), keyClass, valueClass);
        }else if(!(DatabaseManager.keyClass.equals(keyClass) && DatabaseManager.valueClass.equals(valueClass)))
            throw new IllegalAccessError("Trying to change data type is forbidden");
        return (DatabaseManager<K, V>) instance;
    }

    @SneakyThrows
    public void close() {
        persist();
        fileOut.close();
    }
}