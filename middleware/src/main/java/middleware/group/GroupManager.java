package middleware.group;

import lombok.SneakyThrows;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static middleware.group.GroupCommands.JOIN;
import static middleware.group.GroupCommands.JOINING;

public class GroupManager {

    private final String MY_ID;
    private final int PORT;
    private final Map<String,Socket> socketMap = new HashMap<>();   //TODO: this must be shared with messaging middleware impl

    public GroupManager(String id, int port){
        this.MY_ID = id;
        this.PORT = port;
        //TODO: start a server socket thread
    }

    @SneakyThrows(ClassNotFoundException.class)
    public void join(String knownHost) throws IOException {
        Socket knownSocket = new Socket(knownHost,PORT);
        ObjectOutputStream out = new ObjectOutputStream(knownSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(knownSocket.getInputStream());

        out.writeObject(JOIN);
        out.writeObject(MY_ID);

        Map<String,InetAddress> replicas  = (Map<String, InetAddress>) in.readObject();

        for (Map.Entry<String,InetAddress> e :replicas.entrySet()) {
            final String id = e.getKey();
            final InetAddress address = e.getValue();
            Socket newSocket = new Socket(address,PORT);
            ObjectOutputStream newOut = new ObjectOutputStream(newSocket.getOutputStream());
            newOut.writeObject(JOINING);
            socketMap.put(id,newSocket);
        }

    }

    public void leave() {

    }
}
