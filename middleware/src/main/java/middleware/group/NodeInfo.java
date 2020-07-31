package middleware.group;

import lombok.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

@Data @Setter(AccessLevel.NONE)
public class NodeInfo implements Serializable {

	public static final int MESSAGES_PORT_OFFSET = 1;

	private String hostname;
	private int port;
	@EqualsAndHashCode.Exclude @NonNull
	private transient Socket groupSocket;
	@EqualsAndHashCode.Exclude @NonNull
	private transient ObjectOutputStream groupOut;
	@EqualsAndHashCode.Exclude @NonNull
	private transient ObjectInputStream groupIn;
	@EqualsAndHashCode.Exclude @NonNull @Getter(AccessLevel.NONE)
	private transient Socket messageSocket;
	@EqualsAndHashCode.Exclude @NonNull
	private transient ObjectOutputStream messageOut;
	@EqualsAndHashCode.Exclude @NonNull
	private transient ObjectInputStream messageIn;

	public NodeInfo(Socket groupSocket) throws IOException {
		setSocketAndChannels(groupSocket);
	}

	public NodeInfo(Socket groupSocket, ObjectOutputStream groupOut, ObjectInputStream groupIn) throws IOException {
		this.groupSocket = groupSocket;
		this.hostname = groupSocket.getInetAddress().getHostName();
		this.port = groupSocket.getPort();
		this.messageSocket = new Socket(this.hostname,port+MESSAGES_PORT_OFFSET);
		this.groupOut = groupOut;
		this.groupIn = groupIn;
		this.messageOut = new ObjectOutputStream(messageSocket.getOutputStream());
		this.messageIn = new ObjectInputStream(messageSocket.getInputStream());
	}

	public void connect() throws IOException {
		setSocketAndChannels(new Socket(hostname,port));
	}

	private void setSocketAndChannels(Socket socket) throws IOException {
		this.groupSocket = socket;
		this.hostname = socket.getInetAddress().getHostName();
		this.port = socket.getPort();
		this.messageSocket = new Socket(hostname,port+MESSAGES_PORT_OFFSET);
		this.groupOut = new ObjectOutputStream(socket.getOutputStream());
		this.groupIn = new ObjectInputStream(socket.getInputStream());
		this.messageOut = new ObjectOutputStream(messageSocket.getOutputStream());
		this.messageIn = new ObjectInputStream(messageSocket.getInputStream());
	}

	public void close(){
		try {
			groupOut.close();
			groupIn.close();
			groupSocket.close();
			messageOut.close();
			messageIn.close();
			messageSocket.close();
		}catch ( IOException e ){
			//Ignored
		}
	}
}
