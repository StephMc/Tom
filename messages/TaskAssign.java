package messages;

public class TaskAssign {
	public Task task;
	public String agentId;
	
	public TaskAssign(Task task, String agentId) {
		this.task = task;
		this.agentId = agentId;
	}

}
