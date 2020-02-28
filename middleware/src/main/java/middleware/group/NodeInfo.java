package middleware.group;

public class NodeInfo{
	private String hostname;
	private int port;
	private Integer timestamp;
	private GroupCommands command;


	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Integer getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Integer timestamp) {
		this.timestamp = timestamp;
	}

	public GroupCommands getCommand() {
		return command;
	}

	public void setCommand(GroupCommands command) {
		this.command = command;
	}
}
