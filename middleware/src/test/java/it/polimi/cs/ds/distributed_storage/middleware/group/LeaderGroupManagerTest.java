package it.polimi.cs.ds.distributed_storage.middleware.group;

import it.polimi.cs.ds.distributed_storage.exceptions.ParsingException;
import lombok.Data;
import it.polimi.cs.ds.distributed_storage.markers.Primitive;
import it.polimi.cs.ds.distributed_storage.middleware.messages.MessageBroker;
import it.polimi.cs.ds.distributed_storage.middleware.messages.VectorClock;
import org.junit.Rule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.ExpectedException;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static it.polimi.cs.ds.distributed_storage.middleware.group.GroupCommands.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class LeaderGroupManagerTest {
	private static final String ID = "leader";
	private static final String REPLICA_ID = "replica";
	private static final String LEAVING_REPLICA_ID = "leaving";
	private static final int PORT = 12345;
	private static int SERVER_PORT;
	private static final String HOSTNAME = "localhost";
	private static ServerSocket socketAcceptor;
	private static ServerSocket messageAcceptor;
	private static final Map<String, NodeInfo> sockets = new HashMap<>(2);
	private static final List<ReplicaStub> serverSockets = new ArrayList<>(2);
	private static final List<ReplicaStub> messageSockets = new ArrayList<>(2);
	private static Thread acceptorThread;
	private static Thread messageAcceptorThread;
	private static final Map<String,NodeInfo> replicas = new HashMap<>();
	private static final Map<Integer,Integer> data = new HashMap<>();
	private final MessageBroker<?> broker;
	private static final VectorClock vectorClock = new VectorClock(ID);
	private final LeaderGroupManager<Integer,Integer> tested;

	@Rule
	ExpectedException expectedException = ExpectedException.none();

	@BeforeAll
	static void initServer() throws IOException {
		socketAcceptor = new ServerSocket(0);
		SERVER_PORT = socketAcceptor.getLocalPort();
		messageAcceptor  = new ServerSocket(SERVER_PORT+NodeInfo.MESSAGES_PORT_OFFSET);
		assumeTrue(messageAcceptor.getLocalPort()==(socketAcceptor.getLocalPort()+NodeInfo.MESSAGES_PORT_OFFSET));
		assumeFalse(messageAcceptor.getLocalPort()==PORT);
		assumeFalse(socketAcceptor.getLocalPort()==PORT);
		assumeFalse(messageAcceptor.getLocalPort()==socketAcceptor.getLocalPort());
		acceptorThread = new Thread(()->{
			while ( !socketAcceptor.isClosed() ){
				try {
					final Socket accepted = socketAcceptor.accept();
					serverSockets.add(new ReplicaStub(accepted));
				} catch ( IOException e ) {
					e.printStackTrace();
					fail();
				}
			}
		});
		messageAcceptorThread = new Thread(()->{
			while ( !messageAcceptor.isClosed() ){
				try {
					final Socket accepted = messageAcceptor.accept();
					messageSockets.add(new ReplicaStub(accepted));
				} catch ( IOException e ) {
					e.printStackTrace();
					fail();
				}
			}
		});
		messageAcceptorThread.start();
		acceptorThread.start();
		sockets.put(REPLICA_ID,new NodeInfo(new Socket(HOSTNAME, SERVER_PORT)));
		sockets.put(LEAVING_REPLICA_ID,new NodeInfo(new Socket(HOSTNAME, SERVER_PORT)));
	}

	LeaderGroupManagerTest(){
		broker = mock(MessageBroker.class);
		when(broker.getReplicasUnmodifiable()).thenReturn(Collections.unmodifiableMap(replicas));
		when(broker.getLocalClock()).thenReturn(vectorClock);
		tested = new LeaderGroupManager<>(ID,SERVER_PORT,broker,()->data);
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
	@Order(3)
	void parseJoinTest() throws ParsingException, IOException, ClassNotFoundException {

		sockets.get(REPLICA_ID).getGroupOut().writeObject(REPLICA_ID);
		sockets.get(LEAVING_REPLICA_ID).getGroupOut().writeObject(LEAVING_REPLICA_ID);

		final Map<String, NodeInfo> replicasBefore = new HashMap<>(replicas);
		tested.parse(JOIN,serverSockets.get(0).getOut(),serverSockets.get(0).getIn(), serverSockets.get(0).getSocket());
		assertEquals(ID, sockets.get(REPLICA_ID).getGroupIn().readObject());
		assertEquals(replicasBefore, sockets.get(REPLICA_ID).getGroupIn().readObject());
		assertTrue(replicas.containsKey(REPLICA_ID));

		replicasBefore.put(REPLICA_ID,replicas.get(REPLICA_ID));

		tested.parse(JOIN,serverSockets.get(1).getOut(),serverSockets.get(1).getIn(), serverSockets.get(1).getSocket());
		assertEquals(ID, sockets.get(LEAVING_REPLICA_ID).getGroupIn().readObject());
		assertEquals(replicasBefore, sockets.get(LEAVING_REPLICA_ID).getGroupIn().readObject());
		assertTrue(replicas.containsKey(REPLICA_ID));
		assertTrue(replicas.containsKey(LEAVING_REPLICA_ID));

	}

	@Test
	@Order(4)
	void parseSynchTest() throws ParsingException, IOException, ClassNotFoundException {
		tested.parse(SYNC,serverSockets.get(0).getOut(),serverSockets.get(0).getIn(),serverSockets.get(0).getSocket());
		assertEquals(data, sockets.get(REPLICA_ID).getGroupIn().readObject());
		assertEquals(vectorClock, sockets.get(REPLICA_ID).getGroupIn().readObject());
	}

	@Test
	@Order(5)
	void parseLeaveTest() throws ParsingException, IOException {

		sockets.get(LEAVING_REPLICA_ID).getGroupOut().writeObject(LEAVING_REPLICA_ID);

		tested.parse(LEAVE,serverSockets.get(1).getOut(),serverSockets.get(1).getIn(),serverSockets.get(1).getSocket());
		assertFalse(replicas.containsKey(LEAVING_REPLICA_ID));
	}

	@Test
	@Order(6)
	@DisplayName("Exception raised if trying to parse JOINING")
	void parseJoiningTest() {
		try{
			tested.parse(JOINING,serverSockets.get(0).getOut(),serverSockets.get(0).getIn(),serverSockets.get(0).getSocket());
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
			tested.parse(ACK,serverSockets.get(0).getOut(),serverSockets.get(0).getIn(),serverSockets.get(0).getSocket());
		}catch ( ParsingException e ){
			assertTrue(e.getMessage().contains(ACK.toString()));
			return;
		}
		fail();
	}

	@Test
	@Order(7)
	@DisplayName("Leave unsupported")
	void leave_unsupportedException(){
		assertThrows(UnsupportedOperationException.class, tested::leave);
	}

	@AfterEach
	void reset(){
		data.clear();
	}

	@AfterAll
	static void closeConnections() throws IOException {
		if(acceptorThread!=null) acceptorThread.interrupt();
		if(messageAcceptorThread!=null) messageAcceptorThread.interrupt();
		for ( ReplicaStub serverSocket: serverSockets )
			serverSocket.close();
		for ( NodeInfo socket: sockets.values() )
			socket.close();
		for ( ReplicaStub messageSocket : messageSockets ) messageSocket.close();
		socketAcceptor.close();
	}


}

@Data
class ReplicaStub {
	private final Socket socket;
	private final ObjectInputStream in;
	private final ObjectOutputStream out;

	ReplicaStub(String host, int port) throws IOException {
		socket = new Socket(host,port);
		out=new ObjectOutputStream(socket.getOutputStream());
		in=new ObjectInputStream(socket.getInputStream());
	}
	
	ReplicaStub(Socket socket) throws IOException {
		this.socket = socket;
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

enum StubApplicativePrimitive implements Primitive {
	A,B
}