package distributedStorage.database;

import lombok.SneakyThrows;

import java.io.*;
import java.net.URI;
import java.util.HashMap;

public class DatabaseManager <K,V>{

    private HashMap<K,V> database;
    private final ObjectOutputStream fileOut;
    private static DatabaseManager<?,?> instance;
    private static Class<?> keyClass;
    private static Class<?> valueClass;

    @SuppressWarnings("unchecked")
    @SneakyThrows(ClassNotFoundException.class)
    private DatabaseManager(File file, Class<K> keyClass, Class<V> valueClass) throws IOException {

        fileOut = new ObjectOutputStream(new FileOutputStream(file));

        try(ObjectInputStream fin = new ObjectInputStream(new FileInputStream(file))){
            database = (HashMap<K, V>) fin.readObject();
        } catch (IOException e) {
            database = new HashMap<>();
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

    @SuppressWarnings("unchecked")
    public static <K,V> DatabaseManager<K,V> getInstance(URI persistenceURI,Class<K> keyClass, Class<V> valueClass) throws IOException {
        if(instance == null){
            return new DatabaseManager<K, V>(new File(persistenceURI),keyClass,valueClass);
        }else if(!(DatabaseManager.keyClass.equals(keyClass) && DatabaseManager.valueClass.equals(valueClass)))
            throw new IllegalAccessError("Trying to change data type is forbidden");
        return (DatabaseManager<K, V>) instance;
    }

    public synchronized V put(K key, V value){
        return database.put(key,value);
    }

    public synchronized V get(K key){
        return database.get(key);
    }

    public synchronized V delete(K key){
        return database.remove(key);
    }

    public synchronized boolean delete(K key, V value){
        return database.remove(key,value);
    }


}