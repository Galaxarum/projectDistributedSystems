package middleware.group;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.net.Socket;

@Getter
@Setter
public class NodeInfo implements Serializable {
	private String hostname;
	private int port;
	private transient Socket socket;

	public NodeInfo(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
}
