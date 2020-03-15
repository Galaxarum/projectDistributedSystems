package middleware.group;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class NodeInfo{
	private String hostname;
	private int port;
	private Integer timestamp;
	private GroupCommands command;
}
