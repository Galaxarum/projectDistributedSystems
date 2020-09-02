package it.polimi.cs.ds.distributed_storage.server.database;

import it.polimi.cs.ds.distributed_storage.server.exceptions.BrokenProtocolException;
import lombok.Getter;
import lombok.SneakyThrows;
import it.polimi.cs.ds.distributed_storage.server.middleware.messages.Message;
import it.polimi.cs.ds.distributed_storage.server.middleware.messages.MessageConsumer;

import java.io.*;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.util.Hashtable;
import java.util.Map;

public class DatabaseManager <K extends Serializable,V extends Serializable> implements MessageConsumer<DataContent<K,V>> {
    /**
     * The actual database. Using {@link Hashtable} grants a thread-safe behaviour.
     */
    @Getter
    private final Hashtable<K,V> database;
    private final ObjectOutputStream fileOut;
    public static final String DEFAULT_PATH  = "./"+DatabaseManager.class.getName();

    @SuppressWarnings("unchecked")
    public DatabaseManager(String persistencePath) throws IOException {
        final File persistedFile  = new File(persistencePath);
        if(!persistedFile.exists() || persistedFile.length()==4) database = new Hashtable<>();
        else if(!persistedFile.isFile()) throw new FileSystemException(persistencePath + "is not a valid file");
        else if(!persistedFile.canRead() || !persistedFile.canWrite()) throw new FileSystemException("Missing read/write rigths over " + persistencePath);
        else try ( ObjectInputStream fin = new ObjectInputStream(new FileInputStream(persistedFile)) ) {
                database = ( Hashtable<K, V> ) fin.readObject();
            } catch ( ClassNotFoundException e ) {
                throw new FileSystemAlreadyExistsException("An instance with different types is already persisted for path " + persistencePath);
            }
        fileOut = new ObjectOutputStream(new FileOutputStream(persistedFile));
    }

    @SneakyThrows(IOException.class)
    public void close() {
        fileOut.writeObject(database);
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

    public <Key extends Serializable, Value extends Serializable> void putAll(Hashtable<Key,Value> data){
        database.putAll(( Map<? extends K, ? extends V> ) data);
    }
}