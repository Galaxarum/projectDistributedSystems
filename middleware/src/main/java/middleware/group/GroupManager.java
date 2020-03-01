package middleware.group;

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

public class GroupManager {

    private final String MY_DEVICE_NAME;
    private final int PORT;
    private Map<String,Socket> socketMap = new HashMap<>();   //TODO: this must be shared with messaging middleware impl
    private Map<String,NodeInfo> replicas = new HashMap<>();
    private Socket targetSocket;

    public GroupManager(String id, int port) throws IOException {
        this.MY_DEVICE_NAME = id;
        this.PORT = port;
        new Thread(new ServerSocketRunnable<P2PConnection>(this.PORT)).start();
    }

    public void join(String knownHost) throws IOException {
        getReplicas(knownHost);

        new JoinGroupUseCase(this.targetSocket, this.replicas, this.MY_DEVICE_NAME, this.PORT).execute();

    }

    @SneakyThrows(ClassNotFoundException.class)
    private void getReplicas(String knownHost) throws IOException {
        targetSocket = new Socket(knownHost,PORT);

        ObjectOutputStream out = new ObjectOutputStream(targetSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(targetSocket.getInputStream());

        out.writeObject(GroupCommands.JOIN);
        out.writeObject(MY_DEVICE_NAME);

        replicas = (Map<String, NodeInfo>) in.readObject();

        for (Map.Entry<String,NodeInfo> singleNode :replicas.entrySet()) {
            final String id = singleNode.getKey();
            final NodeInfo nodeInfo = singleNode.getValue();
            Socket newSocket = new Socket(nodeInfo.getHostname(),nodeInfo.getPort());
            socketMap.put(id, newSocket);
        }
    }

    public void leave() {

    }
}
