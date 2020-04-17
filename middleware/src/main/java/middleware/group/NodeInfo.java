package middleware.group;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

@Getter
@Setter
public class NodeInfo implements Serializable {
	private String hostname;
	private int port;
	private transient Socket socket;
	private transient ObjectOutputStream out;
	private transient ObjectInputStream in;

	public NodeInfo(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}

	public void setSocket(Socket socket,boolean createStreams) throws IOException {
		this.socket = socket;
		if(createStreams) {
			this.out = new ObjectOutputStream(socket.getOutputStream());
			this.in = new ObjectInputStream(socket.getInputStream());
		}
	}

	public void close(){
		try {
			out.close();
			in.close();
			socket.close();
		}catch ( IOException e ){
			//Ignored
		}
	}
}
