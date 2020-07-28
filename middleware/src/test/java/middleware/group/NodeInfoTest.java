package middleware.group;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class NodeInfoTest {

	private static final String HOST_NAME = "localhost";
	private static final int PORT = 12345;
	private static final int SERVER_PORT = 12121;
	private NodeInfo tested;
	private Socket socket;
	private static ServerSocket serverSocket;
	private Socket socketServerSide;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private ObjectOutputStream sout;
	private ObjectInputStream sin;

	@BeforeAll
	static void initServerSocket() throws IOException {
		serverSocket = new ServerSocket(SERVER_PORT);
	}

	@BeforeEach
	void initTested() throws IOException {
		tested = new NodeInfo(HOST_NAME,SERVER_PORT);
		new Thread(()-> {
			try {
				socketServerSide = serverSocket.accept();
				sout = new ObjectOutputStream(socketServerSide.getOutputStream());
				sin = new ObjectInputStream(socketServerSide.getInputStream());
			} catch ( IOException e ) {
				//ignored
			}
		}).start();
		socket = new Socket(HOST_NAME,SERVER_PORT);
	}

	@AfterEach
	void closeChannels(){
		try{
			if(out!=null) out.close();
			if(in!=null) in.close();
		}catch ( IOException e ){
			//ignored
		}finally {
			out=null;
			in=null;
		}
		try{
			if(socket!=null) socket.close();
		}catch ( IOException e ){
			//ignored
		}finally {
			socket=null;
		}
		try{
			if(socketServerSide!=null) socketServerSide.close();
		}catch ( IOException e ){
			//ignored
		}finally {
			socketServerSide=null;
		}
	}

	@Test
	@DisplayName("Can construct and toString is not null")
	void constructorAndToString(){
		assertNotNull(tested);
		assertNotNull(tested.toString());
	}

	@Test
	@DisplayName("Can assign existing socket having existing channels")
	void setExistingSocketWithoutCreatingChannels() throws IOException {
		out  =  new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());
		try{
			tested.setSocket(socket,false);
		}catch ( IOException e ){
			fail();
		}
		assertNull(tested.getIn());
		assertNull(tested.getOut());
		assertEquals(socket,tested.getSocket());
	}

	@Test
	@DisplayName("Can create channels")
	void setNewSocketCreatingChannelsNoExceptions(){
		try{
			tested.setSocket(socket,true);
		}catch ( IOException e ){
			fail();
		}
		assertNotNull(tested.getIn());
		assertNotNull(tested.getOut());
		assertEquals(socket,tested.getSocket());
	}

	@Test
	@DisplayName("Can use nodeinfo to close the socket")
	void closeClosesUnderlyingConnections() {
		try {
			tested.setSocket(socket, true);
		} catch (IOException e){
			fail("Cannot create socket channels");
		}
		assumeNotNull(tested.getIn());
		assumeNotNull(tested.getOut());
		assumeTrue(socket.equals(tested.getSocket()));
		tested.close();
		assertTrue(socket.isClosed());
	}

}
