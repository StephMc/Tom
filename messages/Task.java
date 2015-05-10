package messages;

import java.io.Serializable;
import scheduler.Node;

public class Task extends Node implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8241963443117495286L;
	static public enum AgentTypes{
		POLICE,AMBULANCE
	}
	public AgentTypes agentType;
	
	public Task(String taskId, AgentTypes agentType) {
		super(taskId);
		this.agentType = agentType;	
	}
	
	public void addNode(Node n) {
		children.add(n);
	}

	@Override
	public boolean IsTask() {
		return true;
	}
}
