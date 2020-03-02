package middleware.group;

import exceptions.BrokenProtocolException;
import lombok.SneakyThrows;
import middleware.networkThreads.P2PConnection;
import middleware.primitives.GroupCommands;
import templates.ServerSocketRunnable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static middleware.primitives.GroupCommands.*;

public class GroupManager <K,V>{

    private final String MY_ID;
    private final int PORT;
    private Map<String,Socket> socketMap = new HashMap<>();   //TODO: this must be shared with messaging middleware impl
    private Map<String,NodeInfo> replicas = new HashMap<>();
    private Socket targetSocket;

    public GroupManager(String id, int port) throws IOException {
        this.MY_ID = id;
        this.PORT = port;
        new Thread(new ServerSocketRunnable<P2PConnection>(this.PORT)).start();
    }

    public Map<K,V> join(String knownHost) throws IOException {
        getReplicas(knownHost);

        new JoinGroupUseCase(this.targetSocket, this.replicas, this.MY_DEVICE_NAME, this.PORT).execute();

    }

    @SneakyThrows(ClassNotFoundException.class)
    private void getReplicas(String knownHost) throws IOException {
        targetSocket = new Socket(knownHost,PORT);

        ObjectOutputStream out = new ObjectOutputStream(targetSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(targetSocket.getInputStream());

        out.writeObject(JOIN);
        out.writeObject(MY_ID);

        try {
            replicas = (Map<String, NodeInfo>) in.readObject();
        }catch (ClassCastException e){
            throw new BrokenProtocolException("Expected instance of "+ Map.class.getName()+" of parameters "+String.class.getName()+","+NodeInfo.class.getName(),e);
        }

        for (Map.Entry<String,NodeInfo> singleNode :replicas.entrySet()) {
            final String id = singleNode.getKey();
            final NodeInfo nodeInfo = singleNode.getValue();
            Socket newSocket = new Socket(nodeInfo.getHostname(),nodeInfo.getPort());
            try(ObjectOutputStream newOut = new ObjectOutputStream(newSocket.getOutputStream());
                ObjectInputStream newIn = new ObjectInputStream(newSocket.getInputStream())){
                    newOut.writeObject(JOINING);
                    final GroupCommands ack = (GroupCommands) newIn.readObject();
                    if(!ACK.equals(ack))
                        throw new BrokenProtocolException("Expected "+ACK+" but "+newIn+" was received. This breaks the group joining protocol");
            }
            socketMap.put(id, newSocket);
        }
    }

    public void leave() {

    }
}
