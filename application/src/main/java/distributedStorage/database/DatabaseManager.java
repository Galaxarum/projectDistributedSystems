package distributedStorage.database;

public class DatabaseManager {

    private static Database database;

    public DatabaseManager() {
        if(database == null) database = new Database();
    }

    public static Database get() {
        return DatabaseManager.database;
    }

    public static void set(Database database) {
        DatabaseManager.database = database;
    }
}