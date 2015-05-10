package scheduler;


public class DijkstraDistance {
	public double quality = 0;
	public double duration = 0;
	public Point vector;
	
	public DijkstraDistance(double qual, double dur, double x, double y)
	{
		quality = qual;
		duration = dur;
		vector = new Point(x, y);
	}
	
	public DijkstraDistance Add(DijkstraDistance d2)
	{
		return new DijkstraDistance(this.quality + d2.quality,this.duration + d2.duration, vector.x, vector.y);
	}
	
	public boolean HasGreaterUtility(DijkstraDistance d2)
	{
		//Current implementation is quality based only
		return this.quality>d2.quality;
	}
	
}
