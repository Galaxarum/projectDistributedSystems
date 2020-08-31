package it.polimi.cs.ds.distributed_storage.middleware.group;

import lombok.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

@Data @Setter(AccessLevel.NONE) @NoArgsConstructor
public class NodeInfo implements Serializable {

	public static final int MESSAGES_PORT_OFFSET = 1;

	private String hostname;
	private int port;
	@EqualsAndHashCode.Exclude @NonNull @Getter(AccessLevel.NONE)
	transient Socket groupSocket;
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
		setGroupChannel(groupSocket);
	}

	public NodeInfo(Socket groupSocket, ObjectOutputStream groupOut, ObjectInputStream groupIn) {
		setGroupChannel(groupSocket,groupOut,groupIn);
	}

	public NodeInfo(Socket groupSocket, ObjectOutputStream groupOut, ObjectInputStream groupIn,
	                Socket messageSocket,ObjectOutputStream messageOut, ObjectInputStream messageIn){
		setGroupChannel(groupSocket, groupOut, groupIn);
		setMessageSocket(messageSocket, messageOut, messageIn);
	}

	public void connect() throws IOException {
		setGroupChannel(new Socket(hostname,port));
		setMessageSocket(new Socket(hostname,port+MESSAGES_PORT_OFFSET));
	}

	public void setGroupChannel(Socket groupSocket, ObjectOutputStream groupOut, ObjectInputStream groupIn){
		if(this.groupSocket==null) {
			this.groupSocket = groupSocket;
			this.hostname = groupSocket.getInetAddress().getHostName();
			this.port = groupSocket.getPort();
			this.groupOut = groupOut;
			this.groupIn = groupIn;
		}else throw new UnsupportedOperationException("Sockets can be set only once");
	}

	public void setGroupChannel(Socket groupSocket) throws IOException {
		setGroupChannel(groupSocket,new ObjectOutputStream(groupSocket.getOutputStream()),new ObjectInputStream(groupSocket.getInputStream()));
	}

	public void setMessageSocket(Socket messageSocket,ObjectOutputStream out, ObjectInputStream in) {
		if(this.messageSocket==null) {
			this.messageSocket = messageSocket;
			this.messageOut = out;
			this.messageIn = in;
		}else throw new UnsupportedOperationException("Sockets can be set only once");
	}

	public void setMessageSocket(Socket messageSocket) throws IOException {
		setMessageSocket(messageSocket,new ObjectOutputStream(messageSocket.getOutputStream()),new ObjectInputStream(messageSocket.getInputStream()));
	}

	public void close(){
		try {
			if(groupSocket!=null) {
				groupOut.close();
				groupIn.close();
				groupSocket.close();
			}
			if(messageSocket!=null) {
				messageOut.close();
				messageIn.close();
				messageSocket.close();
			}
		}catch ( IOException e ){
			//Ignored
		}
	}

	public boolean isClosed(){
		return groupSocket.isClosed() || ( messageSocket!=null && messageSocket.isClosed() );
	}
}
