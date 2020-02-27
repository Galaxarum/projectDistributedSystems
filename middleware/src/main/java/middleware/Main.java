package middleware;

import lombok.RequiredArgsConstructor;
import middleware.network.ReplicaSocketListener;
import middleware.network.ServerSocketListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class Main implements Runnable {

    private final String knownHost;
    private final int port;
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    @Override
    public void run() {
        //Accept incoming connections
        try{
            new Thread(new ServerSocketListener(new ServerSocket(port))).start();
        } catch (IOException e) {
            logger.severe("Unable to open ServerSocket");
            return;
        }
        //Open connection with known host
        try {
            Socket socket = new Socket(knownHost, port);
            new Thread(new ReplicaSocketListener(socket)).start();
        } catch (IOException e){
            logger.severe("Unable to open socket with known host");
            return;
        }
    }
}
