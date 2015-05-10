package messages;

import java.io.Serializable;

import scheduler.DijkstraDistance;
import scheduler.Node;
import scheduler.Outcome;
import scheduler.Point;
//import android.util.Log;

public class Method extends Node implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 8241963443117495286L;

	public double x;
	public double y;
	public long activationTime;
	
	public static String FinalPoint = "Finish";
	public static String StartingPoint = "Start";
	private int deadline = 0;
	private Outcome outcome;
	private double heuristicQuality = 40000;

	public Method(String methodId, double wayPointX, double wayPointY, long activationTime) {
		super(methodId);
		this.x = wayPointX;
		this.y = wayPointY;
		this.activationTime = activationTime;
		this.outcome = new Outcome(500, 10, 0);
	}
	public Method(String methodId, double wayPointX, double wayPointY, long activationTime,double quality, double duration) {
		super(methodId);
		this.x = wayPointX;
		this.y = wayPointY;
		this.activationTime = activationTime;
		this.outcome = new Outcome(quality, duration, 0);
	}
	public Method(Method method) {
		this(method.label, method.x, method.y, method.activationTime);
	}

	private double distBetweenPoints(double x1, double y1, double x2, double y2) {
		double x_diff = x1 - x2;
		double y_diff = y1 - y2;
		return Math.sqrt(Math.pow(x_diff, 2) + Math.pow(y_diff, 2));
	}
	
	public DijkstraDistance getPathUtilityRepresentedAsDistance(
			DijkstraDistance distanceTillPreviousNode, Point agentPos) {
		//This is distance calculation for this step only. Previous distance used for calculation, but not appended
		DijkstraDistance d = new DijkstraDistance(0,0,this.x, this.y);
		if (this.label==Method.FinalPoint)
		{
			return d;
		}
		//If task can be performed, return utility value through the function. But if its deadline has passed
		//then return an abnormally large negative utility value to force Dijkstra to reject it.
		double totalDurationTillNow = distanceTillPreviousNode.duration + this.outcome.getDuration();
		if ((totalDurationTillNow) > deadline && deadline != 0) {
			d.quality = Long.MIN_VALUE;
			//Log.d("Tom", "[Method 54] Using infinitely negative utility because of " + deadline + " deadline breakage by duration " + totalDurationTillNow);
		} else {
			//Log.d("Tom", "[Method 54] Deadline " + deadline + " will be met by " + totalDurationTillNow);
			double distance = Math.round(distBetweenPoints(agentPos.x, agentPos.y, this.x, this.y));
			d.quality = this.outcome.getQuality() - distance;
			//Log.d("Tom", "task distance = " + distance + " total quality = " + d.quality);
			if (d.quality>heuristicQuality) {
				//Revisit heuristic logic
				d.quality = Long.MIN_VALUE;
			}
			d.duration = this.outcome.getDuration();
			//Log.d("Tom", "[Method 57] Distance from (" + distanceTillPreviousNode.vector.x + ","+distanceTillPreviousNode.vector.y+ ") to " + this.label + " ("+this.x+","+this.y+") ");
		}
		//Log.d("Tom", "[Method 66] Quality determined for " + this.label + " is " + d.quality );
		return d;
	}

	public Outcome getOutcome() {
		return outcome;
	}

	@Override
	public boolean IsTask() {
		return false;
	}

	public boolean isStartMethod() {
		return label.contentEquals(StartingPoint);
	}

	public boolean isEndMethod() {
		return label.contentEquals(FinalPoint);
	}
}
