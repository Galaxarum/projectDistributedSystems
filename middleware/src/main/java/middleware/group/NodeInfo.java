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
	private final String hostname;
	private final int port;
	@EqualsAndHashCode.Exclude
	private transient Socket socket;
	@EqualsAndHashCode.Exclude
	private transient ObjectOutputStream out;
	@EqualsAndHashCode.Exclude
	private transient ObjectInputStream in;

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
