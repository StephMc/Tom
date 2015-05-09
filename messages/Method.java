package messages;

import java.io.Serializable;

public class Method implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8241963443117495286L;
	static public enum AgentTypes{
		POLICE,AMBULANCE
	}
	public double wayPointX;
	public double wayPointY;
	public long activationTime;
	public String methodId;
	public AgentTypes agentType;
	
	public Method(String methodId, AgentTypes agentType, double wayPointX, 
			double wayPointY, long activationTime) {
		this.methodId = methodId;
		this.agentType = agentType;
		this.wayPointX = wayPointX;
		this.wayPointY = wayPointY;
		this.activationTime = activationTime;	
	}
}
