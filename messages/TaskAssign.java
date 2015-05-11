package messages;

import java.io.Serializable;

public class TaskAssign implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -849849438027388652L;
	public Task task;
	public String agentId;
	
	public TaskAssign(Task task, String agentId) {
		this.task = task;
		this.agentId = agentId;
	}

}
