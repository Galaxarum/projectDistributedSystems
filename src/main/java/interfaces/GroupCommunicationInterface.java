package interfaces;

import java.net.Socket;

/**
 * Operations of this interfaces must be synchronized on a distributed level
 * Subclasses must implement scalar clock
 */
public interface GroupCommunicationInterface {
    void join(String knownPeer);
    void leave();
}
