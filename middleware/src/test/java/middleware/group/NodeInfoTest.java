package middleware.group;

import org.junit.Rule;
import org.junit.jupiter.api.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class NodeInfoTest {

	private static final String HOST_NAME = "localhost";
	private static int SERVER_PORT;
	private NodeInfo tested;
	private Socket socket;
	private static ServerSocket serverSocket;
	private Socket socketServerSide;
	private Thread acceptorThread;
	private InputStream sin;
	private OutputStream sout;

	@Rule private final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@BeforeAll
	static void initServerSocket() throws IOException {
		serverSocket = new ServerSocket(0);
		SERVER_PORT = serverSocket.getLocalPort();
	}

	@BeforeEach
	void initTested() throws IOException {
		acceptorThread = new Thread(()-> {
			while ( acceptorThread.isAlive() ) {
				try {
					socketServerSide = serverSocket.accept();
					sout = new ObjectOutputStream(socketServerSide.getOutputStream());
					sout.flush();
					sin = new ObjectInputStream(socketServerSide.getInputStream());
				} catch ( IOException e ) {
					fail();
				}
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
			assertSame(in,tested.getGroupIn());
			assertSame(out,tested.getGroupOut());
			assertEquals(socket, tested.getGroupSocket());
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
		assertNotNull(tested.getGroupIn());
		assertNotNull(tested.getGroupOut());
		assertEquals(socket,tested.getGroupSocket());
	}

	@Test
	@DisplayName("Can use nodeinfo to close the socket")
	void closeClosesUnderlyingConnections() {
		try {
			tested = new NodeInfo(socket);
		} catch (IOException e){
			fail();
		}
		assumeNotNull(tested.getGroupIn());
		assumeNotNull(tested.getGroupOut());
		assumeTrue(socket.equals(tested.getGroupSocket()));
		tested.close();
		assertTrue(socket.isClosed());
	}

	@Test
	@DisplayName("A deserialized nodeInfo can be used to open a fresh connection")
	void canCreateConnectionAfterDeserialization() throws IOException {
		temporaryFolder.create();
		tested = new NodeInfo(socket);
		final File forSerialize = temporaryFolder.newFile();
		final ObjectOutputStream fout = new ObjectOutputStream(new FileOutputStream(forSerialize));
		fout.writeObject(tested);
		final ObjectInputStream fin = new ObjectInputStream(new FileInputStream(forSerialize));
		final NodeInfo deserialized;
		try {
			deserialized = ( NodeInfo ) fin.readObject();
		} catch ( ClassNotFoundException e ) {
			fail();
			return;
		}
		assertDoesNotThrow(deserialized::connect);
		assertFalse(deserialized.getGroupSocket().isClosed());
		forSerialize.delete();
		temporaryFolder.delete();
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
		}catch ( IOException | NullPointerException ignored ){}
		finally {
			sout=null;sin=null;
		}
	}
}
