package interfaces;

import java.net.Socket;

/**
 * Operations of this interfaces must be synchronized on a distributed level
 */
public interface groupCommunicationInterface {
    void join(Socket knownPeer);
    void leave();
}
