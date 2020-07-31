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
import java.util.*;

import static middleware.group.GroupCommands.*;
import static org.junit.Assert.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LeaderGroupManagerTest {
	private static LeaderGroupManager<Integer,Integer> tested;
	private static final String ID = "leader";
	private static final String REPLICA_ID = "replica";
	private static final String LEAVING_REPLICA_ID = "leaving";
	private static final int PORT = 12345;
	private static final String HOSTNAME = "localhost";
	private static ServerSocket socketAcceptor;
	private static final Map<String,NodeInfo> sockets = new HashMap<>(2);
	private static final List<NodeInfo> serverSockets = new ArrayList<>(2);
	private static Thread acceptorThread;
	private static final Map<String,NodeInfo> replicas = new HashMap<>();
	private static final Map<Integer,Integer> data = new HashMap<>();
	private static final VectorClock vectorClock = new VectorClock(ID);

	@Rule
	ExpectedException expectedException = ExpectedException.none();

	@BeforeAll
	static void initServer() throws IOException {
		socketAcceptor = new ServerSocket(0);
		int SERVER_PORT = socketAcceptor.getLocalPort();
		acceptorThread = new Thread(()->{
			while ( !socketAcceptor.isClosed() ){
				try {
					final Socket accepted = socketAcceptor.accept();
					serverSockets.add(new NodeInfo(accepted));
				} catch ( IOException e ) {
					fail();
				}
			}
		});
		acceptorThread.start();
		tested = ( LeaderGroupManager<Integer, Integer> ) GroupManagerFactory.factory(ID,PORT,null,replicas,data);
		sockets.put(REPLICA_ID,new NodeInfo(new Socket(HOSTNAME, SERVER_PORT)));
		sockets.put(LEAVING_REPLICA_ID,new NodeInfo(new Socket(HOSTNAME, SERVER_PORT)));
	}

	@Test
	@DisplayName("Safety test")
	@Order(1)
	void factoryOk(){
		assertNotNull(tested);
		assertTrue(tested instanceof LeaderGroupManager);
		assertEquals(2,sockets.size());
		assertEquals(2,serverSockets.size());
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

		sockets.get(REPLICA_ID).getGroupOut().writeObject(REPLICA_ID);
		sockets.get(LEAVING_REPLICA_ID).getGroupOut().writeObject(LEAVING_REPLICA_ID);

		final Map<String, NodeInfo> replicasBefore = new HashMap<>(replicas);
		tested.parse(JOIN,serverSockets.get(0).getGroupOut(),serverSockets.get(0).getGroupIn(), serverSockets.get(0).getGroupSocket());
		assertEquals(ID, sockets.get(REPLICA_ID).getGroupIn().readObject());
		assertEquals(replicasBefore, sockets.get(REPLICA_ID).getGroupIn().readObject());
		assertTrue(replicas.containsKey(REPLICA_ID));

		replicasBefore.put(REPLICA_ID,replicas.get(REPLICA_ID));

		tested.parse(JOIN,serverSockets.get(1).getGroupOut(),serverSockets.get(1).getGroupIn(), serverSockets.get(1).getGroupSocket());
		assertEquals(ID, sockets.get(LEAVING_REPLICA_ID).getGroupIn().readObject());
		assertEquals(replicasBefore, sockets.get(LEAVING_REPLICA_ID).getGroupIn().readObject());
		assertTrue(replicas.containsKey(REPLICA_ID));
		assertTrue(replicas.containsKey(LEAVING_REPLICA_ID));

	}

	@Test
	@Order(4)
	void parseSynchTest() throws ParsingException, IOException, ClassNotFoundException {
		tested.parse(SYNC,serverSockets.get(0).getGroupOut(),serverSockets.get(0).getGroupIn(),serverSockets.get(0).getGroupSocket());
		assertEquals(data, sockets.get(REPLICA_ID).getGroupIn().readObject());
		assertEquals(vectorClock, sockets.get(REPLICA_ID).getGroupIn().readObject());
	}

	@Test
	@Order(5)
	void parseLeaveTest() throws ParsingException, IOException {

		sockets.get(LEAVING_REPLICA_ID).getGroupOut().writeObject(LEAVING_REPLICA_ID);

		tested.parse(LEAVE,serverSockets.get(1).getGroupOut(),serverSockets.get(1).getGroupIn(),serverSockets.get(1).getGroupSocket());
		assertFalse(replicas.containsKey(LEAVING_REPLICA_ID));
	}

	@Test
	@Order(6)
	@DisplayName("Exception raised if trying to parse JOINING")
	void parseJoiningTest() {
		try{
			tested.parse(JOINING,serverSockets.get(0).getGroupOut(),serverSockets.get(0).getGroupIn(),serverSockets.get(0).getGroupSocket());
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
			tested.parse(ACK,serverSockets.get(0).getGroupOut(),serverSockets.get(0).getGroupIn(),serverSockets.get(0).getGroupSocket());
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
		for ( NodeInfo serverSocket: serverSockets )
			serverSocket.close();
		for ( NodeInfo socket: sockets.values() )
			socket.close();
		socketAcceptor.close();
	}


}

class replicaStub{
	private final Socket socket;
	private final ObjectInputStream in;
	private final ObjectOutputStream out;

	replicaStub(String host, int port) throws IOException {
		socket = new Socket(host,port);
		out=new ObjectOutputStream(socket.getOutputStream());
		in=new ObjectInputStream(socket.getInputStream());
	}

	void send(Object object) throws IOException {
		out.writeObject(object);
		out.flush();
	}

	Object read() throws IOException, ClassNotFoundException {
		return in.readObject();
	}

	void close(){
		try{in.close();}catch ( IOException ignored ){}
		try{out.close();}catch ( IOException ignored ){}
		try{socket.close();}catch ( IOException ignored ){}
	}
}
