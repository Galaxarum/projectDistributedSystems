import interfaces.DataStoreInterface;
import interfaces.GroupCommunicationInterface;

public class Main {
    public static void main(String[] args) {
        String knownPeer = args[0];
        GroupCommunicationInterface groupCom = null; //=...
        groupCom.join(knownPeer);
        //Get command from client
        DataStoreInterface<Integer,Object> dataStore = null;
        dataStore.put(1,"ciao");
        dataStore.get(1);
        dataStore.delete(1);
        groupCom.leave();
    }
}
