package middleware.dataStructures;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ReplicaList {

    private static ReplicaList instance;
    @Getter
    private Map<Socket,Integer> vectorClocks = new HashMap<>();

    public static ReplicaList getInstance(){
        if(instance==null) instance = new ReplicaList();
        return instance;
    }

    public Set<Socket> getAllReplicas(){
        return vectorClocks.keySet();
    }

    public void put(Socket socket){
        vectorClocks.put(socket,0);
    }

}
