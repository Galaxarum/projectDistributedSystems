package it.polimi.cs.ds.distributed_storage.server.middleware.messages;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class MessageAcceptorImpltest {

	private MessageBrokerImpl<String> producer;
	@Mock private MessageConsumer<String> consumer;

	@BeforeEach
	void init(){
		producer = new MessageBrokerImpl<>("a");
	}

	@Test
	@DisplayName("addConsumer works as a classical set operation")
	void consumersBehavesLikeASet(){
		assumeTrue(producer.consumers.size()==0);
		producer.addConsumer(consumer);
		assertEquals(1,producer.consumers.size());
		assertTrue(producer.consumers.contains(consumer));
		producer.addConsumer(consumer);
		assertEquals(1,producer.consumers.size());
		assertTrue(producer.consumers.contains(consumer));
	}




}
