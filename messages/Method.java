package messages;

import java.io.Serializable;

public class Method implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8241963443117495286L;

	public double wayPointX;
	public double wayPointY;
	public long activationTime;
	public String methodId;
	
	public Method(String methodId, double wayPointX, double wayPointY, long activationTime) {
		this.methodId = methodId;
		this.wayPointX = wayPointX;
		this.wayPointY = wayPointY;
		this.activationTime = activationTime;	
	}
}
