package it.polimi.cs.ds.distributed_storage.middleware;

import it.polimi.cs.ds.distributed_storage.middleware.messages.Message;
import it.polimi.cs.ds.distributed_storage.middleware.messages.MessageConsumer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.currentThread;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IntegrationTest {

	private static final String LEADER_ID = "leader";
	private static final int LEADER_PORT = 12345;
	private static final String REPLICA1_ID = "replica1";
	private static final int REPLICA1_PORT = 12347;
	private static final Map<Integer,Integer> leader1Data = new HashMap<>();
	private static final Map<Integer,Integer> replica1Data = new HashMap<>();
	private static final MessageConsumer<MockContent<MockOperation>> leaderConsumer = new MockConsumer(leader1Data);
	private static final MessageConsumer<MockContent<MockOperation>> replica1Consumer = new MockConsumer(replica1Data);

	private MessagingMiddleware<Integer,Integer,MockContent<MockOperation>> leader;
	private MessagingMiddleware<Integer,Integer,MockContent<MockOperation>> replica1;

	@BeforeEach
	void startMiddlewares() throws IOException {
		currentThread().setPriority(Thread.MIN_PRIORITY);
		leader = new MessagingMiddlewareImpl<>(LEADER_ID,LEADER_PORT,()-> leader1Data);
		final Socket replica1ToLeaderSocket = new Socket(InetAddress.getLoopbackAddress().getHostAddress(),LEADER_PORT);
		replica1 = new MessagingMiddlewareImpl<>(REPLICA1_ID,REPLICA1_PORT,replica1ToLeaderSocket, replica1Data::putAll);
		leader.addConsumer(leaderConsumer);
		replica1.addConsumer(replica1Consumer);
	}


	@Test
	void integrationTest() {
		leader.sendMessage(new MockContent<>(MockOperation.PUT,1,2));
		assertTrue(replica1Data.containsKey(2));
		assertEquals(1,replica1Data.get(2).intValue());
	}

	@AfterEach
	void destroySession(){
		leader1Data.clear();
		replica1Data.clear();
	}
}

@Data @AllArgsConstructor
class MockContent<T>{
	T operation;
	Integer data;
	Integer key;
}

enum MockOperation{
	PUT,
	DELETE
}

@RequiredArgsConstructor
class MockConsumer implements MessageConsumer<MockContent<MockOperation>>{
	private final Map<Integer,Integer> map;

	@Override
	public void consumeMessage(Message<MockContent<MockOperation>> msg) {
		switch ( msg.getContent().operation ) {
			case PUT:
				map.put(msg.getContent().key, msg.getContent().data);
				break;
			case DELETE:
				map.remove(msg.getContent().key);
				break;
		}
	}
}