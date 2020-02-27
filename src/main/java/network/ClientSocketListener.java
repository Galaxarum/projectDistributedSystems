package network;

import lombok.RequiredArgsConstructor;

import java.net.Socket;

@RequiredArgsConstructor
public class ClientSocketListener implements Runnable {

    private final Socket socket;

    @Override
    public void run() {
        while (!socket.isClosed()){
            //TODO: communication with a client
        }
    }
}
