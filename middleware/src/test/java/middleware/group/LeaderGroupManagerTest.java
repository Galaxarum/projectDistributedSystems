package middleware.group;

import exceptions.ParsingException;
import middleware.messages.VectorClock;
import org.junit.Rule;
import org.junit.jupiter.api.*;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static middleware.group.GroupCommands.*;
import static org.junit.Assert.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LeaderGroupManagerTest {
	private static LeaderGroupManager<Integer,Integer> tested;
	private static final String ID = "leader";
	private static final String REPLICA_ID = "replica";
	private static final String LEAVING_REPLICA_ID = "leaving";
	private static final int PORT = 12345;
	private static final int SERVER_PORT = 12121;
	private static final String HOSTNAME = "localhost";
	private static ServerSocket socketAcceptor;
	private static Socket serverSocket;
	private static Socket socket;
	private static ObjectOutputStream cout;
	private static ObjectInputStream cin;
	private static ObjectOutputStream sout;
	private static ObjectInputStream sin;
	private static Thread acceptorThread;
	private static final Map<String,NodeInfo> replicas = new HashMap<>();
	private static final Map<Integer,Integer> data = new HashMap<>();
	private static final VectorClock vectorClock = new VectorClock(ID);

	@Rule
	ExpectedException expectedException = ExpectedException.none();

	@BeforeAll
	static void initServer() throws IOException {
		socketAcceptor = new ServerSocket(SERVER_PORT);
		acceptorThread = new Thread(()->{
			try {
				serverSocket = socketAcceptor.accept();
				sout = new ObjectOutputStream(serverSocket.getOutputStream());
				sin = new ObjectInputStream(serverSocket.getInputStream());
			} catch ( IOException e ) {
				fail();
			}
		});
		acceptorThread.start();
		tested = ( LeaderGroupManager<Integer, Integer> ) GroupManagerFactory.factory(ID,PORT,serverSocket,replicas,data);
		socket = new Socket(HOSTNAME,SERVER_PORT);
		cout = new ObjectOutputStream(socket.getOutputStream());
		cin = new ObjectInputStream(socket.getInputStream());
	}

	@Test
	@DisplayName("Safety test")
	@Order(1)
	void factoryOk(){
		assertNotNull(tested);
		assertTrue(tested instanceof LeaderGroupManager);
	}

	@Test
	@DisplayName("Join throws no exception")
	@Order(2)
	void join_noException(){
		tested.join(vectorClock);
	}

	@Test
	@Order(3)
	void parseJoinTest() throws ParsingException, IOException, ClassNotFoundException {
		final Thread clientThread = new Thread(()->{
			try {
				cout.writeObject(REPLICA_ID);
				cout.writeObject(LEAVING_REPLICA_ID);
				cout.flush();
			}catch ( IOException e ) {
				fail();
			}
		});
		clientThread.start();
		final Map<String, NodeInfo> replicasBefore = new HashMap<>(replicas);
		tested.parse(JOIN,sout,sin,serverSocket);
		assertEquals(ID,cin.readObject());
		assertEquals(replicasBefore,cin.readObject());
		assertTrue(replicas.containsKey(REPLICA_ID));

		replicasBefore.put(REPLICA_ID,replicas.get(REPLICA_ID));

		tested.parse(JOIN,sout,sin,serverSocket);
		cin.readObject();
		cin.readObject();
		assertTrue(replicas.containsKey(REPLICA_ID));
		assertTrue(replicas.containsKey(LEAVING_REPLICA_ID));

		clientThread.interrupt();
	}

	@Test
	@Order(4)
	void parseSynchTest() throws ParsingException, IOException, ClassNotFoundException {
		tested.parse(SYNC,sout,sin,serverSocket);
		assertEquals(data,cin.readObject());
		assertEquals(vectorClock,cin.readObject());
	}

	@Test
	@Order(5)
	void parseLeaveTest() throws ParsingException {
		final Thread leavingClientThread = new Thread(()->{
			try {
				cout.writeObject(LEAVING_REPLICA_ID);
			} catch ( IOException e ) {
				fail();
			}
		});
		leavingClientThread.start();
		tested.parse(LEAVE,sout,sin,serverSocket);
		assertFalse(replicas.containsKey(LEAVING_REPLICA_ID));
	}

	@Test
	@Order(6)
	@DisplayName("Exception raised if trying to parse JOINING")
	void parseJoiningTest() {
		try{
			tested.parse(JOINING,sout,sin,serverSocket);
		}catch ( ParsingException e ){
			assertTrue(e.getMessage().contains(JOINING.toString()));
			return;
		}
		fail();
	}

	@Test
	@Order(6)
	@DisplayName("Exception raised if trying to parse ACK")
	void parseAckTest() {
		try{
			tested.parse(ACK,sout,sin,serverSocket);
		}catch ( ParsingException e ){
			assertTrue(e.getMessage().contains(ACK.toString()));
			return;
		}
		fail();
	}

	@Test
	@DisplayName("Leave unsupported")
	void leave_unsupportedException(){
		assertThrows(UnsupportedOperationException.class,()->tested.leave());
	}

	@AfterAll
	static void closeConnections() throws IOException {
		acceptorThread.interrupt();
		if(serverSocket!=null) serverSocket.close();
		socketAcceptor.close();
	}


}
