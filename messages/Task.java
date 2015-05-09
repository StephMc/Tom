package com.rover.spike;

import java.awt.Point;
import java.io.Serializable;

public class Task implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8241963443117495286L;
	public enum AgentTypes{
		POLICE,AMBULANCE
	}
	public Point wayPoint;
	public long activationTime;
	public int taskId;
	public AgentTypes agentType;
}
