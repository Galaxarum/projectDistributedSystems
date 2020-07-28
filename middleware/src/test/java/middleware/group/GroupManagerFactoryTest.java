package middleware.group;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.*;

/**
 * Run manually test by test
 */
public class GroupManagerFactoryTest {

	private static final String ID = "a";
	private static final int PORT = 12345;
	private static final int SERVER_PORT = 12121;
	private static final String HOST = "localhost";
	private static ServerSocket serverAcceptor;
	private Socket serverSocket;
	private Thread acceptingThread;
	private ObjectInputStream sin;
	private ObjectOutputStream sout;

	@BeforeAll
	static void startServerSocket() throws IOException {
		serverAcceptor = new ServerSocket(SERVER_PORT);
	}

	@BeforeEach
	void acceptConnection(){
		acceptingThread = new Thread(()-> {
			try {
				serverSocket = serverAcceptor.accept();
				sout = new ObjectOutputStream(serverSocket.getOutputStream());
				sin = new ObjectInputStream(serverSocket.getInputStream());
			} catch ( IOException e ) {
				fail();
			}
		});
		acceptingThread.start();
	}

	@AfterEach
	void closeSocketsAndStopThread() throws IOException {
		if(sin!=null) sin.close();
		if(sout!=null) sout.close();
		if(serverSocket!=null) serverSocket.close();
		if(acceptingThread!=null) acceptingThread.interrupt();
	}

	@Test
	@DisplayName("Factory is singleton")
	void singletonTest() throws IOException {
		final GroupManager<Integer,Integer> firstInstance =
				GroupManagerFactory.factory(ID,PORT,null,null,null);
		assertSame(firstInstance,GroupManagerFactory.factory(ID+ID,PORT+1,null,null,null));
	}

	@Test
	@DisplayName("If no leader is given a leader group manager is returned")
	void leaderCreationTest() throws IOException {
		final GroupManager<Integer,Integer> firstInstance =
				GroupManagerFactory.factory(ID,PORT,null,null,null);
		assertTrue(firstInstance instanceof LeaderGroupManager);
	}

	@Test
	@DisplayName("If a leader socket is given an ordinary group manager is returned")
	void ordinaryCreationTest() throws IOException {
		final GroupManager<Integer,Integer> firstInstance =
				GroupManagerFactory.factory(ID,PORT,new Socket(HOST,SERVER_PORT),null,null);
		assertTrue(firstInstance instanceof OrdinaryGroupManager);
	}
}
