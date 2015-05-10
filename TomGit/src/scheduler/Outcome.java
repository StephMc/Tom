package scheduler;

import java.io.Serializable;

public class Outcome implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6694883897542610993L;
	protected float density = (float)1.0;
	protected double quality = 0;
	protected double duration = 0;
	protected double cost = 0;
	
	public Outcome(double quality, double duration, double cost)
	{
		this.quality = quality;
		this.duration = duration;
		this.cost = cost;
	}
	
	public double getQuality()
	{
		return quality;
	}
	
	public double getDuration()
	{
		return duration;
	}
	
	public double getCost()
	{
		return cost;
	}
}
