package middleware.group;

import exceptions.BrokenProtocolException;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.Socket;

@Getter @Setter
public class NodeInfo{
	private String hostname;
	private int port;

	public Socket getSocket(){
		try {
			return new Socket(hostname,port);
		} catch (IOException e) {
			throw new BrokenProtocolException("Impossible to establish connection with the replica "+hostname+":"+port);
		}
	}
}
