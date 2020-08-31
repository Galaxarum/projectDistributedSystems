package it.polimi.cs.ds.distributed_storage.server.middleware.messages;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTest {

	@Mock private VectorClock vectorClock;
	private static final String CONTENT = "content";

	@Test
	void boilerplateTest(){
		Message<String> msg = new Message<>();
		assertDoesNotThrow(()->{
			msg.setTimestamp(vectorClock);
			msg.setContent(CONTENT);
		});
		assertEquals(vectorClock,msg.getTimestamp());
		assertEquals(CONTENT,msg.getContent());
		assertNotNull(msg.toString());
	}

}
