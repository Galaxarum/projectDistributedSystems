package middleware.group;

import org.junit.jupiter.api.*;

import java.io.*;
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
	private Thread acceptorThread;
	private InputStream sin;
	private OutputStream sout;

	@BeforeAll
	static void initServerSocket() throws IOException {
		serverSocket = new ServerSocket(SERVER_PORT);
	}

	@BeforeEach
	void initTested() throws IOException {
		acceptorThread = new Thread(()-> {
			try {
				socketServerSide = serverSocket.accept();
				sout = new ObjectOutputStream(socketServerSide.getOutputStream());
				sout.flush();
				sin = new ObjectInputStream(socketServerSide.getInputStream());
			} catch ( IOException e ) {
				fail();
			}
		});
		acceptorThread.start();
		socket = new Socket(HOST_NAME,SERVER_PORT);
	}

	@Test
	@DisplayName("Can construct and toString is not null")
	void constructorAndToString() throws IOException {
		tested = new NodeInfo(socket);
		assertNotNull(tested);
		assertNotNull(tested.toString());
	}

	@Test
	@DisplayName("Can create using existing socket preserving existing channels")
	void createWithExistingSocketWithoutCreatingChannels() throws IOException {
		try (
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
		) {
			tested = new NodeInfo(socket, out, in);
			assertSame(in,tested.getIn());
			assertSame(out,tested.getOut());
			assertEquals(socket, tested.getSocket());
		}
	}

	@Test
	@DisplayName("Can create channels on creation")
	void setNewSocketCreatingChannelsNoExceptions(){
		try {
			tested = new NodeInfo(socket);
		} catch ( IOException e ) {
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
			tested = new NodeInfo(socket);
		} catch (IOException e){
			fail();
		}
		assumeNotNull(tested.getIn());
		assumeNotNull(tested.getOut());
		assumeTrue(socket.equals(tested.getSocket()));
		tested.close();
		assertTrue(socket.isClosed());
	}


	@AfterEach
	void closeChannels(){
		acceptorThread.interrupt();
		try{
			if(socket!=null) socket.close();
		}catch ( IOException ignored ){
		}finally {
			socket=null;
		}
		try{
			if(socketServerSide!=null) socketServerSide.close();
		}catch ( IOException ignored ){
		}finally {
			socketServerSide=null;
		}
		try {
			sout.close();
			sin.close();
		}catch ( IOException ignored ){}
		finally {
			sout=null;sin=null;
		}
	}
}
