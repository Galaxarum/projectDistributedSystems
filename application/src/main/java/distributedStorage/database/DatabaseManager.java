package distributedStorage.database;

import exceptions.BrokenProtocolException;
import lombok.Getter;
import lombok.SneakyThrows;
import middleware.messages.Message;
import middleware.messages.MessageConsumer;

import java.io.*;
import java.nio.file.FileSystemAlreadyExistsException;
import java.util.Hashtable;

public class DatabaseManager <K,V> implements MessageConsumer<DataContent<K,V>> {
    /**
     * The actual database. Using {@link Hashtable} grants a thread-safe behaviour.
     */
    @Getter
    private Hashtable<K,V> database;
    private final ObjectOutputStream fileOut;
    private static DatabaseManager<?,?> instance;
    public static final String DEFAULT_PATH  = "./"+DatabaseManager.class.getName();

    @SuppressWarnings("unchecked")
    @SneakyThrows(ClassNotFoundException.class)
    private DatabaseManager(File file) throws IOException {

        fileOut = new ObjectOutputStream(new FileOutputStream(file));

        try(ObjectInputStream fin = new ObjectInputStream(new FileInputStream(file))){
            database = (Hashtable<K, V>) fin.readObject();
        } catch (IOException e) {
            database = new Hashtable<>();
        } catch (ClassCastException e){
            throw new FileSystemAlreadyExistsException("An instance with different types is already persisted");
        }

        DatabaseManager.instance = this;
    }

    @SuppressWarnings("unchecked")
    public static <K,V> DatabaseManager<K,V> getInstance(String persistencePath) throws IOException, IllegalAccessException {
        if(instance == null){
            return new DatabaseManager<>(new File(persistencePath));
        }
        try {
            return (DatabaseManager<K, V>) instance;
        }catch (ClassCastException e){
            throw new IllegalAccessException("Another instance with different types has already been created");
        }
    }

    public void persist() throws IOException {
        fileOut.writeObject(database);
    }

    @SneakyThrows
    public void close() {
        persist();
        fileOut.close();
    }

    @Override
    public void consumeMessage(Message<DataContent<K,V>> msg) {
        switch (msg.getContent().getOperations()) {
            case DELETE:
                database.remove(msg.getContent().getKey());
                break;
            case PUT:
                database.put(msg.getContent().getKey(), msg.getContent().getValue());
                break;
            default:
                throw new BrokenProtocolException("Unexpected value: "+msg);
        }
    }
}