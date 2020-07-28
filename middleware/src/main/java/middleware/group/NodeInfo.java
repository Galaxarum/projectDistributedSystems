package middleware.group;

import lombok.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

@Data
@RequiredArgsConstructor
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
		setSocket(socket);
	}

	public String getHostname(){
		return socket.getInetAddress().getHostName();
	}

	public int getPort(){
		return socket.getPort();
	}

	public void connect() throws IOException {
		setSocket(new Socket(hostname,port));
	}

	private void setSocket(Socket socket) throws IOException {
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
