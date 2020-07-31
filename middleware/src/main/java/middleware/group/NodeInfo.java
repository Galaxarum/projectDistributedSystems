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
	private transient Socket groupSocket;
	@EqualsAndHashCode.Exclude @NonNull
	private transient ObjectOutputStream groupOut;
	@EqualsAndHashCode.Exclude @NonNull
	private transient ObjectInputStream groupIn;

	public NodeInfo(Socket groupSocket) throws IOException {
		setSocketAndChannels(groupSocket);
	}

	public NodeInfo(Socket groupSocket, ObjectOutputStream groupOut, ObjectInputStream groupIn){
		this.groupSocket = groupSocket;
		this.hostname = groupSocket.getInetAddress().getHostName();
		this.port = groupSocket.getPort();
		this.groupOut = groupOut;
		this.groupIn = groupIn;
	}

	public void connect() throws IOException {
		setSocketAndChannels(new Socket(hostname,port));
	}

	private void setSocketAndChannels(Socket socket) throws IOException {
		this.groupSocket = socket;
		this.groupOut = new ObjectOutputStream(socket.getOutputStream());
		this.groupIn = new ObjectInputStream(socket.getInputStream());
		this.hostname = socket.getInetAddress().getHostName();
		this.port = socket.getPort();
	}

	public void close(){
		try {
			groupOut.close();
			groupIn.close();
			groupSocket.close();
		}catch ( IOException e ){
			//Ignored
		}
	}
}
