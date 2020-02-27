package dataStructures;

import java.util.List;

public class ReplicaList {

    private static ReplicaList instance;

    public static ReplicaList getInstance(){
        if(instance==null) instance = new ReplicaList();
        return instance;
    }

    public List<String> getAllReplicas(){
        //TODO
        return null;
    };
}
