package middleware.network;

import middleware.dataStructures.ReplicaList;
import lombok.RequiredArgsConstructor;
import middleware.messages.HandShakeMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class ServerSocketListener implements Runnable {

    private final ServerSocket serverSocket;
    private static final Logger logger = Logger.getLogger(ServerSocketListener.class.getName());

    @Override
    public void run() {
        ReplicaList replicaList = ReplicaList.getInstance();
        while (true){
            try {
                final Socket socket = serverSocket.accept();
                final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                final HandShakeMessage handShakeMessage  = (HandShakeMessage) in.readObject();

                if(handShakeMessage.isFromClient()){
                    new Thread(new ClientSocketListener(socket)).start();
                }else {
                    replicaList.put(socket); // The other replicas will be updated by the ReplicaSocketListener
                    new Thread(new ReplicaSocketListener(socket)).start();
                }

            } catch (IOException e) {
                logger.warning("Failed to open an incoming connection");
            } catch (ClassNotFoundException e) {
                logger.severe("Cannot deserialize the connection type message");
            }
        }
    }

}
