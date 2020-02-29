package distributedStorage.network;

import lombok.RequiredArgsConstructor;

import java.net.Socket;

@RequiredArgsConstructor
public class ClientCommandListener implements Runnable {

    private final Socket clientSocket;

    @Override
    public void run() {
        while (!clientSocket.isClosed()){
            //TODO: parse client commands
        }
    }
}
