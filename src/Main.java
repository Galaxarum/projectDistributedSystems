import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        String knownPeer = args[0];
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 12345;
        Socket socket;  //To write messages to the known peer
        ServerSocket socketIn;  //To recive messages
        try {
            socket = new Socket(knownPeer,port);
            socketIn = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Cannot connect to the known peer");
        }
    }
}
