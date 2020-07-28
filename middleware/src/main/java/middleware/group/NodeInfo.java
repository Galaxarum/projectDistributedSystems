package middleware.group;

import lombok.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

@Data
public class NodeInfo implements Serializable {
	@Setter(AccessLevel.NONE)
	private String hostname;
	@Setter(AccessLevel.NONE)
	private int port;
	@EqualsAndHashCode.Exclude @NonNull
	private transient Socket socket;
	@EqualsAndHashCode.Exclude @NonNull
	private transient ObjectOutputStream out;
	@EqualsAndHashCode.Exclude @NonNull
	private transient ObjectInputStream in;

	public NodeInfo(Socket socket) throws IOException {
		setSocketAndChannels(socket);
	}

	public NodeInfo(Socket socket, ObjectOutputStream out, ObjectInputStream in){
		this.socket = socket;
		this.hostname = socket.getInetAddress().getHostName();
		this.port = socket.getPort();
		this.out = out;
		this.in = in;
	}

	public void connect() throws IOException {
		setSocketAndChannels(new Socket(hostname,port));
	}

	private void setSocketAndChannels(Socket socket) throws IOException {
		this.socket = socket;
		this.out = new ObjectOutputStream(socket.getOutputStream());
		this.in = new ObjectInputStream(socket.getInputStream());
		this.hostname = socket.getInetAddress().getHostName();
		this.port = socket.getPort();
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
