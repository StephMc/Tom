package messages;

import java.io.Serializable;

public class Utility implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2711252321119981708L;
	
	public String taskId;
	public String agentId;
	public double cost;
	
	public Utility(String taskId, String agentId, double cost) {
		this.taskId = taskId;
		this.agentId = agentId;
		this.cost = cost;
	}
	
}
