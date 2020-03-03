package middleware.group;

import lombok.Getter;
import lombok.Setter;
import middleware.primitives.GroupCommands;

@Getter @Setter
public class NodeInfo{
	private String hostname;
	private int port;
	private Integer timestamp;
	private GroupCommands command;
}
