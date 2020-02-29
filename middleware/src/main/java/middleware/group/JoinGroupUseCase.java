package middleware.group;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class JoinGroupUseCase {
    private Map<String,NodeInfo> replicas;
    private Map<String, Socket> socketMap = new HashMap<>();
    private String myDeviceName;
    private int myPort;
    private Socket targetSocket;

    public JoinGroupUseCase(Socket targetSocket, Map<String, NodeInfo> replicas, String myDeviceName, int myPort) {
        this.replicas = replicas;
        this.myDeviceName = myDeviceName;
        this.myPort = myPort;
        this.targetSocket = targetSocket;
    }

    public void execute() throws IOException {
        sendJoin();
        sendSync(targetSocket);
        sendReadyToAll();
    }

    private void sendJoin(){
        socketMap.entrySet().stream().forEach(replica -> {
            try{
                ObjectOutputStream newOut = new ObjectOutputStream(replica.getValue().getOutputStream());
                newOut.writeObject(GroupCommands.JOINING);
            } catch (IOException e){
                e.printStackTrace();
            }
        });
    }

    private void sendSync(Socket targetSocket) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(targetSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(targetSocket.getInputStream());

        out.writeObject(GroupCommands.SYNC);
        out.writeObject(myDeviceName);
    }

    private void sendReadyToAll()  {
        socketMap.entrySet().stream().forEach(replica -> {
            try {
                ObjectOutputStream out = new ObjectOutputStream(replica.getValue().getOutputStream());
                out.writeObject(GroupCommands.READY);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
