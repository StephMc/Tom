package messages;

import java.io.Serializable;

public class Task implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8241963443117495286L;
	public enum AgentTypes{
		POLICE,AMBULANCE
	}
	public double wayPoint_x;
	public double wayPoint_y;
	public long activationTime;
	public int taskId;
	public AgentTypes agentType;
}
