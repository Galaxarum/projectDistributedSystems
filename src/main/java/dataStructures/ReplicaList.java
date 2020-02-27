package dataStructures;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
