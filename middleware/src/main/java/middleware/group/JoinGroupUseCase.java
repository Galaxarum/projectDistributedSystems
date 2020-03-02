package middleware.group;

import exceptions.BrokenProtocolException;
import middleware.primitives.GroupCommands;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class JoinGroupUseCase <K,V>{
    private Map<String, Socket> socketMap;
    private Socket targetSocket;

    public JoinGroupUseCase(Socket targetSocket, Map<String, Socket> socketMap) {
        this.targetSocket = targetSocket;
        this.socketMap = socketMap;
    }

    public Map<K,V> execute() throws IOException {
        Map<K,V> data = synch(targetSocket);
        multicastAck();
        return data;
    }

    private Map<K,V> synch(Socket targetSocket) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(targetSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(targetSocket.getInputStream());
        out.writeObject(GroupCommands.SYNC);
        try {
            return (Map<K,V>) in.readObject();
        }catch (ClassCastException | ClassNotFoundException e){
            throw new BrokenProtocolException("Impossible to initialize data from known replica",e);
        }
    }

    private void multicastAck() {
        socketMap.forEach((key, value) -> {
            try(ObjectOutputStream out = new ObjectOutputStream(value.getOutputStream());) {
                out.writeObject(GroupCommands.ACK);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
