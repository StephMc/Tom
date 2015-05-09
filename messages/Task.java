package messages;

import java.io.Serializable;
import java.util.HashMap;

public class Task implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8241963443117495286L;
	static public enum AgentTypes{
		POLICE,AMBULANCE
	}
	public String taskId;
	public HashMap<String, Task> tasks;
	public HashMap<String, Method> methods;
	public AgentTypes agentType;
	
	public Task(String taskId, AgentTypes agentType) {
		this.taskId = taskId;
		this.agentType = agentType;
		tasks = new HashMap<String, Task>();
		methods = new HashMap<String, Method>();
	}
	
	public void addTask(Task t) {
		tasks.put(t.taskId, t);
	}
	
	public void addMethod(Method m) {
		methods.put(m.methodId, m);
	}
}
