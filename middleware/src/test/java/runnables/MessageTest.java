package runnables;

import middleware.messages.Message;
import middleware.messages.VectorClocks;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTest {

	@Mock private VectorClocks vectorClock;
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
