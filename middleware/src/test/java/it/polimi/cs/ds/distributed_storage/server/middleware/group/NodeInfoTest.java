package it.polimi.cs.ds.distributed_storage.server.middleware.group;

import org.junit.Rule;
import org.junit.jupiter.api.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static java.lang.Thread.MAX_PRIORITY;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class NodeInfoTest {

	private static final String HOST_NAME = "localhost";
	private static int SERVER_PORT;
	private NodeInfo tested;
	private Socket socket;
	private static ServerSocket serverSocket;
	private static ServerSocket messageServerSocket;
	private Socket socketServerSide;
	private Socket messageSocket;
	private Thread acceptorThread;
	private Thread messagegAcceptorThread;
	private ObjectInputStream gin;
	private ObjectOutputStream gout;
	private ObjectOutputStream mout;
	private ObjectInputStream min;

	@Rule private final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@BeforeAll
	static void initServerSocket() throws IOException {
		serverSocket = new ServerSocket(0);
		SERVER_PORT = serverSocket.getLocalPort();
		messageServerSocket = new ServerSocket(SERVER_PORT+NodeInfo.MESSAGES_PORT_OFFSET);
	}

	@BeforeEach
	void initTested() throws IOException {
		acceptorThread = new Thread(()-> {
			while ( !serverSocket.isClosed()) try {
				socketServerSide = serverSocket.accept();
				gout = new ObjectOutputStream(socketServerSide.getOutputStream());
				gin = new ObjectInputStream(socketServerSide.getInputStream());
				gout.flush();
			} catch ( IOException e ) {
				e.printStackTrace();
				fail();
			}
		});

		messagegAcceptorThread =  new Thread(()->{
			while ( !serverSocket.isClosed()) try {
				messageSocket = messageServerSocket.accept();
				mout = new ObjectOutputStream(messageSocket.getOutputStream());
				min = new ObjectInputStream(messageSocket.getInputStream());
				mout.flush();
			} catch ( IOException e ) {
				e.printStackTrace();
				fail();
			}
		});

		acceptorThread.setPriority(Thread.MAX_PRIORITY);
		messagegAcceptorThread.setPriority(MAX_PRIORITY);
		acceptorThread.start();
		messagegAcceptorThread.start();
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
			out.writeInt(3);
			gout.writeInt(4);
			out.flush();
			gout.flush();
			assertEquals(3,gin.readInt());
			assertEquals(4,in.readInt());
		}
	}

	@Test
	@DisplayName("Can create channels on creation")
	void setNewSocketCreatingChannelsNoExceptions() throws IOException {
		tested = new NodeInfo(socket);
		assertNotNull(tested.getGroupIn());
		assertNotNull(tested.getGroupOut());
	}

	@Test
	@DisplayName("Can use nodeinfo to close the socket")
	void closeClosesUnderlyingConnections() throws IOException {
		tested = new NodeInfo(socket);
		assumeNotNull(tested.getGroupIn());
		assumeNotNull(tested.getGroupOut());
		tested.close();
		assertTrue(socket.isClosed());
	}

	@Test
	@DisplayName("A deserialized nodeInfo can be used to open a fresh connection")
	void canCreateConnectionAfterDeserialization() throws IOException, ClassNotFoundException {
		temporaryFolder.create();
		tested = new NodeInfo(socket);
		final File forSerialize = temporaryFolder.newFile();
		final ObjectOutputStream fout = new ObjectOutputStream(new FileOutputStream(forSerialize));
		fout.writeObject(tested);
		final ObjectInputStream fin = new ObjectInputStream(new FileInputStream(forSerialize));
		final NodeInfo deserialized;
		deserialized = ( NodeInfo ) fin.readObject();
		assertDoesNotThrow(deserialized::connect);
		//TODO: try to send something

		forSerialize.delete();
		temporaryFolder.delete();
	}

	@AfterEach
	void closeChannels() throws IOException {
		acceptorThread.interrupt();
		if(socket!=null && !socket.isClosed()) socket.close();
		if(socketServerSide!=null && !socketServerSide.isClosed()) socketServerSide.close();
		if(messageSocket!=null && !messageSocket.isClosed()) messageSocket.close();
		try {
			if(gout!=null) gout.close();
			if(gin!=null)gin.close();
			if(mout!=null)mout.close();
			if(min!=null)min.close();
		}catch ( IOException ignored ){}
	}
}
