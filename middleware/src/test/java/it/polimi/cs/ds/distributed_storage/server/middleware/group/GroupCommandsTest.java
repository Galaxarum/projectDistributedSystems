package it.polimi.cs.ds.distributed_storage.server.middleware.group;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertNotNull;

public class GroupCommandsTest {
	@Test
	@DisplayName("No nulls")
	void noNulls(){
		for( GroupCommands cmd: GroupCommands.values())
			assertNotNull(cmd);
	}
}
